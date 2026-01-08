/*
 * Copyright 2024 Google Inc.
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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.BinaryOperator;
import com.google.j2cl.transpiler.ast.BindingPattern;
import com.google.j2cl.transpiler.ast.BooleanLiteral;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.Pattern;
import com.google.j2cl.transpiler.ast.PatternMatchExpression;
import com.google.j2cl.transpiler.ast.RecordPattern;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.Variable;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.List;

/** Normalizes instanceof patterns out. */
public class DesugarInstanceOfPatterns extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewritePatternMatchExpression(
              PatternMatchExpression patternMatchExpression) {
            if (getPatternFromLeftOperandOfConditionalAnd(getParent()) == patternMatchExpression) {
              // Don't desugar the pattern here, desugar the pattern from the binary
              // expression to avoid the unnecessary `true` due to the last term
              // being an assignment.
              return patternMatchExpression;
            }
            return desugarPattern(patternMatchExpression, BooleanLiteral.get(true));
          }

          @Override
          public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
            PatternMatchExpression patternMatchExpression =
                getPatternFromLeftOperandOfConditionalAnd(binaryExpression);
            if (patternMatchExpression == null) {
              return binaryExpression;
            }

            // The pattern is on the lhs of a conditional &&; use the rhs as the term
            // that is evaluated in the multiexpression with the last assignment.
            return desugarPattern(patternMatchExpression, binaryExpression.getRightOperand());
          }
        });
  }

  private PatternMatchExpression getPatternFromLeftOperandOfConditionalAnd(Object node) {
    if (node instanceof BinaryExpression binaryExpression
        && binaryExpression.getOperator() == BinaryOperator.CONDITIONAL_AND
        && binaryExpression.getLeftOperand() instanceof PatternMatchExpression pattern) {
      return pattern;
    }
    return null;
  }

  private static Expression desugarPattern(
      PatternMatchExpression patternMatchExpression, Expression nextTerm) {
    return desugarPattern(
        patternMatchExpression.getSourcePosition(),
        patternMatchExpression.getExpression(),
        patternMatchExpression.getPattern(),
        nextTerm);
  }

  private static Expression desugarPattern(
      SourcePosition sourcePosition, Expression expression, Pattern pattern, Expression nextTerm) {
    return switch (pattern) {
      case BindingPattern bindingPattern ->
          desugarBindingPattern(sourcePosition, expression, bindingPattern, nextTerm);
      case RecordPattern recordPattern ->
          desugarRecordPattern(sourcePosition, expression, recordPattern, nextTerm);
    };
  }

  private static Expression desugarBindingPattern(
      SourcePosition sourcePosition,
      Expression expression,
      BindingPattern pattern,
      Expression nextTerm) {
    Variable patternVariable = pattern.getVariable();

    // The instanceof has a pattern and will be represented as a multi expression as
    // follows:
    //    (ExpType exp = expression,               // avoid double evaluation of expression
    //       exp instanceof T &&                   // perform the instance of operation and
    //       (patternVariable = (T) exp, true),    // cast exp and assign to pattern variable
    //    )

    var resultBuilder = MultiExpression.newBuilder();
    // Always use a variable for the expression that needs to be evaluated twice to
    // prevent increasing code size and make the code more readable.
    Variable expressionVariable;
    if (expression instanceof VariableReference variableReference) {
      // If it is already a variable just use it.
      expressionVariable = variableReference.getTarget();
    } else {
      // Create a new variable to avoid evaluating expression twice.
      expressionVariable =
          Variable.newBuilder()
              .setName("exp")
              .setFinal(true)
              .setTypeDescriptor(expression.getTypeDescriptor())
              .build();
      resultBuilder.addExpressions(
          VariableDeclarationExpression.newBuilder()
              .addVariableDeclaration(expressionVariable, expression)
              .build());
    }

    //  exp instanceof T && (T patternVariable = (T) exp, nextTerm)
    return resultBuilder
        .addExpressions(
            InstanceOfExpression.newBuilder()
                .setExpression(expressionVariable.createReference())
                .setTestTypeDescriptor(patternVariable.getTypeDescriptor())
                .setSourcePosition(sourcePosition)
                .build()
                .infixAnd(
                    assignPatternVariableReturningNextTerm(
                        patternVariable,
                        CastExpression.newBuilder()
                            .setExpression(expressionVariable.createReference())
                            .setCastTypeDescriptor(
                                patternVariable.getTypeDescriptor().toNonNullable())
                            .build(),
                        nextTerm)))
        .build();
  }

  /**
   * Declares and assigns the pattern variable in a multiexpression that evaluates to {@code
   * nextTerm}.
   */
  private static Expression assignPatternVariableReturningNextTerm(
      Variable patternVariable, Expression expression, Expression nextTerm) {
    // (Type var = (Type) expression, true)
    return MultiExpression.newBuilder()
        .addExpressions(
            VariableDeclarationExpression.newBuilder()
                .addVariableDeclaration(patternVariable, expression)
                .build(),
            nextTerm)
        .build();
  }

  private static Expression desugarRecordPattern(
      SourcePosition sourcePosition,
      Expression expression,
      RecordPattern recordPattern,
      Expression nextTerm) {
    // Transform the record pattern `e instanceof R(p1, ...)` into the desugared from of
    // `e instanceof R r &&  <nested pattern expressions>`.

    Variable patternVariable =
        Variable.newBuilder()
            .setName("pattern" + recordPattern.getTypeDescriptor().getSimpleSourceName())
            .setTypeDescriptor(recordPattern.getTypeDescriptor())
            .setFinal(true)
            .build();

    // Say we are translating something like exp instanceof Record(T t,...)

    // exp instanceof Record r && ...
    return desugarPattern(
        sourcePosition,
        expression,
        new BindingPattern(patternVariable),
        deconstructComponents(
            sourcePosition,
            patternVariable,
            recordPattern.getNestedPatterns(),
            recordPattern.getComponentAccessorsDescriptors(),
            nextTerm));
  }

  private static Expression deconstructComponents(
      SourcePosition sourcePosition,
      Variable patternVariable,
      List<Pattern> componentPattern,
      List<MethodDescriptor> accessors,
      Expression nextTerm) {
    if (componentPattern.isEmpty()) {
      return nextTerm;
    }
    MethodDescriptor accessor = accessors.getFirst();
    Pattern nestedPattern = componentPattern.getFirst();
    MethodCall accessorCall =
        MethodCall.Builder.from(accessor).setQualifier(patternVariable.createReference()).build();

    Expression rest =
        deconstructComponents(
            sourcePosition,
            patternVariable,
            componentPattern.subList(1, componentPattern.size()),
            accessors.subList(1, accessors.size()),
            nextTerm);

    // Match the record property using its accessor to the corresponding pattern form.
    return isUnconditionalPattern(accessorCall, nestedPattern)
        // An unconditional pattern does not perform an instanceof, just assigns directly
        // the pattern variable since they are compatible types. Unconditional patterns
        // allow nulls.
        //
        // (t = r.px(), nexTerm)
        ? assignPatternVariableReturningNextTerm(
            ((BindingPattern) nestedPattern).getVariable(), accessorCall, rest)
        // Or a regular pattern which we desugar immediately.
        // r.px() instanceof T t
        : desugarPattern(sourcePosition, accessorCall, nestedPattern, rest);
  }

  /** Whether the pattern is always matched and allows nulls (JLS 14.30.3). */
  private static boolean isUnconditionalPattern(Expression expression, Pattern nestedPattern) {
    // Per JLS 14.30.3 record patterns cannot be unconditional
    TypeDescriptor patternTypeDescriptor = nestedPattern.getTypeDescriptor();
    return nestedPattern instanceof BindingPattern
        // And the variable they declare has to be either a primitive or a supertype of the
        // expression type (only considering their erasures)
        && (patternTypeDescriptor.isPrimitive()
            || expression.getTypeDescriptor().isAssignableTo(patternTypeDescriptor));
  }
}
