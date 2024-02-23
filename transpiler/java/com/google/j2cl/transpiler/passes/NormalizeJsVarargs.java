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
package com.google.j2cl.transpiler.passes;

import com.google.common.collect.Iterables;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Variable;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Implements JavaScript varargs calling convention by rewriting varargs calls and adding a preamble
 * to varargs JsMethods.
 *
 * <p>At the method body the array is stamped to have the correct Java runtime information.
 *
 * <p>At the call sites, array creation/literal the array is spread either statically (if it was an
 * array literal) or at runtime using the ES6 spread (...) operator.
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
      Variable varargsParameter = functionExpression.getJsVarargsParameter();
      maybeAddVarargsPreamble(
          (ArrayTypeDescriptor) varargsParameter.getTypeDescriptor(),
          varargsParameter,
          functionExpression.getBody());
      return functionExpression;
    }

    @Override
    public Node rewriteMethod(Method method) {
      if (!method.getDescriptor().isJsMethodVarargs() || method.isAbstract() || method.isNative()) {
        return method;
      }

      Variable varargsParameter = method.getJsVarargsParameter();
      // If this method (which is a JsMethod) is a bridge then stamp to the expected type of the
      // bridged method. Otherwise there might be a ClassCastException due to the array being of
      // incompatible type.
      ArrayTypeDescriptor varargsStampTypeDescriptor =
          (ArrayTypeDescriptor)
              (method.getDescriptor().isBridge()
                  ? Iterables.getLast(
                      method.getDescriptor().getBridgeTarget().getParameterTypeDescriptors())
                  : varargsParameter.getTypeDescriptor());
      maybeAddVarargsPreamble(varargsStampTypeDescriptor, varargsParameter, method.getBody());
      return method;
    }

    private static void maybeAddVarargsPreamble(
        ArrayTypeDescriptor varargsStampTypeDescriptor, Variable varargsParameter, Block body) {
      if (varargsStampTypeDescriptor.isUntypedArray()) {
        return;
      }

      // TODO(b/36180242): avoid stamping if not needed.
      // stamp the rest (varargs) parameter.
      //   Arrays.stampType(varargsParameter, new arrayType[]...[]);
      MethodCall arrayStampTypeMethodCall =
          RuntimeMethods.createArraysStampTypeMethodCall(
              varargsParameter.createReference(), varargsStampTypeDescriptor);

      body.getStatements().add(0, arrayStampTypeMethodCall.makeStatement(body.getSourcePosition()));
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

      List<Expression> extractedVarargsArguments = extractVarargsArguments(lastArgument);
      if (extractedVarargsArguments != null) {
        return MethodCall.Builder.from(invocation)
            .replaceVarargsArgument(extractedVarargsArguments)
            .build();
      }

      // Pass the array expression with a spread. Note that in Java such array expression can be
      // null and if the method does not dereference it there will be no error. But applying the
      // spread operator to null results in a JavaScript error.
      return MethodCall.Builder.from(invocation)
          .replaceVarargsArgument(lastArgument.prefixSpread())
          .build();
    }
  }

  @Nullable
  private static List<Expression> extractVarargsArguments(Expression expression) {
    // If the last argument is an array literal, or an array creation with array literal, or a
    // zero length array literal, extract it.
    if (expression instanceof NewArray) {
      expression = extractExplicitInitializer((NewArray) expression);
    }

    if (expression instanceof ArrayLiteral) {
      return ((ArrayLiteral) expression).getValueExpressions();
    }

    return null;
  }

  /** Extracts an explicit initializer from a NewArray expression. */
  @Nullable
  private static Expression extractExplicitInitializer(NewArray newArray) {
    Expression initializer = newArray.getInitializer();
    if (initializer != null) {
      return initializer;
    }

    if (newArray.getDimensionExpressions().get(0) instanceof NumberLiteral) {
      NumberLiteral numberLiteral = (NumberLiteral) newArray.getDimensionExpressions().get(0);
      if (numberLiteral.getValue().intValue() == 0) {
        // This is newArray of zero length, even if it didn't have an initializer we can provide
        // and empty array literal of the right type.
        return new ArrayLiteral(newArray.getTypeDescriptor());
      }
    }

    return null;
  }
}

