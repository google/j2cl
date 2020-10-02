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
package com.google.j2cl.transpiler.passes;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.ArrayAccess;
import com.google.j2cl.ast.ArrayLength;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.AssertStatement;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.BinaryExpression;
import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.BreakStatement;
import com.google.j2cl.ast.CastExpression;
import com.google.j2cl.ast.ConditionalExpression;
import com.google.j2cl.ast.ContinueStatement;
import com.google.j2cl.ast.DoWhileStatement;
import com.google.j2cl.ast.EmptyStatement;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.ExpressionStatement;
import com.google.j2cl.ast.ExpressionWithComment;
import com.google.j2cl.ast.Field;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.ForStatement;
import com.google.j2cl.ast.FunctionExpression;
import com.google.j2cl.ast.IfStatement;
import com.google.j2cl.ast.InstanceOfExpression;
import com.google.j2cl.ast.Invocation;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.JsDocCastExpression;
import com.google.j2cl.ast.LabeledStatement;
import com.google.j2cl.ast.Literal;
import com.google.j2cl.ast.MemberDescriptor;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.ast.MultiExpression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.NewInstance;
import com.google.j2cl.ast.PostfixExpression;
import com.google.j2cl.ast.PrefixExpression;
import com.google.j2cl.ast.PrimitiveTypes;
import com.google.j2cl.ast.ReturnStatement;
import com.google.j2cl.ast.Statement;
import com.google.j2cl.ast.SuperReference;
import com.google.j2cl.ast.SwitchStatement;
import com.google.j2cl.ast.SynchronizedStatement;
import com.google.j2cl.ast.ThisReference;
import com.google.j2cl.ast.ThrowStatement;
import com.google.j2cl.ast.TryStatement;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.UnaryExpression;
import com.google.j2cl.ast.VariableDeclarationExpression;
import com.google.j2cl.ast.VariableDeclarationFragment;
import com.google.j2cl.ast.VariableReference;
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

  /** Base class for defining how to insert a conversion operation in a given conversion context. */
  protected abstract static class ContextRewriter {

    /**
     * An {@code expression} is being used as if it was of a particular type.
     *
     * <p>E.g an assignment like {@code Integer i = 3}, where the {@code int} expression 3 is being
     * used as if it was of {@code Integer} reference type. Thus it might require conversion.
     *
     * <p>But it is also applicable to a receiver in a method call, e.g.
     *
     * <p>
     *
     * <pre><code>
     *    <T extends A> m(...) {
     *      T t;
     *      t.methodOfA()
     *    }
     * </code>
     * </pre>
     *
     * <p>where {@code t} might be used as if it were of type {@code A} but that is not guaranteed
     * at runtime.
     */
    @SuppressWarnings("unused")
    public Expression rewriteTypeConversionContext(
        TypeDescriptor inferredTypeDescriptor,
        TypeDescriptor actualTypeDescriptor,
        Expression expression) {
      return expression;
    }

    /** An {@code expression} that has been assigned to a field or variable of a particular type. */
    @SuppressWarnings("unused")
    public Expression rewriteAssignmentContext(
        TypeDescriptor inferredTypeDescriptor,
        TypeDescriptor actualTypeDescriptor,
        Expression expression) {
      // Handle generically as a type conversion context.
      return rewriteTypeConversionContext(inferredTypeDescriptor, actualTypeDescriptor, expression);
    }

    /**
     * An {@code expression} is an operand of a binary numeric expression where the other operand is
     * of type {@code otherOperandTypeDescriptor}.
     */
    @SuppressWarnings("unused")
    public Expression rewriteBinaryNumericPromotionContext(
        TypeDescriptor otherOperandTypeDescriptor, Expression operand) {
      return operand;
    }

    /** An {@code expression} is of a JsEnum type and needs boxing. */
    public Expression rewriteJsEnumBoxingConversionContext(Expression expression) {
      return expression;
    }

    /** A {@code castExpression} requesting and explicit type conversion. */
    public Expression rewriteCastContext(CastExpression castExpression) {
      return castExpression;
    }

    /** An {@code expression} that is used as a qualifier of a member of a particular type. */
    @SuppressWarnings("unused")
    public Expression rewriteMemberQualifierContext(
        TypeDescriptor inferredTypeDescriptor,
        TypeDescriptor actualTypeDescriptor,
        Expression expression) {
      // Handle generically as a type conversion context.
      return rewriteTypeConversionContext(inferredTypeDescriptor, actualTypeDescriptor, expression);
    }

    /** An {@code argument} that is passed to a method as a parameter. */
    public Expression rewriteMethodInvocationContext(
        ParameterDescriptor inferredParameterDescriptor,
        ParameterDescriptor actualParameterDescriptor,
        Expression argument) {
      // By default handle method invocation parameter passing like assignments.
      return rewriteTypeConversionContext(
          inferredParameterDescriptor.getTypeDescriptor(),
          actualParameterDescriptor.getTypeDescriptor(),
          argument);
    }

    /** An {@code expression} that is used as a string. */
    public Expression rewriteStringContext(Expression expression) {
      return expression;
    }

    /** An {@code operand} that is used in an unary numeric operation. */
    public Expression rewriteUnaryNumericPromotionContext(Expression operand) {
      return operand;
    }

    /** An {@code operand} that is used a boolean expression. */
    public Expression rewriteBooleanConversionContext(Expression operand) {
      return operand;
    }
  }

  private final ContextRewriter contextRewriter;

  public ConversionContextVisitor(ContextRewriter contextRewriter) {
    this.contextRewriter = contextRewriter;
  }

  @Override
  public ArrayAccess rewriteArrayAccess(ArrayAccess arrayAccess) {
    Expression expression = arrayAccess.getArrayExpression();

    return ArrayAccess.newBuilder()
        .setArrayExpression(
            rewriteTypeConversionContextWithoutDeclaration(
                expression.getTypeDescriptor(), expression))
        .setIndexExpression(
            // The index is always int so gets rewritten with unary numeric promotion context
            contextRewriter.rewriteUnaryNumericPromotionContext(arrayAccess.getIndexExpression()))
        .build();
  }

  @Override
  public ArrayLength rewriteArrayLength(ArrayLength arrayLength) {
    Expression expression = arrayLength.getArrayExpression();

    return ArrayLength.Builder.from(arrayLength)
        .setArrayExpression(
            rewriteTypeConversionContextWithoutDeclaration(
                expression.getTypeDescriptor(), expression))
        .build();
  }

  @Override
  public ArrayLiteral rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
    // assignment context
    ArrayTypeDescriptor typeDescriptor = arrayLiteral.getTypeDescriptor();
    List<Expression> valueExpressions =
        arrayLiteral.getValueExpressions().stream()
            .map(
                valueExpression ->
                    rewriteTypeConversionContextWithoutDeclaration(
                        typeDescriptor.getComponentTypeDescriptor(), valueExpression))
            .collect(toImmutableList());
    return new ArrayLiteral(typeDescriptor, valueExpressions);
  }

  @Override
  public AssertStatement rewriteAssertStatement(AssertStatement assertStatement) {
    Expression message = assertStatement.getMessage();
    // Treat the second parameter as a string context for primitives to enforce the right conversion
    // rules, but let objects flow so that exceptions are allowed and handled correctly.
    boolean isMessageStringContext = message != null && message.getTypeDescriptor().isPrimitive();
    return AssertStatement.newBuilder()
        .setSourcePosition(assertStatement.getSourcePosition())
        .setExpression(
            contextRewriter.rewriteBooleanConversionContext(assertStatement.getExpression()))
        .setMessage(
            isMessageStringContext ? contextRewriter.rewriteStringContext(message) : message)
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
          contextRewriter.rewriteAssignmentContext(
              leftOperand.getTypeDescriptor(),
              leftOperand.getDeclaredTypeDescriptor(),
              rightOperand);
    }

    // binary numeric promotion context
    if (AstUtils.matchesBinaryNumericPromotionContext(binaryExpression)) {
      if (!binaryExpression.getOperator().isCompoundAssignment()) {
        leftOperand =
            contextRewriter.rewriteBinaryNumericPromotionContext(
                rightOperand.getTypeDescriptor(), leftOperand);
      }
      rightOperand =
          contextRewriter.rewriteBinaryNumericPromotionContext(
              leftOperand.getTypeDescriptor(), rightOperand);
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
      rightOperand =
          rewriteTypeConversionContextWithoutDeclaration(PrimitiveTypes.INT, rightOperand);
    }

    // boolean context
    if (AstUtils.matchesBooleanConversionContext(binaryExpression.getOperator())) {
      if (!binaryExpression.getOperator().isCompoundAssignment()) {
        leftOperand = contextRewriter.rewriteBooleanConversionContext(leftOperand);
      }
      rightOperand = contextRewriter.rewriteBooleanConversionContext(rightOperand);
    }

    // JsEnum boxing conversion context.
    if (AstUtils.matchesJsEnumBoxingConversionContext(binaryExpression)) {
      if (AstUtils.isNonNativeJsEnum(leftOperand.getDeclaredTypeDescriptor())) {
        leftOperand = contextRewriter.rewriteJsEnumBoxingConversionContext(leftOperand);
      }
      if (AstUtils.isNonNativeJsEnum(rightOperand.getDeclaredTypeDescriptor())) {
        rightOperand = contextRewriter.rewriteJsEnumBoxingConversionContext(rightOperand);
      }
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
            rewriteTypeConversionContextWithoutDeclaration(
                typeDescriptor, conditionalExpression.getTrueExpression()))
        .setFalseExpression(
            rewriteTypeConversionContextWithoutDeclaration(
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
                field.getDescriptor().getTypeDescriptor(),
                field.getDescriptor().getDeclarationDescriptor().getTypeDescriptor(),
                field.getInitializer()))
        .build();
  }

  @Override
  public FieldAccess rewriteFieldAccess(FieldAccess fieldAccess) {
    return FieldAccess.Builder.from(fieldAccess)
        .setQualifier(rewriteInstanceQualifier(fieldAccess.getQualifier(), fieldAccess.getTarget()))
        .build();
  }

  private Expression rewriteInstanceQualifier(
      Expression qualifier, MemberDescriptor memberDescriptor) {
    if (memberDescriptor.isStatic()) {
      return qualifier;
    }

    return contextRewriter.rewriteMemberQualifierContext(
        memberDescriptor.getEnclosingTypeDescriptor(),
        memberDescriptor.getDeclarationDescriptor().getEnclosingTypeDescriptor(),
        qualifier);
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
  public Expression rewriteExpression(Expression expression) {
    // Every expression needs to be handled explicitly or excluded here. This is to ensure when new
    // expressions are added to the AST that a conscious decision is made, and avoid the implicit
    // noop rewriting.
    // Expressions that don't need handling include:
    //    - literals (including class literals)
    //    - references (variable references, this and super)
    //    - jsdoc casts (since they are used to specifically avoid rewriting)
    //    - any other expression that only requires its subexpressions to be handled, but don't need
    //      any rewriting themselves (MultiExpression, ExpressionWithComment, VariableDeclarations)

    if (expression instanceof Literal // literals
        || expression instanceof JavaScriptConstructorReference
        // expressions that needs only subexpressions to be handled
        || expression instanceof MultiExpression
        || expression instanceof ExpressionWithComment
        || expression instanceof FunctionExpression
        || expression instanceof VariableDeclarationExpression
        // jsdoc casts
        || expression instanceof JsDocCastExpression
        // references
        || expression instanceof ThisReference
        || expression instanceof SuperReference
        || expression instanceof VariableReference) {
      // These expressions do not need rewriting.
      return expression;
    }
    throw new IllegalStateException();
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
  public InstanceOfExpression rewriteInstanceOfExpression(
      InstanceOfExpression instanceOfExpression) {

    if (AstUtils.matchesJsEnumBoxingConversionContext(instanceOfExpression)) {
      Expression expression =
          contextRewriter.rewriteJsEnumBoxingConversionContext(
              instanceOfExpression.getExpression());

      if (expression != instanceOfExpression.getExpression()) {
        return InstanceOfExpression.Builder.from(instanceOfExpression)
            .setExpression(expression)
            .build();
      }
    }

    return instanceOfExpression;
  }

  @Override
  public MethodCall rewriteMethodCall(MethodCall methodCall) {
    // method invocation context
    return MethodCall.Builder.from(methodCall)
        .setQualifier(rewriteInstanceQualifier(methodCall.getQualifier(), methodCall.getTarget()))
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
            rewriteTypeConversionContextWithoutDeclaration(
                returnStatement.getTypeDescriptor(), returnStatement.getExpression()))
        .setTypeDescriptor(returnStatement.getTypeDescriptor())
        .setSourcePosition(returnStatement.getSourcePosition())
        .build();
  }

  @Override
  public Statement rewriteStatement(Statement statement) {
    // Every statement needs to be handled explicitly or excluded here.
    if (statement instanceof ExpressionStatement
        || statement instanceof Block
        || statement instanceof BreakStatement
        || statement instanceof ContinueStatement
        || statement instanceof TryStatement
        || statement instanceof ThrowStatement
        || statement instanceof EmptyStatement
        || statement instanceof LabeledStatement
        || statement instanceof SynchronizedStatement) {
      // These statements do not need rewriting.
      return statement;
    }
    throw new IllegalStateException();
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
            rewriteTypeConversionContextWithoutDeclaration(
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

  private Expression rewriteTypeConversionContextWithoutDeclaration(
      TypeDescriptor toTypeDescriptor, Expression expression) {
    return contextRewriter.rewriteTypeConversionContext(
        toTypeDescriptor, toTypeDescriptor, expression);
  }

  private List<Expression> rewriteMethodInvocationContextArguments(Invocation invocation) {
    ImmutableList<ParameterDescriptor> inferredParameterDescriptors =
        invocation.getTarget().getParameterDescriptors();
    ImmutableList<ParameterDescriptor> declaredParameterDescriptors =
        invocation.getTarget().getDeclarationDescriptor().getParameterDescriptors();
    List<Expression> argumentExpressions = invocation.getArguments();

    // Look at each param/argument pair.
    List<Expression> newArgumentExpressions = new ArrayList<>();
    for (int argIndex = 0; argIndex < inferredParameterDescriptors.size(); argIndex++) {
      ParameterDescriptor inferredParameterDescriptor = inferredParameterDescriptors.get(argIndex);
      ParameterDescriptor declaredParameterDescriptor = declaredParameterDescriptors.get(argIndex);
      Expression argumentExpression = argumentExpressions.get(argIndex);
      newArgumentExpressions.add(
          contextRewriter.rewriteMethodInvocationContext(
              inferredParameterDescriptor, declaredParameterDescriptor, argumentExpression));
    }
    return newArgumentExpressions;
  }
}
