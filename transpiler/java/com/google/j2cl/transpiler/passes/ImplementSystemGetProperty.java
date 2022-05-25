/*
 * Copyright 2021 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.StringLiteral;
import java.util.List;
import java.util.Map;

/** Rewrite System.getProperty() calls based on values passed to the transpiler */
public class ImplementSystemGetProperty extends NormalizationPass {
  private final Map<String, String> properties;

  public ImplementSystemGetProperty(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            if (!isSystemGetPropertyCall(methodCall)) {
              return methodCall;
            }

            List<Expression> arguments = methodCall.getArguments();

            // JsInteropRestrictionChecker enforces the first parameter is a StringLiteral.
            String propertyKey = ((StringLiteral) arguments.get(0)).getValue();
            String value = properties.get(propertyKey);
            Expression defaultValue = arguments.size() == 2 ? arguments.get(1) : null;

            checkState(
                value != null || defaultValue != null,
                "No value found for property %s",
                propertyKey);

            MultiExpression.Builder expressionBuilder = MultiExpression.newBuilder();
            if (value == null || (defaultValue != null && defaultValue.hasSideEffects())) {
              // Default value expression can have side effect and needs to be evaluated if present.
              expressionBuilder.addExpressions(defaultValue);
            }

            if (value != null) {
              expressionBuilder.addExpressions(new StringLiteral(value));
            }

            return expressionBuilder.build();
          }
        });
  }

  private static boolean isSystemGetPropertyCall(MethodCall methodCall) {
    return "java.lang.System.getProperty".equals(methodCall.getTarget().getQualifiedBinaryName());
  }
}
