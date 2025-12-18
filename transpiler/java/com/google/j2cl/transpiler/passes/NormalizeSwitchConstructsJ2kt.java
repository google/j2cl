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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BindingPattern;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.BreakOrContinueStatement;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DoWhileStatement;
import com.google.j2cl.transpiler.ast.EmbeddedStatement;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.Label;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.NullLiteral;
import com.google.j2cl.transpiler.ast.NumberLiteral;
import com.google.j2cl.transpiler.ast.PatternMatchExpression;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.RecordPattern;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchCase;
import com.google.j2cl.transpiler.ast.SwitchCaseDefault;
import com.google.j2cl.transpiler.ast.SwitchCasePattern;
import com.google.j2cl.transpiler.ast.SwitchConstruct;
import com.google.j2cl.transpiler.ast.SwitchExpression;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.YieldStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes switch statement for Kotlin, into cascading labeled blocks.
 *
 * <p>Before:
 *
 * <pre>{@code
 * SWITCH: switch (x) {
 *   case 1:
 *     runCase1();
 *   case 2:
 *     runCase2();
 *     break SWITCH;
 *   default:
 *     runDefault();
 * }
 * }</pre>
 *
 * <p>After:
 *
 * <pre>{@code
 * SWITCH: {
 *   CASE_3: {
 *     CASE_2: {
 *       CASE_1: {
 *         switch (x) {
 *           case 1: break CASE_1;
 *           case 2: break CASE_2;
 *           default: break CASE_3:
 *         }
 *         break SWITCH;
 *       }
 *       runCase1();
 *     }
 *     runCase2();
 *     break SWITCH;
 *   }
 *   runDefault();
 * }
 * }</pre>
 */
public class NormalizeSwitchConstructsJ2kt extends NormalizationPass {
  /** Case/label pair. */
  @AutoValue
  abstract static class SwitchCaseWithLabel {
    abstract SwitchCase getSwitchCase();

