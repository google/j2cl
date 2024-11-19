/*
 * Copyright 2018 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchConstruct;
import com.google.j2cl.transpiler.ast.SwitchExpression;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Makes switch statements to comply with Java semantics. */
public class NormalizeSwitchStatements extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public SwitchStatement rewriteSwitchStatement(SwitchStatement switchStatement) {
            return normalizeSwitchConstruct(switchStatement);
          }

          @Override
          public Node rewriteSwitchExpression(SwitchExpression switchExpression) {
            return normalizeSwitchConstruct(switchExpression);
          }

          private <T extends SwitchConstruct<T>> T normalizeSwitchConstruct(T switchConstruct) {
            Expression expression = switchConstruct.getExpression();
            TypeDescriptor expressionTypeDescriptor = expression.getTypeDescriptor();

            if (TypeDescriptors.isJavaLangString(expressionTypeDescriptor)
                || (AstUtils.isJsEnumBoxingSupported() && expressionTypeDescriptor.isJsEnum())) {
              // Switch on strings and unboxed JsEnums should throw on null.
              return switchConstruct.toBuilder()
                  .setExpression(
                      RuntimeMethods.createCheckNotNullCall(switchConstruct.getExpression()))
                  .build();
            }

            if (expressionTypeDescriptor.isEnum()) {
              return convertEnumSwitchConstruct(switchConstruct);
            }

            checkArgument(TypeDescriptors.isBoxedOrPrimitiveType(expressionTypeDescriptor));
            // Switch on primitives do not require conversions.
            return switchConstruct;
          }
        });
  }

  /**
   * Rewrites switch statements on enums to comply with Java semantics.
   *
   * <p>Switch statements on enum objects will be done through their ordinals, accomplishing two
   * objectives:
   * <li>1. avoid referring to enum objects on case clauses,
   * <li>2. throw if the expression is null to comply with Java semantics.
   */
  private static <T extends SwitchConstruct<T>> T convertEnumSwitchConstruct(T switchConstruct) {
    return switchConstruct.toBuilder()
        .setExpression(
            MethodCall.Builder.from(
                    TypeDescriptors.get().javaLangEnum.getMethodDescriptor("ordinal"))
                .setQualifier(switchConstruct.getExpression())
                .build())
        .setCases(
            switchConstruct.getCases().stream()
                .map(NormalizeSwitchStatements::convertToOrdinalCase)
                .collect(toImmutableList()))
        .build();
  }

  private static SwitchCase convertToOrdinalCase(SwitchCase switchCase) {
    if (switchCase.isDefault()) {
      return switchCase;
    }
    for (int i = 0; i < switchCase.getCaseExpressions().size(); i++) {
      FieldAccess enumFieldAccess = (FieldAccess) switchCase.getCaseExpressions().get(i);
      switchCase
          .getCaseExpressions()
          .set(
              i,
              FieldAccess.Builder.from(enumFieldAccess)
                  .setTarget(
                      AstUtils.getEnumOrdinalConstantFieldDescriptor(enumFieldAccess.getTarget()))
                  .build());
    }
    return switchCase;
  }
}
