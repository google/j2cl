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
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isBoxedType;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangString;
import static com.google.j2cl.transpiler.ast.TypeDescriptors.isNumericPrimitive;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.common.InternalCompilerError;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.EmbeddedStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PatternMatchExpression;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchCaseDefault;
import com.google.j2cl.transpiler.ast.SwitchCaseExpressions;
import com.google.j2cl.transpiler.ast.SwitchCasePattern;
import com.google.j2cl.transpiler.ast.SwitchExpression;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.YieldStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Makes switch statements to comply with Java semantics. */
public class NormalizeSwitchConstructs extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    removeSwitchExpressions(compilationUnit);
    normalizeSwitchStatements(compilationUnit);
    // Switch with patterns are rewritten using instanceof patterns that need to be desugared.
    new DesugarInstanceOfPatterns().applyTo(compilationUnit);
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
          public Statement rewriteSwitchStatement(SwitchStatement switchStatement) {
            Expression expression = switchStatement.getExpression();
            TypeDescriptor expressionTypeDescriptor = expression.getTypeDescriptor();

            if (switchStatement.hasPatterns()
                || (isBoxedType(expressionTypeDescriptor) && switchStatement.allowsNulls())) {
              // TODO(b/466394693): Consider making this an independent pass that handles both
              // switch statement and switch expressions to allow its reuse in J2KT.
              return convertToIntegerSwitch(switchStatement);
            }

            boolean isBoxableJsEnum =
                AstUtils.isJsEnumBoxingSupported() && expressionTypeDescriptor.isJsEnum();

            if (!switchStatement.allowsNulls()
                && (isJavaLangString(expressionTypeDescriptor)
                    || isBoxableJsEnum
                    || isBoxedType(expressionTypeDescriptor))) {
              // Switch on strings and unboxed JsEnums should throw on null, unless they are
              // explicitly handled.
              return switchStatement.toBuilder()
                  .setExpression(
                      isBoxedType(expressionTypeDescriptor)
                              && isNumericPrimitive(expressionTypeDescriptor.toUnboxedType())
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
   * Rewrite a switch with patterns into a if nest that does the evaluation and selects a case by
   * its index and a switch on that index. This way the control flow inside the switch, like
   * unlabeled breaks, don't need to be resolved.
   */
  private Statement convertToIntegerSwitch(SwitchStatement switchStatement) {
    var initializationStatements = new ArrayList<Statement>();

    var selector = switchStatement.getExpression();
    if (!switchStatement.allowsNulls()) {
      // If the switch does not allow nulls perform the null check.
      selector = RuntimeMethods.createCheckNotNullCall(selector);
    }
    SourcePosition sourcePosition = switchStatement.getSourcePosition();
    if (!selector.isIdempotent()) {
      // This is the temporary variable to avoid repeated evaluation of the original selector
      // expression.
      selector =
          createTemporaryVariable(
                  sourcePosition,
                  "$selector",
                  selector.getTypeDescriptor(),
                  /* initializer= */ selector,
                  initializationStatements)
              .createReference();
    }

    var caseIndexVariable =
        createTemporaryVariable(
            sourcePosition,
            "$caseIndex",
            PrimitiveTypes.INT,
            /* initializer= */ null,
            initializationStatements);

    // Create a conditional expression that returns the case index.
    //
    // if (... first case expression evaluation ...) {
    //   $caseSelector = 0;
    // } else if (... second case expression evaluation ...) {
    //    ...
    // } else if (... last case expression evaluation ...) {
    //   $caseSelector = last;
    // }
    List<SwitchCase> cases = switchStatement.getCases();
    Statement caseIndexSelectionLogic =
        createCaseSelectorLogic(cases, selector, caseIndexVariable, 0);

    // Rewrite the cases for the resulting switch using integer selector.
    //   case 0:
    //     ... original case 0 statements ...
    //
    //            ...
    //
    //   case N:
    //     ... original case N statements ...
    //
    //   default:
    //     ... original default case statements ...
    // }
    for (int i = 0; i < cases.size(); i++) {
      SwitchCase switchCase = cases.get(i);
      cases.set(
          i,
          switchCase.isDefault()
              // No need to rewrite the default case.
              ? switchCase
              // Rewrite the corresponding `case Pattern when guard` into `case index`.
              : SwitchCaseExpressions.newBuilder()
                  .setCaseExpressions(ImmutableList.of(NumberLiteral.fromInt(i)))
                  .setStatements(switchCase.getStatements())
                  .setCanFallthrough(switchCase.canFallthrough())
                  .build());
    }

    return Block.newBuilder()
        // T $selector = <selector expression>;
        .addStatements(initializationStatements)
        .addStatements(caseIndexSelectionLogic)
        // switch (< case index expression >) {
        //   ...rewritten cases ...
        // }
        .addStatements(
            SwitchStatement.Builder.from(switchStatement)
                .setAllowsNulls(false)
                .setExpression(caseIndexVariable.createReference())
                .build())
        .build();
  }

  private static Variable createTemporaryVariable(
      SourcePosition sourcePosition,
      String name,
      TypeDescriptor typeDescriptor,
      Expression initializer,
      List<Statement> initializationStatements) {
    var selectorVariable =
        Variable.newBuilder()
            .setName(name)
            .setTypeDescriptor(typeDescriptor)
            .setFinal(true)
            .build();
    initializationStatements.add(
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(selectorVariable, initializer)
            .build()
            .makeStatement(sourcePosition));
    return selectorVariable;
  }

  /**
   * Construct a conditional that tests the pattern and returns the case index.
   *
   * <pre>{@code
   * if ($selector instanceof Pattern p && guard))
   *     $caseIndex = caseNumber;
   * else  <next case condition>
   *     ...
   * }</pre>
   */
  private Statement createCaseSelectorLogic(
      List<SwitchCase> cases, Expression selector, Variable caseIndexVariable, int currentIndex) {

    Statement caseIndexAssignment =
        Block.newBuilder()
            .addStatement(
                BinaryExpression.Builder.asAssignmentTo(caseIndexVariable)
                    .setRightOperand(NumberLiteral.fromInt(currentIndex))
                    .build()
                    .makeStatement(SourcePosition.NONE))
            .build();

    if (currentIndex == cases.size() || cases.get(currentIndex).isDefault()) {
      return caseIndexAssignment;
    }

    SwitchCase switchCase = cases.get(currentIndex);
    return IfStatement.newBuilder()
        .setConditionExpression(createCaseCondition(selector, switchCase))
        .setThenStatement(caseIndexAssignment)
        .setElseStatement(
            createCaseSelectorLogic(cases, selector, caseIndexVariable, currentIndex + 1))
        .setSourcePosition(cases.get(currentIndex).getSourcePosition())
        .build();
  }

  /** Creates the condition for switch case in a switch with patterns. */
  private static Expression createCaseCondition(Expression selector, SwitchCase switchCase) {
    return switch (switchCase) {
      case SwitchCaseExpressions s -> createExpressionsCondition(selector, s.getCaseExpressions());

      case SwitchCasePattern s -> createPatternCondition(selector, s);

      case SwitchCaseDefault s -> throw new IllegalArgumentException();
    };
  }

  private static Expression createExpressionsCondition(
      Expression selector, List<Expression> expressions) {
    Expression expression = expressions.getFirst();
    Expression condition =
        (expression instanceof NullLiteral || expression.getTypeDescriptor().isEnum())
            // nulls and enums can be compared by reference.
            ? selector.clone().infixEquals(expression)
            // strings and primitives are compared using `equals.`
            : RuntimeMethods.createObjectsEqualsMethodCall(selector.clone(), expression);

    return expressions.size() > 1
        ? condition.infixOr(
            createExpressionsCondition(selector, expressions.subList(1, expressions.size())))
        : condition;
  }

  private static Expression createPatternCondition(Expression selector, SwitchCasePattern s) {
    Expression condition =
        // $selector instanceof Pattern p
        PatternMatchExpression.newBuilder()
            .setExpression(selector.clone())
            .setPattern(s.getPattern())
            .build();
    Expression guard = s.getGuard();
    if (guard == null) {
      return condition;
    }
    // condition && guard.
    return condition.infixAnd(guard);
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