    abstract Label getLabel();
  }

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    normalizeSwitchPatterns(compilationUnit);
    convertFallThroughSwitchExpressionsToSwitchStatements(compilationUnit);
    normalizeSwitchStatements(compilationUnit);
    convertToSwitchExpression(compilationUnit);
    reorderSwitchCases(compilationUnit);
    removeTrailingBreaks(compilationUnit);
  }

  /**
   * Normalizes switch patterns to the subset that is directly supported by the Kotlin when
   * expression.
   */
  private void normalizeSwitchPatterns(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {

          @Override
          public Statement rewriteSwitchStatement(SwitchStatement switchStatement) {
            if (!switchStatement.hasPatterns()) {
              return switchStatement;
            }

            List<Expression> variableDeclarations = new ArrayList<>();
            Statement rewrittenSwitch =
                normalizeSwitchPatterns(switchStatement, variableDeclarations);
            return Block.newBuilder()
                .addStatements(
                    variableDeclarations.stream()
                        .map(e -> e.makeStatement(switchStatement.getSourcePosition()))
                        .collect(toImmutableList()))
                .addStatement(rewrittenSwitch)
                .build();
          }

          @Override
          public Expression rewriteSwitchExpression(SwitchExpression switchExpression) {
            if (!switchExpression.hasPatterns()) {
              return switchExpression;
            }

            List<Expression> variableDeclarations = new ArrayList<>();
            Expression rewrittenSwitch =
                normalizeSwitchPatterns(switchExpression, variableDeclarations);
            return MultiExpression.newBuilder()
                .addExpressions(variableDeclarations)
                .addExpressions(rewrittenSwitch)
                .build();
          }
        });

    // Desugar the newly introduced record pattern matching.
    new DesugarInstanceOfPatterns().applyTo(compilationUnit);
  }

  private <T extends SwitchConstruct<T>> T normalizeSwitchPatterns(
      T switchConstruct, List<Expression> variableDeclarations) {
    // Extract the evaluation of the selector because the implementation of the binding patterns
    // will need to refer to its value.
    Expression selectorExpression = switchConstruct.getExpression();
    Variable selector =
        Variable.newBuilder()
            .setName("$selector")
            .setTypeDescriptor(selectorExpression.getTypeDescriptor())
            .setFinal(true)
            .build();
    variableDeclarations.add(
        VariableDeclarationExpression.newBuilder()
            .addVariableDeclaration(selector, selectorExpression)
            .build());

    var cases = switchConstruct.getCases();
    for (int i = 0; i < cases.size(); i++) {
      if (!(cases.get(i) instanceof SwitchCasePattern switchCase)) {
        continue;
      }
      cases.set(
          i,
          switch (switchCase.getPattern()) {
            case BindingPattern pattern -> {
              Variable variable = pattern.getVariable();
              // Collect the declaration of the variables that appear in the binding patterns so
              // that they are declared in a scope accessible by all the references.
              variableDeclarations.add(
                  VariableDeclarationExpression.newBuilder()
                      .addVariableDeclarations(variable)
                      .build());
              yield switchCase.toBuilder()
                  .setPattern(
                      // Replace the variable by a dummy unnamed variable that will be ignored
                      // in the backend.
                      new BindingPattern(Variable.Builder.from(variable).setName("_").build()))
                  .setGuard(
                      // Add a trivially true guard that has the assignment of the pattern
                      // variable.
                      MultiExpression.newBuilder()
                          .setExpressions(
                              BinaryExpression.Builder.asAssignmentTo(variable)
                                  .setRightOperand(selector.createReference())
                                  .build(),
                              switchCase.getGuard() == null
                                  ? BooleanLiteral.get(true)
                                  : switchCase.getGuard())
                          .build())
                  .build();
            }

            case RecordPattern pattern -> {
              // Rewrite the record pattern into a binding top level pattern and a guard.
              //
              //  case R1(R2(....))
              //
              // becomes
              //
              // case R1 && selector instanceof R1(R2(....))
              //

              yield switchCase.toBuilder()
                  .setPattern(
                      // Replace the variable by a dummy unnamed variable that will be ignored
                      // in the backend.
                      new BindingPattern(
                          Variable.newBuilder()
                              .setName("_")
                              .setTypeDescriptor(pattern.getTypeDescriptor())
                              .build()))
                  .setGuard(
                      // Add a trivially true guard that has the assignment of the pattern
                      // variable.
                      MultiExpression.newBuilder()
                          .setExpressions(
                              PatternMatchExpression.newBuilder()
                                  .setExpression(selector.createReference())
                                  .setPattern(pattern)
                                  .build(),
                              switchCase.getGuard() == null
                                  ? BooleanLiteral.get(true)
                                  : switchCase.getGuard())
                          .build())
                  .build();
            }
          });
    }

    return switchConstruct.toBuilder().setExpression(selector.createReference()).build();
  }

  /** Converts switch expressions that have non fallthrough cases into switch statements. */
  private void convertFallThroughSwitchExpressionsToSwitchStatements(
      CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          final Map<SwitchExpression, Label> assignedLabelBySwitchExpression = new HashMap<>();

          @Override
          public Expression rewriteSwitchExpression(SwitchExpression switchExpression) {
            if (!switchExpression.canFallthrough()) {
              // Can be left as a switch expression, since those are rendered directly as `when`
              // expressions.
              return switchExpression;
            }

            // Since it has fallthrough and can not be converted directly to a `when` expression,
            // lower it into a `switch` statement with a label (which is necessary since at this
            // point all potential targets of breaks are assumed labeled). At the end it will be
            // enclosed into an embedded statement to handle the return value.
            var switchStatement =
                SwitchStatement.Builder.from(switchExpression)
                    .build()
                    .encloseWithLabel(getLabel(switchExpression));

            // Switch expressions are always exhaustive, but in some situations the kotlin compiler
            // can not determine that statically and might fail to compile the generated code.
            // To avoid the issue add `throw new AssertionError();` after the `switch statement`
            // to help kotlinc to determine that all the exits are within the switch statement.
            var throwStatement =
                ThrowStatement.newBuilder()
                    .setExpression(
                        NewInstance.newBuilder()
                            .setTarget(
                                TypeDescriptors.get()
                                    .javaLangAssertionError
                                    .getDefaultConstructorMethodDescriptor())
                            .build())
                    .setSourcePosition(switchExpression.getSourcePosition())
                    .build();

            return EmbeddedStatement.newBuilder()
                .setStatement(
                    Block.newBuilder().setStatements(switchStatement, throwStatement).build())
                .setTypeDescriptor(switchExpression.getTypeDescriptor())
                .build();
          }

          @Override
          public Node rewriteYieldStatement(YieldStatement yieldStatement) {
            SwitchExpression enclosingSwitchExpression =
                (SwitchExpression) getParent(SwitchExpression.class::isInstance);
            if (!enclosingSwitchExpression.canFallthrough()) {
              return yieldStatement;
            }

            // TODO(b/391582571): the actual target of the yield is the enclosing embedded statement
            // but the label is on the switch.

            // Add the target label for correctness.
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

  /** Normalizes switch statements to simplify the conversion to switch expressions . */
  private static void normalizeSwitchStatements(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitSwitchStatement(SwitchStatement switchStatement) {
            normalizeSwitchCaseTypes(switchStatement);
            ensureExhaustive(switchStatement);
          }
        });
  }

  /** Convert switch case expressions to be of the same type as switch expression. */
  private static void normalizeSwitchCaseTypes(SwitchStatement switchStatement) {
    TypeDescriptor targetTypeDescriptor = switchStatement.getExpression().getTypeDescriptor();
    switchStatement
        .getCases()
        .forEach(switchCase -> convertSwitchCaseExpressionType(switchCase, targetTypeDescriptor));
  }

  private static void convertSwitchCaseExpressionType(
      SwitchCase switchCase, TypeDescriptor targetTypeDescriptor) {
    List<Expression> caseExpressions = switchCase.getCaseExpressions();
    for (int i = 0; i < caseExpressions.size(); i++) {
      Expression caseExpression = caseExpressions.get(i);

      if (caseExpression instanceof NumberLiteral literal) {
        caseExpressions.set(
            i, new NumberLiteral(targetTypeDescriptor.toUnboxedType(), literal.getValue()));
      } else if (!caseExpression.getTypeDescriptor().isSameBaseType(targetTypeDescriptor)) {
        caseExpressions.set(
            i,
            caseExpression instanceof NullLiteral
                ? caseExpression
                : CastExpression.newBuilder()
                    .setExpression(caseExpression)
                    .setCastTypeDescriptor(targetTypeDescriptor.toNonNullable())
                    .build());
      }
    }
  }

  /** Adds the default case if missing. */
  private static void ensureExhaustive(SwitchStatement switchStatement) {
    if (switchStatement.getCases().stream().anyMatch(SwitchCase::isDefault)) {
      return;
    }

    switchStatement.getCases().add(SwitchCaseDefault.newBuilder().build());
  }

  /**
   * Converts switch statements to switch expressions adding necessary control flow to handle
   * fallthrough cases.
   */
  private static void convertToSwitchExpression(CompilationUnit compilationUnit) {
    // Convert to cascading labeled blocks.
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteSwitchStatement(SwitchStatement switchStatement) {
            if (canConvertDirectlyToWhen(switchStatement)) {
              return SwitchExpression.Builder.from(switchStatement)
                  .build()
                  .makeStatement(switchStatement.getSourcePosition());
            }

            List<SwitchCaseWithLabel> switchCasesAndLabels =
                createLabelsForSwitchCases(switchStatement.getCases());

            Label switchLabel = ((LabeledStatement) getParent()).getLabel();
            Statement dispatchStatement =
                createDispatchStatement(
                    switchLabel,
                    switchStatement.getExpression(),
                    switchCasesAndLabels,
                    switchStatement.getSourcePosition());

            return createNestedCaseHandlingBlocks(
                dispatchStatement, switchCasesAndLabels, switchStatement.getSourcePosition());
          }
        });
  }

  /** Creates labels for each case, and returns case/label pairs. */
  private static List<SwitchCaseWithLabel> createLabelsForSwitchCases(
      List<SwitchCase> switchCases) {
    return switchCases.stream()
        .map(
            switchCase ->
                new AutoValue_NormalizeSwitchConstructsJ2kt_SwitchCaseWithLabel(
                    switchCase, Label.newBuilder().setName("CASE").build()))
        .collect(toImmutableList());
  }

  /**
   * Creates the logic to dispatch to target switch case.
   *
   * <pre>{@code
   * {
   *   switch (foo) {
   *     CASE_1: break LABEL_1;
   *     CASE_2: break LABEL_2;
   *     CASE_3: break LABEL_3;
   *   }
   *   break SWITCH_LABEL;
   * }
   * }</pre>
   */
  private static Statement createDispatchStatement(
      Label switchLabel,
      Expression expression,
      List<SwitchCaseWithLabel> switchCasesWithLabels,
      SourcePosition sourcePosition) {
    ImmutableList<SwitchCase> cases =
        switchCasesWithLabels.stream()
            .map(
                switchCaseWithLabel ->
                    createCaseWithBreak(
                        switchCaseWithLabel.getSwitchCase(),
                        switchCaseWithLabel.getLabel(),
                        sourcePosition))
            .collect(toImmutableList());

    Expression whenExpression =
        SwitchExpression.newBuilder()
            .setTypeDescriptor(PrimitiveTypes.VOID)
            .setExpression(expression)
            .setCases(cases)
            .setSourcePosition(sourcePosition)
            .build();

    return Block.newBuilder()
        .addStatement(whenExpression.makeStatement(sourcePosition))
        .addStatement(
            BreakStatement.newBuilder()
                .setSourcePosition(sourcePosition)
                .setLabelReference(switchLabel.createReference())
                .build())
        .build();
  }

  /**
   * Returns {@code switchCase} where statements are replaced with a break statement with {@code
   * label}.
   */
  private static SwitchCase createCaseWithBreak(
      SwitchCase switchCase, Label label, SourcePosition sourcePosition) {
    return switchCase.toBuilder()
        .setStatements(
            BreakStatement.newBuilder()
                .setLabelReference(label.createReference())
                .setSourcePosition(sourcePosition)
                .build())
        .build();
  }

  /**
   * Wraps {@code dispatchStatement} with nested labeled statements for each case/label pair.
   *
   * <pre>{@code
   * {
   *   LABEL_3: {
   *     LABEL_2: {
   *       LABEL_1: {
   *         // Dispatch statement
   *       }
   *       // Case 1 statements
   *     }
   *     // Case 2 statements
   *   }
   *   // Case 3 statements
   * }
   * }</pre>
   */
  private static Statement createNestedCaseHandlingBlocks(
      Statement dispatchStatement,
      List<SwitchCaseWithLabel> switchCaseWithLabels,
      SourcePosition sourcePosition) {
    for (SwitchCaseWithLabel switchCaseWithLabel : switchCaseWithLabels) {
      dispatchStatement =
          Block.newBuilder()
              .addStatement(
                  LabeledStatement.newBuilder()
                      .setLabel(switchCaseWithLabel.getLabel())
                      .setStatement(dispatchStatement)
                      .setSourcePosition(sourcePosition)
                      .build())
              .addStatements(switchCaseWithLabel.getSwitchCase().getStatements())
              .build();
    }

    return dispatchStatement;
  }

  /**
   * For a switch to be directly expressible as a when in Kotlin two conditions have to be met: the
   * default case needs to be the last one, and there should not be any fallthrough except for empty
   * cases.
   */
  private static boolean canConvertDirectlyToWhen(SwitchStatement switchStatement) {
    List<SwitchCase> cases = switchStatement.getCases();
    List<SwitchCase> casesExceptLast = cases.isEmpty() ? cases : cases.subList(0, cases.size() - 1);
    return casesExceptLast.stream()
        .allMatch(switchCase -> !switchCase.isDefault() && !switchCase.canFallthrough());
  }

  /** Moves the default case to the end. */
  private void reorderSwitchCases(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteSwitchExpression(SwitchExpression switchExpression) {
            var cases =
                Streams.concat(
                        switchExpression.getCases().stream()
                            .filter(Predicates.not(SwitchCase::isDefault)),
                        switchExpression.getCases().stream().filter(SwitchCase::isDefault))
                    .collect(toImmutableList());
            return switchExpression.toBuilder().setCases(cases).build();
          }
        });
  }

  /** Removes breaks at the end that are equivalent to normal flow out. */
  private static void removeTrailingBreaks(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteLabeledStatement(LabeledStatement labeledStatement) {
            removeTrailingBreaks(labeledStatement.getStatement(), labeledStatement.getLabel());
            return labeledStatement;
          }
        });
  }

  /**
   * Removes trailing breaks or continues that target ether a block, an `if` statement or a `do { }
   * while(false)` statement.
   */
  private static void removeTrailingBreaks(Statement body, Label label) {
    if (body instanceof Block block) {
      removeTrailingBreaks(block.getStatements(), label);
    }
    if (body instanceof ExpressionStatement expressionStatement) {
      if (expressionStatement.getExpression() instanceof SwitchExpression switchExpression) {
        for (SwitchCase switchCase : switchExpression.getCases()) {
          removeTrailingBreaks(switchCase.getStatements(), label);
        }
      }
    }

    if (body instanceof DoWhileStatement doWhileStatement) {
      if (doWhileStatement.getConditionExpression().isBooleanFalse()) {
        removeTrailingBreaks(doWhileStatement.getBody(), label);
      }
    }

    if (body instanceof IfStatement ifStatement) {
      removeTrailingBreaks(ifStatement.getThenStatement(), label);
      if (ifStatement.getElseStatement() != null) {
        removeTrailingBreaks(ifStatement.getElseStatement(), label);
      }
    }
  }

  private static void removeTrailingBreaks(List<Statement> statements, Label label) {
    int i = statements.size() - 1;
    while (i >= 0) {
      Statement lastStatement = statements.get(i);
      // We can safely remove continue statements since we are only handling cases where continue
      // statements target `do { continue; } while(false)`; and in these cases continue statements
      // is equivalent to break statements.
      if (lastStatement instanceof BreakOrContinueStatement breakOrContinueStatement
          && breakOrContinueStatement.getLabelReference().getTarget() == label) {
        statements.remove(i--);
      } else {
        removeTrailingBreaks(lastStatement, label);
        break;
      }
    }
  }
}
