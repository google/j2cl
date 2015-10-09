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
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.Call;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.OperatorSideEffectUtils;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.TernaryExpression;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Driver for rewriting conversions in different contexts.
 *
 * <p>Traverses the AST, recognizing and categorizing different conversion contexts and dispatching
 * conversion requests in that context.
 */
public class ConversionContextVisitor extends AbstractRewriter {

  public void run(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

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
     * Expression is always going to primitive.
     */
    public Expression rewriteBinaryNumericPromotionContext(Expression operandExpression) {
      return operandExpression;
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
    public Expression rewriteStringContext(Expression operandExpression) {
      return operandExpression;
    }

    /**
     * Expression is always going to primitive.
     */
    public Expression rewriteUnaryNumericPromotionContext(Expression operandExpression) {
      return operandExpression;
    }

    public void run(CompilationUnit compilationUnit) {
      new ConversionContextVisitor(this).run(compilationUnit);
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
    ArrayTypeDescriptor typeDescriptor = arrayLiteral.getTypeDescriptor();
    List<Expression> valueExpressions = new ArrayList<>();
    for (Expression valueExpression : arrayLiteral.getValueExpressions()) {
      valueExpressions.add(
          contextRewriter.rewriteAssignmentContext(
              typeDescriptor.getComponentTypeDescriptor(), valueExpression));
    }
    return new ArrayLiteral(typeDescriptor, valueExpressions);
  }

  @Override
  public Node rewriteBinaryExpression(BinaryExpression binaryExpression) {
    if (splitEnablesBinaryPromotionConversion(binaryExpression)
        || splitEnablesAssignmentConversion(binaryExpression)) {
      Expression splitExpression = OperatorSideEffectUtils.splitBinaryExpression(binaryExpression);
      splitExpression.accept(this);
      return splitExpression;
    }

    // TODO: find out if what we do here in letting multiple conversion contexts perform changes on
    // the same binary expression, all in one pass, is the right thing or the wrong thing.

    // binary numeric promotion context
    if (AstUtils.matchesBinaryNumericPromotionContext(binaryExpression)) {
      binaryExpression =
          new BinaryExpression(
              binaryExpression.getTypeDescriptor(),
              contextRewriter.rewriteBinaryNumericPromotionContext(
                  binaryExpression.getLeftOperand()),
              binaryExpression.getOperator(),
              contextRewriter.rewriteBinaryNumericPromotionContext(
                  binaryExpression.getRightOperand()));
    }

    // assignment context
    if (AstUtils.matchesAssignmentContext(binaryExpression.getOperator())) {
      binaryExpression =
          new BinaryExpression(
              binaryExpression.getTypeDescriptor(),
              binaryExpression.getLeftOperand(),
              binaryExpression.getOperator(),
              contextRewriter.rewriteAssignmentContext(
                  binaryExpression.getLeftOperand().getTypeDescriptor(),
                  binaryExpression.getRightOperand()));
    }

    // string context
    if (AstUtils.matchesStringContext(binaryExpression)) {
      binaryExpression =
          new BinaryExpression(
              binaryExpression.getTypeDescriptor(),
              contextRewriter.rewriteStringContext(binaryExpression.getLeftOperand()),
              binaryExpression.getOperator(),
              contextRewriter.rewriteStringContext(binaryExpression.getRightOperand()));
    }

    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(binaryExpression)) {
      binaryExpression =
          new BinaryExpression(
              binaryExpression.getTypeDescriptor(),
              contextRewriter.rewriteUnaryNumericPromotionContext(
                  binaryExpression.getLeftOperand()),
              binaryExpression.getOperator(),
              contextRewriter.rewriteUnaryNumericPromotionContext(
                  binaryExpression.getRightOperand()));
    }

    return binaryExpression;
  }

  @Override
  public Node rewriteCastExpression(CastExpression castExpression) {
    // cast context
    return contextRewriter.rewriteCastContext(castExpression);
  }

  @Override
  public Node rewriteField(Field field) {
    if (field.getInitializer() == null) {
      // Nothing to rewrite.
      return field;
    }

    // assignment context
    Field newField =
        new Field(
            field.getDescriptor(),
            contextRewriter.rewriteAssignmentContext(
                field.getDescriptor().getTypeDescriptor(), field.getInitializer()));
    newField.setCapturedVariable(field.getCapturedVariable());
    newField.setCompileTimeConstant(field.isCompileTimeConstant());
    return newField;
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    // method invocation context
    return new MethodCall(
        methodCall.getQualifier(),
        methodCall.getTarget(),
        rewriteMethodInvocationContextArguments(methodCall));
  }

  @Override
  public Node rewriteNewArray(NewArray newArray) {
    // unary numeric promotion context
    List<Expression> dimensionExpressions = new ArrayList<>();
    for (Expression dimensionExpression : newArray.getDimensionExpressions()) {
      dimensionExpressions.add(
          contextRewriter.rewriteUnaryNumericPromotionContext(dimensionExpression));
    }
    return new NewArray(
        newArray.getTypeDescriptor(), dimensionExpressions, newArray.getArrayLiteral());
  }

  @Override
  public Node rewriteNewInstance(NewInstance newInstance) {
    // method invocation context
    return new NewInstance(
        newInstance.getQualifier(),
        newInstance.getTarget(),
        rewriteMethodInvocationContextArguments(newInstance));
  }

  @Override
  // TODO: check assignment context as well for compound assignment operators?
  public Node rewritePostfixExpression(PostfixExpression postfixExpression) {
    if (splitEnablesUnaryPromotionConversion(postfixExpression)) {
      Expression splitExpression =
          OperatorSideEffectUtils.splitPostfixExpression(postfixExpression);
      splitExpression.accept(this);
      return splitExpression;
    }

    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(postfixExpression.getTypeDescriptor())) {
      return new PostfixExpression(
          postfixExpression.getTypeDescriptor(),
          contextRewriter.rewriteUnaryNumericPromotionContext(postfixExpression.getOperand()),
          postfixExpression.getOperator());
    }

    return postfixExpression;
  }

