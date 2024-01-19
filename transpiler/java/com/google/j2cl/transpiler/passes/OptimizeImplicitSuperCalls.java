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

import static com.google.common.collect.Iterables.getLast;
import static com.google.j2cl.transpiler.ast.AstUtils.getConstructorInvocationStatement;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;

/** Remove super constructor calls which can be implicit. */
public class OptimizeImplicitSuperCalls extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethod(Method method) {
            if (!method.isConstructor()) {
              return method;
            }

            ExpressionStatement constructorCallStatement =
                getConstructorInvocationStatement(method);
            if (constructorCallStatement == null) {
              return method;
            }

            MethodCall constructorCall = (MethodCall) constructorCallStatement.getExpression();
            MethodDescriptor methodDescriptor = method.getDescriptor();
            MethodDescriptor constructorCallTarget = constructorCall.getTarget();

            boolean isSuperCall = !constructorCallTarget.inSameTypeAs(methodDescriptor);
            if (!isSuperCall) {
              return method;
            }

            if (!constructorCall.getArguments().isEmpty()) {
              // Keep calls with non-empty args to non-vararg methods.
              if (!constructorCallTarget.isVarargs()) {
                return method;
              }

              // Keep calls with non-ArrayLiteral vararg argument.
              Expression varargArgument = getLast(constructorCall.getArguments());
              if (!(varargArgument instanceof ArrayLiteral)) {
                return method;
              }

              // Keep calls with non-empty ArrayLiteral vararg argument.
              // Assuming that this pass is executed after NormalizeVarargInvocationsJ2kt, calls
              // with empty ArrayLiteral vararg argument are safe to be removed.
              ArrayLiteral arrayLiteral = (ArrayLiteral) varargArgument;
              if (!arrayLiteral.getValueExpressions().isEmpty()) {
                return method;
              }
            }

            method.getBody().getStatements().remove(constructorCallStatement);

            return method;
          }
        });
  }
}
