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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.ConditionalExpression;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrimitiveTypes;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.UnaryExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.WhileStatement;
import java.util.ArrayList;
import java.util.List;

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

    /** Expression is going to the given type. */
    @SuppressWarnings("unused")
    public Expression rewriteMethodInvocationContext(
        ParameterDescriptor parameterDescriptor, Expression argumentExpression) {
      return argumentExpression;
    }

    /** Expression is always going to String. */
    @SuppressWarnings("unused")
    public Expression rewriteStringContext(Expression operandExpression) {
      return operandExpression;
    }

    /**
     * Expression is always going to primitive.
     */
    public Expression rewriteUnaryNumericPromotionContext(Expression operandExpression) {
      return operandExpression;
    }

    /** Expression is always going to boolean. */
    public Expression rewriteBooleanConversionContext(Expression operandExpression) {
      return operandExpression;
    }
  }

  private final ContextRewriter contextRewriter;

  public ConversionContextVisitor(ContextRewriter contextRewriter) {
    this.contextRewriter = contextRewriter;
  }

  @Override
  public ArrayAccess rewriteArrayAccess(ArrayAccess arrayAccess) {
    // unary numeric promotion context
    return ArrayAccess.newBuilder()
        .setArrayExpression(arrayAccess.getArrayExpression())
        .setIndexExpression(
            contextRewriter.rewriteUnaryNumericPromotionContext(arrayAccess.getIndexExpression()))
        .build();
  }

  @Override
  public ArrayLiteral rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
    // assignment context
    ArrayTypeDescriptor typeDescriptor = arrayLiteral.getTypeDescriptor();
    List<Expression> valueExpressions =
        arrayLiteral
            .getValueExpressions()
            .stream()
            .map(
                valueExpression ->
                    contextRewriter.rewriteAssignmentContext(
                        typeDescriptor.getComponentTypeDescriptor(), valueExpression))
            .collect(toImmutableList());
    return new ArrayLiteral(typeDescriptor, valueExpressions);
  }

  @Override
  public AssertStatement rewriteAssertStatement(AssertStatement assertStatement) {
    // unary numeric promotion context
    return AssertStatement.newBuilder()
        .setSourcePosition(assertStatement.getSourcePosition())
        .setExpression(
            contextRewriter.rewriteBooleanConversionContext(assertStatement.getExpression()))
        .setMessage(assertStatement.getMessage())
        .build();
  }

  @Override
  public BinaryExpression rewriteBinaryExpression(BinaryExpression binaryExpression) {
    // TODO(rluble): find out if what we do here in letting multiple conversion contexts perform
    // changes on the same binary expression, all in one pass, is the right thing or the wrong
    // thing.

    Expression leftOperand = binaryExpression.getLeftOperand();
    Expression rightOperand = binaryExpression.getRightOperand();
    // assignment context
    if (AstUtils.matchesAssignmentContext(binaryExpression.getOperator())) {
      rightOperand =
          contextRewriter.rewriteAssignmentContext(leftOperand.getTypeDescriptor(), rightOperand);
    }

    // binary numeric promotion context
    if (AstUtils.matchesBinaryNumericPromotionContext(binaryExpression)) {
      if (!binaryExpression.getOperator().isCompoundAssignment()) {
        leftOperand =
            contextRewriter.rewriteBinaryNumericPromotionContext(leftOperand, rightOperand);
      }
      rightOperand =
          contextRewriter.rewriteBinaryNumericPromotionContext(rightOperand, leftOperand);
    }

    // string context
    if (AstUtils.matchesStringContext(binaryExpression)) {
      if (!binaryExpression.getOperator().isCompoundAssignment()) {
        leftOperand = contextRewriter.rewriteStringContext(leftOperand);
      }
      rightOperand = contextRewriter.rewriteStringContext(rightOperand);
    }

    /* See JLS 5.6.1. */
    if (binaryExpression.getOperator().isShiftOperator()) {
      if (!binaryExpression.getOperator().isCompoundAssignment()) {
        // the left operand matches a unary numeric promotion context.
        leftOperand = contextRewriter.rewriteUnaryNumericPromotionContext(leftOperand);
      }
      // The left operand of a shift will be always treated as an int as the maximum shift distance
      // is 64.
      rightOperand = contextRewriter.rewriteAssignmentContext(PrimitiveTypes.INT, rightOperand);
    }

    // boolean context
    if (AstUtils.matchesBooleanConversionContext(binaryExpression.getOperator())) {
      if (!binaryExpression.getOperator().isCompoundAssignment()) {
        leftOperand = contextRewriter.rewriteBooleanConversionContext(leftOperand);
      }
      rightOperand = contextRewriter.rewriteBooleanConversionContext(rightOperand);
    }

    if (leftOperand != binaryExpression.getLeftOperand()
        || rightOperand != binaryExpression.getRightOperand()) {
      binaryExpression =
          BinaryExpression.newBuilder()
              .setLeftOperand(leftOperand)
              .setOperator(binaryExpression.getOperator())
              .setRightOperand(rightOperand)
              .build();
    }
    return binaryExpression;
  }

  @Override
  public Expression rewriteCastExpression(CastExpression castExpression) {
    // cast context
    return contextRewriter.rewriteCastContext(castExpression);
  }

  @Override
  public ConditionalExpression rewriteConditionalExpression(
      ConditionalExpression conditionalExpression) {
    // assignment context
    TypeDescriptor typeDescriptor = conditionalExpression.getTypeDescriptor();
    return ConditionalExpression.newBuilder()
        .setTypeDescriptor(typeDescriptor)
        .setConditionExpression(
            contextRewriter.rewriteBooleanConversionContext(
                conditionalExpression.getConditionExpression()))
        .setTrueExpression(
            contextRewriter.rewriteAssignmentContext(
                typeDescriptor, conditionalExpression.getTrueExpression()))
        .setFalseExpression(
            contextRewriter.rewriteAssignmentContext(
                typeDescriptor, conditionalExpression.getFalseExpression()))
        .build();
  }

  @Override
  public Field rewriteField(Field field) {
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
  public DoWhileStatement rewriteDoWhileStatement(DoWhileStatement doWhileStatement) {
    return DoWhileStatement.newBuilder()
        .setSourcePosition(doWhileStatement.getSourcePosition())
        .setConditionExpression(
            contextRewriter.rewriteBooleanConversionContext(
                doWhileStatement.getConditionExpression()))
        .setBody(doWhileStatement.getBody())
        .build();
  }

  @Override
  public ForStatement rewriteForStatement(ForStatement forStatement) {
    if (forStatement.getConditionExpression() == null) {
      return forStatement;
    }
    return ForStatement.Builder.from(forStatement)
        .setConditionExpression(
            contextRewriter.rewriteBooleanConversionContext(forStatement.getConditionExpression()))
        .build();
  }

  @Override
  public IfStatement rewriteIfStatement(IfStatement ifStatement) {
    return IfStatement.newBuilder()
        .setSourcePosition(ifStatement.getSourcePosition())
        .setConditionExpression(
            contextRewriter.rewriteBooleanConversionContext(ifStatement.getConditionExpression()))
        .setThenStatement(ifStatement.getThenStatement())
        .setElseStatement(ifStatement.getElseStatement())
        .build();
  }

  @Override
  public MethodCall rewriteMethodCall(MethodCall methodCall) {
    // method invocation context
    return MethodCall.Builder.from(methodCall)
        .setArguments(rewriteMethodInvocationContextArguments(methodCall))
        .build();
  }

  @Override
  public NewArray rewriteNewArray(NewArray newArray) {
    // unary numeric promotion context
    List<Expression> dimensionExpressions =
        newArray
            .getDimensionExpressions()
            .stream()
            .map(contextRewriter::rewriteUnaryNumericPromotionContext)
            .collect(toImmutableList());
    return NewArray.newBuilder()
        .setTypeDescriptor(newArray.getTypeDescriptor())
        .setDimensionExpressions(dimensionExpressions)
        .setArrayLiteral(newArray.getArrayLiteral())
        .build();
  }

  @Override
  public NewInstance rewriteNewInstance(NewInstance newInstance) {
    // method invocation context
    return NewInstance.Builder.from(newInstance)
        .setArguments(rewriteMethodInvocationContextArguments(newInstance))
        .build();
  }

  @Override
  public UnaryExpression rewritePostfixExpression(PostfixExpression postfixExpression) {
    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(postfixExpression.getTypeDescriptor())) {
      return PostfixExpression.newBuilder()
          .setOperand(
              contextRewriter.rewriteUnaryNumericPromotionContext(postfixExpression.getOperand()))
          .setOperator(postfixExpression.getOperator())
          .build();
    }
    return postfixExpression;
  }

  @Override
  public UnaryExpression rewritePrefixExpression(PrefixExpression prefixExpression) {
    // unary numeric promotion context
    if (AstUtils.matchesBooleanConversionContext(prefixExpression.getOperator())) {
      return PrefixExpression.newBuilder()
          .setOperand(
              contextRewriter.rewriteBooleanConversionContext(prefixExpression.getOperand()))
          .setOperator(prefixExpression.getOperator())
          .build();
    }

    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(prefixExpression)) {
      return PrefixExpression.newBuilder()
          .setOperand(
              contextRewriter.rewriteUnaryNumericPromotionContext(prefixExpression.getOperand()))
          .setOperator(prefixExpression.getOperator())
          .build();
    }
    return prefixExpression;
  }

  @Override
  public ReturnStatement rewriteReturnStatement(ReturnStatement returnStatement) {
    if (returnStatement.getExpression() == null) {
      // Nothing to rewrite.
      return returnStatement;
    }

    // assignment context
    return ReturnStatement.newBuilder()
        .setExpression(
            contextRewriter.rewriteAssignmentContext(
                returnStatement.getTypeDescriptor(), returnStatement.getExpression()))
        .setTypeDescriptor(returnStatement.getTypeDescriptor())
        .setSourcePosition(returnStatement.getSourcePosition())
        .build();
  }

  @Override
  public SwitchStatement rewriteSwitchStatement(SwitchStatement switchStatement) {
    // unary numeric promotion
    return SwitchStatement.newBuilder()
        .setSourcePosition(switchStatement.getSourcePosition())
        .setSwitchExpression(
            contextRewriter.rewriteUnaryNumericPromotionContext(
                switchStatement.getSwitchExpression()))
        .setCases(switchStatement.getCases())
        .build();
  }

  @Override
  public VariableDeclarationFragment rewriteVariableDeclarationFragment(
      VariableDeclarationFragment variableDeclaration) {
    if (variableDeclaration.getInitializer() == null) {
      // Nothing to rewrite.
      return variableDeclaration;
    }

    // assignment context
    return VariableDeclarationFragment.newBuilder()
        .setVariable(variableDeclaration.getVariable())
        .setInitializer(
            contextRewriter.rewriteAssignmentContext(
                variableDeclaration.getVariable().getTypeDescriptor(),
                variableDeclaration.getInitializer()))
        .build();
  }

  @Override
  public WhileStatement rewriteWhileStatement(WhileStatement whileStatement) {
    return WhileStatement.newBuilder()
        .setSourcePosition(whileStatement.getSourcePosition())
        .setConditionExpression(
            contextRewriter.rewriteBooleanConversionContext(
                whileStatement.getConditionExpression()))
        .setBody(whileStatement.getBody())
        .build();
  }

  private List<Expression> rewriteMethodInvocationContextArguments(Invocation invocation) {
    ImmutableList<ParameterDescriptor> parameterDescriptors =
        invocation.getTarget().getParameterDescriptors();
    List<Expression> argumentExpressions = invocation.getArguments();

    // Look at each param/argument pair.
    List<Expression> newArgumentExpressions = new ArrayList<>();
    for (int argIndex = 0; argIndex < parameterDescriptors.size(); argIndex++) {
      ParameterDescriptor parameterDescriptor = parameterDescriptors.get(argIndex);
      Expression argumentExpression = argumentExpressions.get(argIndex);
      newArgumentExpressions.add(
          contextRewriter.rewriteMethodInvocationContext(parameterDescriptor, argumentExpression));
    }
    return newArgumentExpressions;
  }
}