  @Override
  // TODO: check assignment context as well for compound assignment operators?
  public Node rewritePrefixExpression(PrefixExpression prefixExpression) {
    if (splitEnablesUnaryPromotionConversion(prefixExpression)) {
      Expression splitExpression = OperatorSideEffectUtils.splitPrefixExpression(prefixExpression);
      splitExpression.accept(this);
      return splitExpression;
    }

    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(prefixExpression)) {
      return new PrefixExpression(
          prefixExpression.getTypeDescriptor(),
          contextRewriter.rewriteUnaryNumericPromotionContext(prefixExpression.getOperand()),
          prefixExpression.getOperator());
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
        contextRewriter.rewriteUnaryNumericPromotionContext(switchStatement.getMatchExpression()),
        switchStatement.getBodyStatements());
  }

  @Override
  public Node rewriteTernaryExpression(TernaryExpression ternaryExpression) {
    // assignment context
    TypeDescriptor typeDescriptor = ternaryExpression.getTypeDescriptor();
    return new TernaryExpression(
        typeDescriptor,
        ternaryExpression.getConditionExpression(),
        contextRewriter.rewriteAssignmentContext(
            typeDescriptor, ternaryExpression.getTrueExpression()),
        contextRewriter.rewriteAssignmentContext(
            typeDescriptor, ternaryExpression.getFalseExpression()));
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

  private List<Expression> rewriteMethodInvocationContextArguments(Call call) {
    ImmutableList<TypeDescriptor> parameterTypeDescriptors =
        call.getTarget().getParameterTypeDescriptors();
    List<Expression> argumentExpressions = call.getArguments();

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

  private boolean splitEnablesAssignmentConversion(BinaryExpression binaryExpression) {
    if (!binaryExpression.getOperator().isCompoundAssignment()) {
      return false;
    }
    BinaryExpression numericRightOperand =
        new BinaryExpression(
            binaryExpression.getTypeDescriptor(),
            binaryExpression.getLeftOperand(),
            binaryExpression.getOperator().withoutAssignment(),
            binaryExpression.getRightOperand());
    return contextRewriter.rewriteAssignmentContext(
            binaryExpression.getLeftOperand().getTypeDescriptor(), numericRightOperand)
        != numericRightOperand;
  }

  private boolean splitEnablesBinaryPromotionConversion(BinaryExpression binaryExpression) {
    if (!binaryExpression.getOperator().isCompoundAssignment()) {
      return false;
    }
    if (!AstUtils.matchesBinaryNumericPromotionContext(
        binaryExpression.getLeftOperand(),
        binaryExpression.getOperator().withoutAssignment(),
        binaryExpression.getRightOperand())) {
      return false;
    }
    return contextRewriter.rewriteBinaryNumericPromotionContext(binaryExpression.getLeftOperand())
        != binaryExpression.getLeftOperand();
  }

  private boolean splitEnablesUnaryPromotionConversion(PostfixExpression postfixExpression) {
    return AstUtils.matchesUnaryNumericPromotionContext(postfixExpression.getTypeDescriptor())
        && contextRewriter.rewriteUnaryNumericPromotionContext(postfixExpression.getOperand())
            != postfixExpression.getOperand();
  }

  private boolean splitEnablesUnaryPromotionConversion(PrefixExpression prefixExpression) {
    return prefixExpression.getOperator().hasSideEffect()
        && AstUtils.matchesUnaryNumericPromotionContext(prefixExpression.getTypeDescriptor())
        && contextRewriter.rewriteUnaryNumericPromotionContext(prefixExpression.getOperand())
            != prefixExpression.getOperand();
  }
}
