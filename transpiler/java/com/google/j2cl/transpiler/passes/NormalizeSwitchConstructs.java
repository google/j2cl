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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.EmbeddedStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchExpression;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.YieldStatement;
import java.util.HashMap;
import java.util.Map;

/** Makes switch statements to comply with Java semantics. */
public class NormalizeSwitchConstructs extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    removeSwitchExpressions(compilationUnit);
    normalizeSwitchStatements(compilationUnit);
  }

  /** Transform switch expressions into switch statements that are embedded in expressions. */
  private static void removeSwitchExpressions(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          final Map<SwitchExpression, Label> assignedLabelBySwitchExpression = new HashMap<>();

          @Override
          public Expression rewriteSwitchExpression(SwitchExpression switchExpression) {
            return EmbeddedStatement.newBuilder()
                .setStatement(
                    SwitchStatement.Builder.from(switchExpression)
                        .build()
                        .encloseWithLabel(getLabel(switchExpression)))
                .setTypeDescriptor(switchExpression.getTypeDescriptor())
                .build();
          }

          @Override
          public Node rewriteYieldStatement(YieldStatement yieldStatement) {
            SwitchExpression enclosingSwitchExpression =
                (SwitchExpression) getParent(SwitchExpression.class::isInstance);
            return YieldStatement.Builder.from(yieldStatement)
                .setLabelReference(getLabel(enclosingSwitchExpression).createReference())
                .build();
          }

          private Label getLabel(SwitchExpression switchExpression) {
            return assignedLabelBySwitchExpression.computeIfAbsent(
                checkNotNull(switchExpression), s -> Label.newBuilder().setName("SWITCH").build());
          }
        });
  }

  /**
   * Normalize switch statements to:
   * <ul>
   * <li> comply with jsinterop JsEnum semantics,
   * <li> make explicit the null check of the switch expression.
   * <li> and make explicit the use of ordinals as switch case constants.
   * </ul>
   */
  private void normalizeSwitchStatements(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public SwitchStatement rewriteSwitchStatement(SwitchStatement switchStatement) {
            Expression expression = switchStatement.getExpression();
            TypeDescriptor expressionTypeDescriptor = expression.getTypeDescriptor();

            boolean isBoxableJsEnum =
                AstUtils.isJsEnumBoxingSupported() && expressionTypeDescriptor.isJsEnum();

            if (!switchStatement.allowsNulls()
                && (TypeDescriptors.isJavaLangString(expressionTypeDescriptor)
                    || isBoxableJsEnum
                    || TypeDescriptors.isBoxedType(expressionTypeDescriptor))) {
              // Switch on strings and unboxed JsEnums should throw on null, unless they are
              // explicitly handled.
              return switchStatement.toBuilder()
                  .setExpression(
                      TypeDescriptors.isBoxedType(expressionTypeDescriptor)
                              && TypeDescriptors.isNumericPrimitive(
                                  expressionTypeDescriptor.toUnboxedType())
                          // Trigger unboxing which will also implicitly accomplish the null check.
                          ? CastExpression.newBuilder()
                              .setCastTypeDescriptor(PrimitiveTypes.INT)
                              .setExpression(expression)
                              .build()
                          : RuntimeMethods.createCheckNotNullCall(switchStatement.getExpression()))
                  .build();
            }

            // Boxable JsEnums are left untouched since they are handled directly in JavaScript
            // (which is the only backend that currently supports them). Regular enum switches are
            // converted to integer switches on their ordinals.
            if (expressionTypeDescriptor.isEnum() && !isBoxableJsEnum) {
              return convertEnumSwitchStatement(switchStatement);
            }

            return switchStatement;
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
  private static SwitchStatement convertEnumSwitchStatement(SwitchStatement switchStatement) {
    boolean hasCaseNull = switchStatement.allowsNulls();

    var switchExpression = MultiExpression.newBuilder();

    Expression expression = switchStatement.getExpression();
    if (hasCaseNull && !expression.isIdempotent()) {
      // Avoid evaluating the switch expression twice.
      //
      // ($switchExpr = expression, ...)
      Variable tempVariable =
          Variable.newBuilder()
              .setTypeDescriptor(expression.getTypeDescriptor())
              .setFinal(true)
              .setName("$switchExpression")
              .build();
      switchExpression.addExpressions(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(tempVariable, expression)
              .build());
      expression = tempVariable.createReference();
    }

    switchExpression.addExpressions(
        hasCaseNull
            // ($switchExpr == null ? -1 : $switchExpr.ordinal())
            ? ConditionalExpression.newBuilder()
                .setConditionExpression(expression.clone().infixEqualsNull())
                .setTypeDescriptor(PrimitiveTypes.INT)
                .setTrueExpression(NumberLiteral.fromInt(NULL_ENUM_ORDINAL))
                .setFalseExpression(getOrdinalMethodCall(expression))
                .build()
            : getOrdinalMethodCall(expression));

    return switchStatement.toBuilder()
        .setExpression(switchExpression.build())
        .setCases(
            switchStatement.getCases().stream()
                .map(NormalizeSwitchConstructs::convertToOrdinalCase)
                .collect(toImmutableList()))
        .build();
  }

  // An integer value that can never be the ordinal of an enum, is used to represent null.
  private static final int NULL_ENUM_ORDINAL = -1;

  private static MethodCall getOrdinalMethodCall(Expression expression) {
    return MethodCall.Builder.from(
            TypeDescriptors.get().javaLangEnum.getMethodDescriptor("ordinal"))
        .setQualifier(expression)
        .build();
  }

  private static SwitchCase convertToOrdinalCase(SwitchCase switchCase) {
    for (int i = 0; i < switchCase.getCaseExpressions().size(); i++) {
      Expression caseExpression = switchCase.getCaseExpressions().get(i);
      if (caseExpression instanceof FieldAccess enumFieldAccess) {
        caseExpression =
            FieldAccess.Builder.from(enumFieldAccess)
                .setTarget(
                    AstUtils.getEnumOrdinalConstantFieldDescriptor(enumFieldAccess.getTarget()))
                .build();
      } else if (caseExpression instanceof NullLiteral) {
        caseExpression = NumberLiteral.fromInt(NULL_ENUM_ORDINAL);
      } else {
        throw new InternalCompilerError("Unexpected case expression: %s", caseExpression);
      }
      switchCase.getCaseExpressions().set(i, caseExpression);
    }
    return switchCase;
  }
}
