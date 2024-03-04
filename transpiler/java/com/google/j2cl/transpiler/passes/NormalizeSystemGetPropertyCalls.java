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

import static com.google.j2cl.transpiler.ast.AstUtils.getSystemGetPropertyGetter;
import static com.google.j2cl.transpiler.ast.AstUtils.isSystemGetPropertyCall;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import java.util.List;

/** Rewrite System.getProperty() calls based on values passed to the transpiler */
public class NormalizeSystemGetPropertyCalls extends NormalizationPass {

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
            boolean isRequired = arguments.size() != 2;

            MethodCall propertyGetterCall =
                MethodCall.Builder.from(getSystemGetPropertyGetter(propertyKey, isRequired))
                    .setSourcePosition(methodCall.getSourcePosition())
                    .build();

            if (isRequired) {
              return propertyGetterCall;
            }

            Expression defaultValue = arguments.get(1);
            Variable propertyValueVariable =
                Variable.newBuilder()
                    .setName("$propertyValue")
                    .setFinal(true)
                    .setTypeDescriptor(TypeDescriptors.get().javaLangString)
                    .build();
            Variable defaultValueVariable =
                Variable.newBuilder()
                    .setName("$defaultValue")
                    .setFinal(true)
                    .setTypeDescriptor(TypeDescriptors.get().javaLangString)
                    .build();
            return MultiExpression.newBuilder()
                .addExpressions(
                    VariableDeclarationExpression.newBuilder()
                        .addVariableDeclaration(propertyValueVariable, propertyGetterCall)
                        .addVariableDeclaration(defaultValueVariable, defaultValue)
                        .build(),
                    ConditionalExpression.newBuilder()
                        .setTypeDescriptor(TypeDescriptors.get().javaLangString)
                        .setConditionExpression(
                            propertyValueVariable.createReference().infixEqualsNull())
                        .setFalseExpression(propertyValueVariable.createReference())
                        .setTrueExpression(defaultValueVariable.createReference())
                        .build())
                .build();
          }
        });
  }
}
