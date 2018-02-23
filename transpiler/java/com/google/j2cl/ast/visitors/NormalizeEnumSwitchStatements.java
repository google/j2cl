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
package com.google.j2cl.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.SwitchCase;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.TypeDescriptors;

/** Makes enum switch statements go through ordinals. */
public class NormalizeEnumSwitchStatements extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public SwitchStatement rewriteSwitchStatement(SwitchStatement switchStatement) {
            Expression expression = switchStatement.getSwitchExpression();

            if (!expression.getTypeDescriptor().isEnum()) {
              return switchStatement;
            }

            return SwitchStatement.Builder.from(switchStatement)
                .setSwitchExpression(
                    MethodCall.Builder.from(
                            TypeDescriptors.get().javaLangEnum.getMethodDescriptorByName("ordinal"))
                        .setQualifier(expression)
                        .build())
                .setCases(
                    switchStatement
                        .getCases()
                        .stream()
                        .map(NormalizeEnumSwitchStatements::convertToOrdinalCase)
                        .collect(ImmutableList.toImmutableList()))
                .build();
          }
        });
  }

  private static SwitchCase convertToOrdinalCase(SwitchCase switchCase) {
    if (switchCase.isDefault()) {
      return switchCase;
    }

    FieldAccess enumField = (FieldAccess) switchCase.getCaseExpression();
    return SwitchCase.Builder.from(switchCase)
        .setCaseExpression(
            FieldAccess.Builder.from(enumField)
                .setTargetFieldDescriptor(
                    AstUtils.getEnumOrdinalConstantFieldDescriptor(enumField.getTarget()))
                .build())
        .build();
  }
}
