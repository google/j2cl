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

import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.StringLiteral;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;

/**
 * Rewrite String concatenation using a {@see StringBuilder}
 *
 * <p>
 *
 * <pre>
 *   {@code "foo" + a + "bar"}
 *   is rewritten as
 *   {@code
 *   $string_builder = new StringBuilder();
 *   $string_builder.append("foo");
 *   $string_builder.append(a);
 *   $string_builder.append("bar);
 *   $string_builder.toString();
 *   }
 * </pre>
 */
public class ImplementStringConcatenation extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            if (!binaryExpression.isStringConcatenation()) {
              return binaryExpression;
            }
            if (isInStringConcatenationChain()) {
              // When we have a chain of String concatenation: "Foo" + a + "Bar", we rewrite the
              // entire chain when we exit the top-most BinaryOperation of the chain in order to use
              // use StringBuilder instance.
              return binaryExpression;
            }

            ImmutableList<Expression> operands = collectConcatOperands(binaryExpression);

            // Create String.valueOf call for the simple case.
            if (operands.size() == 1) {
              return RuntimeMethods.createStringValueOfMethodCall(operands.get(0));
            }

            MultiExpression.Builder multiExpressionBuilder = MultiExpression.newBuilder();

            // $stringBuilder = new StringBuilder()
            Variable stringBuilder =
                Variable.newBuilder()
                    .setFinal(true)
                    .setName("$stringBuilder")
                    .setTypeDescriptor(TypeDescriptors.get().javaLangStringBuilder)
                    .build();

            multiExpressionBuilder.addExpressions(
                VariableDeclarationExpression.newBuilder()
                    .addVariableDeclaration(
                        stringBuilder,
                        NewInstance.Builder.from(
                                TypeDescriptors.get()
                                    .javaLangStringBuilder
                                    .getDefaultConstructorMethodDescriptor())
                            .build())
                    .build());

            // Add  $stringBuilder.append() calls
            for (Expression operand : operands) {
              multiExpressionBuilder.addExpressions(
                  MethodCall.Builder.from(
                          TypeDescriptors.get()
                              .javaLangStringBuilder
                              .getMethodDescriptor(
                                  "append",
                                  getAppendParameterTypeDescriptor(operand.getTypeDescriptor())))
                      .setQualifier(stringBuilder.createReference())
                      .setArguments(operand)
                      .build());
            }

            // $stringBuilder.toString()
            multiExpressionBuilder.addExpressions(
                MethodCall.Builder.from(
                        TypeDescriptors.get().javaLangStringBuilder.getMethodDescriptor("toString"))
                    .setQualifier(stringBuilder.createReference())
                    .build());

            return multiExpressionBuilder.build();
          }

          private boolean isInStringConcatenationChain() {
            return getParent() instanceof BinaryExpression
                && ((BinaryExpression) getParent()).getOperator() == BinaryOperator.PLUS;
          }
        });
  }

  private static TypeDescriptor getAppendParameterTypeDescriptor(TypeDescriptor typeDescriptor) {
    // There is an append() overload for String and every primitives.
    if (typeDescriptor.isPrimitive() || TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return typeDescriptor;
    }

    return TypeDescriptors.get().javaLangObject;
  }

  private static ImmutableList<Expression> collectConcatOperands(Expression expression) {
    if (expression instanceof BinaryExpression
        && ((BinaryExpression) expression).isStringConcatenation()) {
      BinaryExpression binaryExpression = (BinaryExpression) expression;
      return ImmutableList.<Expression>builder()
          .addAll(collectConcatOperands(binaryExpression.getLeftOperand()))
          .addAll(collectConcatOperands(binaryExpression.getRightOperand()))
          .build();
    }

    if (expression instanceof StringLiteral && ((StringLiteral) expression).getValue().isEmpty()) {
      // Skip empty string on concat; esp. happens with common patterns like ("" + x).
      return ImmutableList.of();
    }

    return ImmutableList.of(expression);
  }
}
