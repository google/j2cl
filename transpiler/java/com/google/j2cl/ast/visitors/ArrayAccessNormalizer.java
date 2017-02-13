package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;

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
              checkArgument(expression.getOperator() == BinaryOperator.ASSIGN);
              ArrayAccess leftSide = (ArrayAccess) expression.getLeftOperand();
              // Return a call to an Arrays or LongUtils array assignment method.
              return AstUtils.createArraySetExpression(
                  leftSide.getArrayExpression(),
                  leftSide.getIndexExpression(),
                  expression.getRightOperand());
            }
            return expression;
          }
        });
  }
}
