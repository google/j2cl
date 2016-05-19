package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NumberLiteral;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;

/**
 * Rewrites array set operations to use Arrays.$set or LongUtils.$set operations.
 * <p>
 * This prevents side effects from being duplicated.
 */
public class ArrayAccessNormalizer extends AbstractRewriter {

  public static void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ArrayAccessNormalizer());
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression node) {
    if (node.getOperator().hasSideEffect() && node.getLeftOperand() instanceof ArrayAccess) {
      ArrayAccess leftSide = (ArrayAccess) node.getLeftOperand();
      // Return a call to an Arrays or LongUtils array assignment method.
      return AstUtils.createArraySetExpression(
          leftSide.getArrayExpression(),
          leftSide.getIndexExpression(),
          node.getOperator(),
          node.getRightOperand());
    }
    return node;
  }

  @Override
  public Node rewritePrefixExpression(PrefixExpression node) {
    if (node.getOperator().hasSideEffect() && node.getOperand() instanceof ArrayAccess) {
      ArrayAccess arrayAccess = (ArrayAccess) node.getOperand();
      // Return a call to an Arrays or LongUtils array assignment method with the corresponding
      // binary compound assignment operator and a synthesized literal one.
      return AstUtils.createArraySetExpression(
          arrayAccess.getArrayExpression(),
          arrayAccess.getIndexExpression(),
          AstUtils.getCorrespondingCompoundAssignmentOperator(node.getOperator()),
          new NumberLiteral(arrayAccess.getTypeDescriptor(), 1));
    }
    return node;
  }

  @Override
  public Node rewritePostfixExpression(PostfixExpression node) {
    if (node.getOperator().hasSideEffect() && node.getOperand() instanceof ArrayAccess) {
      ArrayAccess arrayAccess = (ArrayAccess) node.getOperand();
      // Return a call to an Arrays or LongUtils postfix array assignment method.
      return AstUtils.createArraySetPostfixExpression(
          arrayAccess.getArrayExpression(), arrayAccess.getIndexExpression(), node.getOperator());
    }
    return node;
  }
}
