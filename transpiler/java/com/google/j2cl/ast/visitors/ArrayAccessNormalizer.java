package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;

/**
 * Rewrites array set operations to use Arrays.$set or LongUtils.$set operations.
 *
 * <p>This prevents side effects from being duplicated.
 */
public class ArrayAccessNormalizer extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Expression rewriteBinaryExpression(BinaryExpression expression) {
            if (expression.getOperator().hasSideEffect()
                && expression.getLeftOperand() instanceof ArrayAccess) {
              ArrayAccess leftSide = (ArrayAccess) expression.getLeftOperand();
              // Return a call to an Arrays or LongUtils array assignment method.
              return AstUtils.createArraySetExpression(
                  leftSide.getArrayExpression(),
                  leftSide.getIndexExpression(),
                  expression.getOperator(),
                  expression.getRightOperand());
            }
            return expression;
          }

          @Override
          public Expression rewritePrefixExpression(PrefixExpression expression) {
            if (expression.getOperator().hasSideEffect()
                && expression.getOperand() instanceof ArrayAccess) {
              ArrayAccess arrayAccess = (ArrayAccess) expression.getOperand();
              // Return a call to an Arrays or LongUtils array assignment method with the
              // corresponding binary compound assignment operator and a synthesized literal one.
              return AstUtils.createArraySetExpression(
                  arrayAccess.getArrayExpression(),
                  arrayAccess.getIndexExpression(),
                  AstUtils.getCorrespondingCompoundAssignmentOperator(expression.getOperator()),
                  new NumberLiteral(arrayAccess.getTypeDescriptor(), 1));
            }
            return expression;
          }

          @Override
          public Expression rewritePostfixExpression(PostfixExpression expression) {
            if (expression.getOperator().hasSideEffect()
                && expression.getOperand() instanceof ArrayAccess) {
              ArrayAccess arrayAccess = (ArrayAccess) expression.getOperand();
              // Return a call to an Arrays or LongUtils postfix array assignment method.
              return AstUtils.createArraySetPostfixExpression(
                  arrayAccess.getArrayExpression(),
                  arrayAccess.getIndexExpression(),
                  expression.getOperator());
            }
            return expression;
          }
        });
  }
}
