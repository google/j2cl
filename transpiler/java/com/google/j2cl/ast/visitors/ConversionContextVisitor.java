/*
 * Copyright 2015 Google Inc.
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
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.BinaryOperator;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.ConditionalExpression;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.OperatorSideEffectUtils;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.UnaryExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Driver for rewriting conversions in different contexts.
 *
 * <p>Traverses the AST, recognizing and categorizing different conversion contexts and dispatching
 * conversion requests in that context.
 */
public final class ConversionContextVisitor extends AbstractRewriter {

  /**
   * Base class for defining how to insert a conversion operation in a given conversion context.
   */
  protected abstract static class ContextRewriter {

    /**
     * Expression is going to the given type.
     */
    @SuppressWarnings("unused")
    public Expression rewriteAssignmentContext(
        TypeDescriptor toTypeDescriptor, Expression expression) {
      return expression;
    }

    /**
     * Subject expression is interacting with other expression.
     */
    @SuppressWarnings("unused")
    public Expression rewriteBinaryNumericPromotionContext(
        Expression subjectOperandExpression, Expression otherOperandExpression) {
      return subjectOperandExpression;
    }

    /**
     * Contained expression is going to the contained type.
     */
    public Expression rewriteCastContext(CastExpression castExpression) {
      return castExpression;
    }

    /**
     * Expression is going to the given type.
     */
    @SuppressWarnings("unused")
    public Expression rewriteMethodInvocationContext(
        TypeDescriptor parameterTypeDescriptor, Expression argumentExpression) {
      return argumentExpression;
    }

    /**
     * Expression is always going to String.
     */
    @SuppressWarnings("unused")
    public Expression rewriteStringContext(
        Expression operandExpression, Expression otherOperandExpression) {
      return operandExpression;
    }

    /**
     * Expression is always going to primitive.
     */
    public Expression rewriteUnaryNumericPromotionContext(Expression operandExpression) {
      return operandExpression;
    }
  }

  private final ContextRewriter contextRewriter;

  public ConversionContextVisitor(ContextRewriter contextRewriter) {
    this.contextRewriter = contextRewriter;
  }

  @Override
  public Node rewriteArrayAccess(ArrayAccess arrayAccess) {
    // unary numeric promotion context
    return new ArrayAccess(
        arrayAccess.getArrayExpression(),
        contextRewriter.rewriteUnaryNumericPromotionContext(arrayAccess.getIndexExpression()));
  }

