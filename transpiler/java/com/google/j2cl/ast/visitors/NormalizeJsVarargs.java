/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.ast.visitors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PostfixOperator;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableReference;
import java.util.Arrays;

/**
 * Implements JavaScript varargs calling convention by rewriting varargs calls and adding a prolog
 * to varargs JsMethods.
 *
 * <p>At the method body, we copy the var arguments to a local array, and replaces reference to the
 * var parameter with reference to the local copy.
 *
 * <p>At the call sites, array creation/literal is unwrapped as array literals, which are passed as
 * individual parameters. And array object is wrapped with other arguments and passed as a single
 * array of arguments, and invoked in the form of fn.apply().
 *
 * <p>TODO: Optimize the copying away if only read access to vararg[i] and vararg.length happen in
 * the body.
 */
public class NormalizeJsVarargs extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new NormalizeJsMethodsVarargs());
    compilationUnit.accept(new NormalizeVarargsJsMethodCallsVisitor());
  }

  /**
   * Normalizes varargs JsMethods, which includes
   *
   * <p>
   * 1. Adding preamble to the method body to copy the var arguments to a local variable.
   *
   * <p>
   * 2. Fixing references to the varargs parameter with references to the local variable.
   */
  private static class NormalizeJsMethodsVarargs extends AbstractRewriter {
    private static final Variable ARGUMENTS_PARAMETER =
        Variable.Builder.fromDefault()
            .setName("arguments")
            .setTypeDescriptor(TypeDescriptors.getForArray(TypeDescriptors.get().javaLangObject, 1))
            .setIsParameter(true)
            .setIsRaw(true)
            .build();
    private static final TypeDescriptor primitiveInt = TypeDescriptors.get().primitiveInt;
    private static final TypeDescriptor primitiveBoolean = TypeDescriptors.get().primitiveBoolean;

    private Variable varargsParameter = null;
    /**
     * Local variable that holds the elements of varargs array.
     */
    private Variable varargsLocalCopy = null;

    @Override
    public boolean shouldProcessMethod(Method method) {
      varargsParameter = null;
      varargsLocalCopy = null;
      if (!method.getDescriptor().isJsMethodVarargs() || getCurrentType().isInterface()) {
        return false;
      }
      varargsParameter = Iterables.getLast(method.getParameters());
      varargsLocalCopy =
          Variable.Builder.fromDefault()
              .setName("$var_args_copy")
              .setTypeDescriptor(varargsParameter.getTypeDescriptor())
              .build();
      return true;
    }

    @Override
    public Node rewriteVariableReference(VariableReference variableReference) {
      if (variableReference.getTarget() == varargsParameter) {
        return varargsLocalCopy.getReference();
      }
      return variableReference;
    }

    @Override
    public Node rewriteMethod(Method method) {
      Preconditions.checkArgument(method.getDescriptor().isJsMethodVarargs());

      // copy the varargs array.
      //   <Type>[] $var_args_copy = new <Type>[arguments.length - offset];
      //   for (int $i = 0; $i < arguments.length - offset; i++) {
      //     $var_args_copy[i] = arguments[i + offset];
      //   }

      // (1) $var_args_copy = new VarArgsType[arguments.length - varargsIndex];
      Preconditions.checkArgument(!method.getParameters().isEmpty());
      int varargsIndex = method.getParameters().size() - 1;
      Expression newArray =
          new NewArray(
              varargsParameter.getTypeDescriptor(),
              Arrays.<Expression>asList(createArraySizeExpression(varargsIndex)),
              null);
      Statement variableDeclaration =
          new ExpressionStatement(
              new VariableDeclarationExpression(
                  new VariableDeclarationFragment(varargsLocalCopy, newArray)));

      // (2) (loop body) $var_args_copy[i] = arguments[i + varargsIndex];
      Variable loopVariable =
          Variable.Builder.fromDefault().setName("$i").setTypeDescriptor(primitiveInt).build();

      Expression indexExpression =
          varargsIndex == 0
              ? loopVariable.getReference()
              : new BinaryExpression(
                  primitiveInt,
                  loopVariable.getReference(),
                  BinaryOperator.PLUS,
                  new NumberLiteral(primitiveInt, varargsIndex));
      Statement body =
          new ExpressionStatement(
              AstUtils.createArraySetExpression(
                  varargsLocalCopy.getReference(),
                  loopVariable.getReference(),
                  BinaryOperator.ASSIGN,
                  new ArrayAccess(ARGUMENTS_PARAMETER.getReference(), indexExpression)));

      // (3). (for statement) for ($i = 0; i < arguments.length - idx; i++) { ... }
      Statement forStatement =
          new ForStatement(
              new BinaryExpression(
                  primitiveBoolean,
                  loopVariable.getReference(),
                  BinaryOperator.LESS,
                  createArraySizeExpression(varargsIndex)),
              new Block(body),
              Arrays.<Expression>asList(
                  new VariableDeclarationExpression(
                      new VariableDeclarationFragment(
                          loopVariable, new NumberLiteral(primitiveInt, 0)))),
              Arrays.<Expression>asList(
                  new PostfixExpression(
                      primitiveInt, loopVariable.getReference(), PostfixOperator.INCREMENT)));

      return Method.Builder.from(method)
          .addStatement(0, variableDeclaration)
          .addStatement(1, forStatement)
          .build();
    }

    private Expression createArraySizeExpression(int varargsIndex) {
      FieldAccess argumentsLengthReference =
          FieldAccess.Builder.from(AstUtils.ARRAY_LENGTH_FIELD_DESCRIPTION)
              .setQualifier(ARGUMENTS_PARAMETER.getReference())
              .build();

      return varargsIndex == 0
          ? argumentsLengthReference
          : new BinaryExpression(
              primitiveInt,
              argumentsLengthReference,
              BinaryOperator.MINUS,
              new NumberLiteral(primitiveInt, varargsIndex));
    }
  }

  /**
   * Normalizes method calls to varargs JsMethods.
   *
   * <p>
   * If the var argument is in the form of array literal, unwrap it at call site. Otherwise, call
   * the function in the form of function.apply(thisArg, [args]).
   */
  private static class NormalizeVarargsJsMethodCallsVisitor extends AbstractRewriter {
    @Override
    public Node rewriteInvocation(Invocation invocation) {
      MethodDescriptor target = invocation.getTarget();
      if (!target.isJsMethodVarargs()) {
        return invocation;
      }
      Expression lastArgument = Iterables.getLast(invocation.getArguments());

      // If the last argument is an array literal, or an array creation with array literal,
      // unwrap array literal, and pass the unwrapped arguments directly.
      if (lastArgument instanceof ArrayLiteral) {
        return MethodCall.Builder.from(invocation)
            .replaceVarargsArgument(((ArrayLiteral) lastArgument).getValueExpressions())
            .build();
      }

      if (lastArgument instanceof NewArray) {
        ArrayLiteral arrayLiteral = ((NewArray) lastArgument).getArrayLiteral();
        if (arrayLiteral != null) {
          return MethodCall.Builder.from(invocation)
              .replaceVarargsArgument(arrayLiteral.getValueExpressions())
              .build();
        }
      }

      // Here we wrap the vararg type with $Util.$checkNotNullVararg before applying the spread
      // operator because the spread of a null causes a runtime exception in Javascript.
      // The reason for this is that there is a mismatch between Java varargs and Javascript varargs
      // semantics.  In Java, if you pass a null for the varargs it passes a null array rather than
      // an array with a single null object.  In Javascript however we pass the values of the
      // varargs as arguments not as an array so there is no way to express this.
      // $checkNotNullVararg errors out early if null is passed as a jsvararg parameter.
      // TODO: For non-nullable types we can avoid this.
      TypeDescriptor returnType = TypeDescriptors.toNonNullable(lastArgument.getTypeDescriptor());
      MethodDescriptor nullToEmptyDescriptor =
          MethodDescriptor.Builder.fromDefault()
              .setReturnTypeDescriptor(returnType)
              .setIsStatic(true)
              .setMethodName("$checkNotNull")
              .setJsInfo(JsInfo.RAW)
              .setEnclosingClassTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
              .setReturnTypeDescriptor(returnType)
              .addParameter(lastArgument.getTypeDescriptor())
              .build();

      MethodCall nullToEmpty =
          MethodCall.Builder.from(nullToEmptyDescriptor).setArguments(lastArgument).build();
      return MethodCall.Builder.from(invocation)
          .replaceVarargsArgument(
              new PrefixExpression(returnType, nullToEmpty, PrefixOperator.SPREAD))
          .build();
    }
  }
}
