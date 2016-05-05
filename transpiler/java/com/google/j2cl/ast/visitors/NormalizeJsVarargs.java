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
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.ForStatement;
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
 * <p>
 * At the method body, we copy the var arguments to a local array, and replaces reference to the var
 * parameter with reference to the local copy.
 *
 * <p>
 * At the call sites, array creation/literal is unwrapped as array literals, which are passed as
 * individual parameters. And array object is wrapped with other arguments and passed as a single
 * array of arguments, and invoked in the form of fn.apply().
 *
 * TODO: Optimize the copying away if only read access to vararg[i] and vararg.length happen in the
 * body.
 */
public class NormalizeJsVarargs extends AbstractRewriter {
  private static final Variable ARGUMENTS_PARAMETER =
      new Variable(
          "arguments",
          TypeDescriptors.getForArray(TypeDescriptors.get().javaLangObject, 1),
          false,
          true,
          true);
  private static TypeDescriptor primitiveInt = TypeDescriptors.get().primitiveInt;
  private static TypeDescriptor primitiveBoolean = TypeDescriptors.get().primitiveBoolean;

  public static void applyTo(CompilationUnit compilationUnit) {
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
    private Variable varargsParameter = null;
    /**
     * Local variable that holds the elements of varargs array.
     */
    private Variable varargsLocalCopy = null;

    @Override
    public boolean shouldProcessMethod(Method method) {
      varargsParameter = null;
      varargsLocalCopy = null;
      if (!method.getDescriptor().isJsMethodVarargs() || getCurrentJavaType().isInterface()) {
        return false;
      }
      varargsParameter = Iterables.getLast(method.getParameters());
      varargsLocalCopy =
          new Variable("$var_args_copy", varargsParameter.getTypeDescriptor(), false, false);
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
      FieldAccess argumentsLengthReference =
          new FieldAccess(
              ARGUMENTS_PARAMETER.getReference(), AstUtils.ARRAY_LENGTH_FIELD_DESCRIPTION);
      Expression arraySize =
          varargsIndex == 0
              ? argumentsLengthReference
              : new BinaryExpression(
                  primitiveInt,
                  argumentsLengthReference,
                  BinaryOperator.MINUS,
                  new NumberLiteral(primitiveInt, varargsIndex));
      Expression newArray =
          new NewArray(
              varargsParameter.getTypeDescriptor(), Arrays.<Expression>asList(arraySize), null);
      Statement variableDeclaration =
          new ExpressionStatement(
              new VariableDeclarationExpression(
                  new VariableDeclarationFragment(varargsLocalCopy, newArray)));

      // (2) (loop body) $var_args_copy[i] = arguments[i + varargsIndex];
      Variable loopVariable = new Variable("$i", primitiveInt, false, false);
      Expression leftOperand =
          new ArrayAccess(varargsLocalCopy.getReference(), loopVariable.getReference());
      Expression indexExpression =
          varargsIndex == 0
              ? loopVariable.getReference()
              : new BinaryExpression(
                  primitiveInt,
                  loopVariable.getReference(),
                  BinaryOperator.PLUS,
                  new NumberLiteral(primitiveInt, varargsIndex));
      Expression rightOperand =
          new ArrayAccess(ARGUMENTS_PARAMETER.getReference(), indexExpression);
      Statement body =
          new ExpressionStatement(
              new BinaryExpression(
                  leftOperand.getTypeDescriptor(),
                  leftOperand,
                  BinaryOperator.ASSIGN,
                  rightOperand));

      // (3). (for statement) for ($i = 0; i < arguments.length - idx; i++) { ... }
      Statement forStatement =
          new ForStatement(
              new BinaryExpression(
                  primitiveBoolean, loopVariable.getReference(), BinaryOperator.LESS, arraySize),
              body,
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
    public Node rewriteMethodCall(MethodCall methodCall) {
      MethodDescriptor target = methodCall.getTarget();
      if (!target.isJsMethodVarargs()) {
        return methodCall;
      }
      Expression lastArgument = Iterables.getLast(methodCall.getArguments());
      MethodCall.Builder methodCallBuilder = MethodCall.Builder.from(methodCall);

      // If the last argument is an array literal, or an array creation with array literal,
      // unwrap array literal, and pass the unwrapped arguments directly.
      ArrayLiteral arrayLiteral = null;
      if (lastArgument instanceof ArrayLiteral) {
        arrayLiteral = (ArrayLiteral) lastArgument;
      }
      if (lastArgument instanceof NewArray) {
        arrayLiteral = ((NewArray) lastArgument).getArrayLiteral();
      }
      if (arrayLiteral != null) {
        // Directly unwrap as array literal.
        methodCallBuilder.removeLastArgument();
        for (Expression varArgument : arrayLiteral.getValueExpressions()) {
          // add argument to the call site, but do not change the MethodDescriptor.
          methodCallBuilder.addArgument(varArgument, null);
        }
        return methodCallBuilder.build();
      }

      methodCallBuilder.removeLastArgument();
      methodCallBuilder.addArgument(
          new PrefixExpression(
              lastArgument.getTypeDescriptor(), lastArgument, PrefixOperator.SPREAD),
          null);
      return methodCallBuilder.build();
    }
  }
}