  @Override
  public Node rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
    // assignment context
    TypeDescriptor typeDescriptor = arrayLiteral.getTypeDescriptor();
    List<Expression> valueExpressions =
        arrayLiteral
            .getValueExpressions()
            .stream()
            .map(
                valueExpression ->
                    contextRewriter.rewriteAssignmentContext(
                        typeDescriptor.getComponentTypeDescriptor(), valueExpression))
            .collect(Collectors.toList());
    return new ArrayLiteral(typeDescriptor, valueExpressions);
  }

  @Override
  public Node rewriteAssertStatement(AssertStatement assertStatement) {
    // unary numeric promotion context
    return new AssertStatement(
        contextRewriter.rewriteUnaryNumericPromotionContext(assertStatement.getExpression()),
        assertStatement.getMessage());
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
    if (splitEnablesMoreConversions(binaryExpression)) {
      return OperatorSideEffectUtils.splitBinaryExpression(binaryExpression).accept(this);
    }

    return rewriteRegularBinaryExpression(binaryExpression);
  }

  @Override
  public Node rewriteCastExpression(CastExpression castExpression) {
    // cast context
    return contextRewriter.rewriteCastContext(castExpression);
  }

  @Override
  public Node rewriteConditionalExpression(ConditionalExpression conditionalExpression) {
    // assignment context
    TypeDescriptor typeDescriptor = conditionalExpression.getTypeDescriptor();
    return new ConditionalExpression(
        typeDescriptor,
        conditionalExpression.getConditionExpression(),
        contextRewriter.rewriteAssignmentContext(
            typeDescriptor, conditionalExpression.getTrueExpression()),
        contextRewriter.rewriteAssignmentContext(
            typeDescriptor, conditionalExpression.getFalseExpression()));
  }

  @Override
  public Node rewriteField(Field field) {
    if (field.getInitializer() == null) {
      // Nothing to rewrite.
      return field;
    }

    // assignment context
    return Field.Builder.from(field)
        .setInitializer(
            contextRewriter.rewriteAssignmentContext(
                field.getDescriptor().getTypeDescriptor(), field.getInitializer()))
        .build();
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    // method invocation context
    return MethodCall.Builder.from(methodCall)
        .setArguments(rewriteMethodInvocationContextArguments(methodCall))
        .build();
  }

  @Override
  public Node rewriteNewArray(NewArray newArray) {
    // unary numeric promotion context
    List<Expression> dimensionExpressions =
        newArray
            .getDimensionExpressions()
            .stream()
            .map(contextRewriter::rewriteUnaryNumericPromotionContext)
            .collect(Collectors.toList());
    return NewArray.newBuilder()
        .setTypeDescriptor(newArray.getTypeDescriptor())
        .setDimensionExpressions(dimensionExpressions)
        .setArrayLiteral(newArray.getArrayLiteral())
        .build();
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    // method invocation context
    return NewInstance.Builder.from(newInstance)
        .setArguments(rewriteMethodInvocationContextArguments(newInstance))
        .build();
  }

  @Override
  public Node rewritePostfixExpression(PostfixExpression postfixExpression) {
    if (splitEnablesMoreConversions(postfixExpression)) {
      return OperatorSideEffectUtils.splitPostfixExpression(postfixExpression).accept(this);
    }

    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(postfixExpression.getTypeDescriptor())) {
      return PostfixExpression.newBuilder()
          .setTypeDescriptor(postfixExpression.getTypeDescriptor())
          .setOperand(
              contextRewriter.rewriteUnaryNumericPromotionContext(postfixExpression.getOperand()))
          .setOperator(postfixExpression.getOperator())
          .build();
    }
    return postfixExpression;
  }

  @Override
  public Node rewritePrefixExpression(PrefixExpression prefixExpression) {
    if (splitEnablesMoreConversions(prefixExpression)) {
      return OperatorSideEffectUtils.splitPrefixExpression(prefixExpression).accept(this);
    }

    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(prefixExpression)) {
      return PrefixExpression.newBuilder()
          .setTypeDescriptor(prefixExpression.getTypeDescriptor())
          .setOperand(
              contextRewriter.rewriteUnaryNumericPromotionContext(prefixExpression.getOperand()))
          .setOperator(prefixExpression.getOperator())
          .build();
    }
    return prefixExpression;
  }

  @Override
  public Node rewriteReturnStatement(ReturnStatement returnStatement) {
    if (returnStatement.getExpression() == null) {
      // Nothing to rewrite.
      return returnStatement;
    }

    // assignment context
    return new ReturnStatement(
        contextRewriter.rewriteAssignmentContext(
            returnStatement.getTypeDescriptor(), returnStatement.getExpression()),
        returnStatement.getTypeDescriptor());
  }

  @Override
  public Node rewriteSwitchStatement(SwitchStatement switchStatement) {
    // unary numeric promotion
    return new SwitchStatement(
        contextRewriter.rewriteUnaryNumericPromotionContext(switchStatement.getSwitchExpression()),
        switchStatement.getBodyStatements());
  }

  @Override
  public Node rewriteVariableDeclarationFragment(VariableDeclarationFragment variableDeclaration) {
    if (variableDeclaration.getInitializer() == null) {
      // Nothing to rewrite.
      return variableDeclaration;
    }

    // assignment context
    return new VariableDeclarationFragment(
        variableDeclaration.getVariable(),
        contextRewriter.rewriteAssignmentContext(
            variableDeclaration.getVariable().getTypeDescriptor(),
            variableDeclaration.getInitializer()));
  }

  private BinaryExpression rewriteRegularBinaryExpression(BinaryExpression binaryExpression) {
    // TODO: find out if what we do here in letting multiple conversion contexts perform changes on
    // the same binary expression, all in one pass, is the right thing or the wrong thing.

    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();
    // assignment context
    if (AstUtils.matchesAssignmentContext(binaryExpression.getOperator())) {
      rightOperand =
          contextRewriter.rewriteAssignmentContext(leftOperand.getTypeDescriptor(), rightOperand);
    }

    // binary numeric promotion context
    if (AstUtils.matchesBinaryNumericPromotionContext(binaryExpression)) {
      leftOperand = contextRewriter.rewriteBinaryNumericPromotionContext(leftOperand, rightOperand);
      rightOperand =
          contextRewriter.rewriteBinaryNumericPromotionContext(rightOperand, leftOperand);
    }

    // string context
    if (AstUtils.matchesStringContext(binaryExpression)) {
      leftOperand = contextRewriter.rewriteStringContext(leftOperand, rightOperand);
      rightOperand = contextRewriter.rewriteStringContext(rightOperand, leftOperand);
    }

    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(binaryExpression)) {
      leftOperand = contextRewriter.rewriteUnaryNumericPromotionContext(leftOperand);
      rightOperand = contextRewriter.rewriteUnaryNumericPromotionContext(rightOperand);
    }

    if (leftOperand != binaryExpression.getLeftOperand()
        || rightOperand != binaryExpression.getRightOperand()) {
      binaryExpression =
          BinaryExpression.newBuilder()
              .setTypeDescriptor(binaryExpression.getTypeDescriptor())
              .setLeftOperand(leftOperand)
              .setOperator(binaryExpression.getOperator())
              .setRightOperand(rightOperand)
              .build();
    }
    return binaryExpression;
  }

  private List<Expression> rewriteMethodInvocationContextArguments(Invocation invocation) {
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        invocation.getTarget().getParameterTypeDescriptors();
    List<Expression> argumentExpressions = invocation.getArguments();

    // Look at each param/argument pair.
    List<Expression> newArgumentExpressions = new ArrayList<>();
    for (int argIndex = 0; argIndex < parameterTypeDescriptors.size(); argIndex++) {
      TypeDescriptor parameterTypeDescriptor = parameterTypeDescriptors.get(argIndex);
      Expression argumentExpression = argumentExpressions.get(argIndex);
      newArgumentExpressions.add(
          contextRewriter.rewriteMethodInvocationContext(
              parameterTypeDescriptor, argumentExpression));
    }
    return newArgumentExpressions;
  }

  private boolean splitEnablesMoreConversions(BinaryExpression binaryExpression) {
    if (!binaryExpression.getOperator().isCompoundAssignment()) {
      return false;
    }
    BinaryExpression assignmentRightOperand =
        BinaryExpression.Builder.from(binaryExpression)
            .setOperator(binaryExpression.getOperator().getUnderlyingBinaryOperator())
            .build();
    // The assignment operation retains the type of the original compound operation, which
    // in turn must be equal to the type of the lvalue (lhs).
    BinaryExpression assignmentExpression =
        BinaryExpression.Builder.from(binaryExpression)
            .setOperator(BinaryOperator.ASSIGN)
            .setRightOperand(assignmentRightOperand)
            .build();
    return rewriteRegularBinaryExpression(assignmentExpression) != assignmentExpression
        || rewriteRegularBinaryExpression(assignmentRightOperand) != assignmentRightOperand;
  }

  private boolean splitEnablesMoreConversions(UnaryExpression unaryExpression) {
    if (!unaryExpression.getOperator().hasSideEffect()) {
      return false;
    }
    Expression operand = unaryExpression.getOperand();
    BinaryExpression assignmentRightOperand =
        BinaryExpression.newBuilder()
            .setTypeDescriptor(unaryExpression.getTypeDescriptor())
            .setLeftOperand(operand)
            .setOperator(unaryExpression.getOperator().getUnderlyingBinaryOperator())
            .setRightOperand(OperatorSideEffectUtils.createLiteralOne(operand.getTypeDescriptor()))
            .build();
    // The assignment operation retains the type of the original compound operation, which
    // in turn must be equal to the type of the lvalue.
    BinaryExpression assignmentExpression =
        BinaryExpression.newBuilder()
            .setTypeDescriptor(operand.getTypeDescriptor())
            .setLeftOperand(operand)
            .setOperator(BinaryOperator.ASSIGN)
            .setRightOperand(assignmentRightOperand)
            .build();
    return rewriteRegularBinaryExpression(assignmentExpression) != assignmentExpression
        || rewriteRegularBinaryExpression(assignmentRightOperand) != assignmentRightOperand;
  }
}
