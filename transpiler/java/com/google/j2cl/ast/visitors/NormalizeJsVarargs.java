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

import com.google.common.collect.Iterables;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrefixOperator;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.TypeDescriptors;
import com.google.j2cl.ast.Variable;
import java.util.List;

/**
 * Implements JavaScript varargs calling convention by rewriting varargs calls and adding a preamble
 * to varargs JsMethods.
 *
 * <p>At the method body the array is stamped to have the correct Java runtime information.
 *
 * <p>At the call sites, array creation/literal the array is spread either statically (if it was an
 * array literal) or at runtime using the ES6 spread (...) operator.
 *
 * <p>TODO(tdeegan): Optimize the copying away if only read access to vararg[i] and vararg.length
 * happen in the body.
 */
public class NormalizeJsVarargs extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new AddJsVarargsPreambles());
    compilationUnit.accept(new NormalizeJsVarargsCalls());
  }

  /**
   * Adds the method preamble to stamp the varargs array with the correct type information if
   * needed.
   */
  private static class AddJsVarargsPreambles extends AbstractRewriter {
    @Override
    public Node rewriteFunctionExpression(FunctionExpression functionExpression) {
      if (!functionExpression.isJsVarargs()) {
        return functionExpression;
      }
      maybeAddVarargsPreamble(
          functionExpression.getJsVarargsParameter(), functionExpression.getBody());
      return functionExpression;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.getDescriptor().isJsMethodVarargs() || getCurrentType().isInterface()) {
        return method;
      }

      maybeAddVarargsPreamble(method.getJsVarargsParameter(), method.getBody());
      return method;
    }

    private static void maybeAddVarargsPreamble(Variable varargsParameter, Block body) {
      if (varargsParameter.getTypeDescriptor().isUntypedArray()) {
        return;
      }

      // TODO(b/36180242): avoid stamping if not needed.
      // stamp the rest (varargs) parameter.
      //   Arrays.stampType(varargsParameter, new arrayType[]...[]);
      MethodDescriptor arrayStampTypeMethodDescriptor =
          MethodDescriptor.newBuilder()
              .setEnclosingTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
              .setJsInfo(JsInfo.RAW)
              .setStatic(true)
              .setName("$stampType")
              .setParameterTypeDescriptors(
                  TypeDescriptors.getForArray(TypeDescriptors.get().javaLangObject, 1),
                  TypeDescriptors.get().javaLangObject,
                  TypeDescriptors.get().primitiveDouble)
              .build();
      // Use the raw type as the stamped leaf type. So that we use the upper bound of a generic type
      // parameter type instead of the type parameter itself.
      MethodCall arrayStampTypeMethodCall =
          MethodCall.Builder.from(arrayStampTypeMethodDescriptor)
              .setArguments(
                  varargsParameter.getReference(),
                  new JavaScriptConstructorReference(
                      varargsParameter.getTypeDescriptor().getLeafTypeDescriptor()),
                  new NumberLiteral(
                      TypeDescriptors.get().primitiveDouble,
                      varargsParameter.getTypeDescriptor().getDimensions()))
              .build();

      List<Statement> statements = body.getStatements();
      statements.add(0, arrayStampTypeMethodCall.makeStatement(body.getSourcePosition()));
    }
  }

  /**
   * Normalizes method calls to varargs JsMethods.
   *
   * <p>If the var argument is in the form of array literal, unwrap it at call site. Otherwise, call
   * the function using the ES6 spread (...) operator.
   */
  private static class NormalizeJsVarargsCalls extends AbstractRewriter {
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
      // TODO(tdeegan): For non-nullable types we can avoid this.
      TypeDescriptor returnType = TypeDescriptors.toNonNullable(lastArgument.getTypeDescriptor());
      MethodDescriptor nullToEmptyDescriptor =
          MethodDescriptor.newBuilder()
              .setReturnTypeDescriptor(returnType)
              .setStatic(true)
              .setName("$checkNotNull")
              .setJsInfo(JsInfo.RAW)
              .setEnclosingTypeDescriptor(TypeDescriptors.BootstrapType.ARRAYS.getDescriptor())
              .setReturnTypeDescriptor(returnType)
              .addParameterTypeDescriptors(lastArgument.getTypeDescriptor())
              .build();

      MethodCall nullToEmpty =
          MethodCall.Builder.from(nullToEmptyDescriptor).setArguments(lastArgument).build();
      return MethodCall.Builder.from(invocation)
          .replaceVarargsArgument(
              PrefixExpression.newBuilder()
                  .setTypeDescriptor(returnType)
                  .setOperand(nullToEmpty)
                  .setOperator(PrefixOperator.SPREAD)
                  .build())
          .build();
    }
  }
}
