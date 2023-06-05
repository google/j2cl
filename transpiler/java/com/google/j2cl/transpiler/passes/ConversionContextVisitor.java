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
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.ArrayAccess;
import com.google.j2cl.transpiler.ast.ArrayCreationReference;
import com.google.j2cl.transpiler.ast.ArrayLength;
import com.google.j2cl.transpiler.ast.ArrayLiteral;
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor;
import com.google.j2cl.transpiler.ast.AssertStatement;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.BinaryExpression;
import com.google.j2cl.transpiler.ast.Block;
import com.google.j2cl.transpiler.ast.BreakStatement;
import com.google.j2cl.transpiler.ast.CastExpression;
import com.google.j2cl.transpiler.ast.ConditionalExpression;
import com.google.j2cl.transpiler.ast.ContinueStatement;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.ExpressionWithComment;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.FieldDeclarationStatement;
import com.google.j2cl.transpiler.ast.ForEachStatement;
import com.google.j2cl.transpiler.ast.FunctionExpression;
import com.google.j2cl.transpiler.ast.IfStatement;
import com.google.j2cl.transpiler.ast.InstanceOfExpression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.JsDocCastExpression;
import com.google.j2cl.transpiler.ast.JsDocExpression;
import com.google.j2cl.transpiler.ast.LabeledStatement;
import com.google.j2cl.transpiler.ast.Literal;
import com.google.j2cl.transpiler.ast.LocalClassDeclarationStatement;
import com.google.j2cl.transpiler.ast.LoopStatement;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.MethodDescriptor.ParameterDescriptor;
import com.google.j2cl.transpiler.ast.MethodLike;
import com.google.j2cl.transpiler.ast.MethodReference;
import com.google.j2cl.transpiler.ast.MultiExpression;
import com.google.j2cl.transpiler.ast.NewArray;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.PostfixExpression;
import com.google.j2cl.transpiler.ast.PrefixExpression;
import com.google.j2cl.transpiler.ast.ReturnStatement;
import com.google.j2cl.transpiler.ast.Statement;
import com.google.j2cl.transpiler.ast.SwitchStatement;
import com.google.j2cl.transpiler.ast.SynchronizedStatement;
import com.google.j2cl.transpiler.ast.ThisOrSuperReference;
import com.google.j2cl.transpiler.ast.ThrowStatement;
import com.google.j2cl.transpiler.ast.TryStatement;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import com.google.j2cl.transpiler.ast.UnaryExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression;
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment;
import com.google.j2cl.transpiler.ast.VariableReference;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

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

    /** An {@code expression} that is of a JsEnum type and needs boxing. */
    public Expression rewriteJsEnumBoxingConversionContext(Expression expression) {
      return expression;
    }

    /** A {@code castExpression} requesting an explicit type conversion. */
    public Expression rewriteCastContext(CastExpression castExpression) {
      return castExpression;
    }

    /** An {@code expression} that is subject of a switch statement. */
    public Expression rewriteSwitchExpressionContext(Expression expression) {
      TypeDescriptor typeDescriptor = expression.getTypeDescriptor();
      if (!TypeDescriptors.isBoxedOrPrimitiveType(typeDescriptor)) {
        return expression;
      }
      return (TypeDescriptors.isJavaLangBoolean(typeDescriptor.toRawTypeDescriptor())
              || TypeDescriptors.isPrimitiveBoolean(typeDescriptor))
          ? rewriteBooleanConversionContext(expression)
          : rewriteUnaryNumericPromotionContext(expression);
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

    Expression arrayExpression =
        rewriteTypeConversionContextWithoutDeclaration(
            expression.getTypeDescriptor().toNonNullable(), expression);
    // The index is always int so gets rewritten with unary numeric promotion context
    Expression indexExpression =
        contextRewriter.rewriteUnaryNumericPromotionContext(arrayAccess.getIndexExpression());

    if (arrayExpression == arrayAccess.getArrayExpression()
        && indexExpression == arrayAccess.getIndexExpression()) {
      return arrayAccess;
    }

    return ArrayAccess.Builder.from(arrayAccess)
        .setArrayExpression(arrayExpression)
        .setIndexExpression(indexExpression)
        .build();
  }

  @Override
  public ArrayLength rewriteArrayLength(ArrayLength arrayLength) {
    Expression expression = arrayLength.getArrayExpression();

    Expression arrayExpression =
        rewriteTypeConversionContextWithoutDeclaration(
            expression.getTypeDescriptor().toNonNullable(), expression);

    if (arrayExpression == arrayLength.getArrayExpression()) {
      return arrayLength;
    }

    return ArrayLength.Builder.from(arrayLength).setArrayExpression(arrayExpression).build();
  }

  @Override
  public ArrayLiteral rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
    // assignment context
    ArrayTypeDescriptor typeDescriptor = arrayLiteral.getTypeDescriptor();
    ImmutableList<Expression> valueExpressions =
        arrayLiteral.getValueExpressions().stream()
            .map(
                valueExpression ->
                    rewriteTypeConversionContextWithoutDeclaration(
                        typeDescriptor.getComponentTypeDescriptor(), valueExpression))
            .collect(toImmutableList());

    if (valueExpressions.equals(arrayLiteral.getValueExpressions())) {
      return arrayLiteral;
    }

    return new ArrayLiteral(typeDescriptor, valueExpressions);
  }

  @Override
  public AssertStatement rewriteAssertStatement(AssertStatement assertStatement) {

    Expression expression =
        contextRewriter.rewriteBooleanConversionContext(assertStatement.getExpression());
    Expression message =
        assertStatement.getMessage() == null
            ? null
            : rewriteTypeConversionContextWithoutDeclaration(
                TypeDescriptors.get().javaLangObject, assertStatement.getMessage());

    if (message == assertStatement.getMessage() && expression == assertStatement.getExpression()) {
      return assertStatement;
    }

    return AssertStatement.Builder.from(assertStatement)
        .setExpression(expression)
        .setMessage(message)
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
    if (AstUtils.matchesAssignmentContext(binaryExpression)) {
      rightOperand =
          contextRewriter.rewriteAssignmentContext(
              leftOperand.getTypeDescriptor(),
              leftOperand.getDeclaredTypeDescriptor(),
              rightOperand);
    }

    // binary numeric promotion context
    if (AstUtils.matchesBinaryNumericPromotionContext(binaryExpression)) {
      // TODO(b/206415539): Perform the appropriate rewriting of the lhs for compound assignments.
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
      rightOperand = contextRewriter.rewriteUnaryNumericPromotionContext(rightOperand);
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

    if (leftOperand == binaryExpression.getLeftOperand()
        && rightOperand == binaryExpression.getRightOperand()) {
      return binaryExpression;
    }
    return BinaryExpression.Builder.from(binaryExpression)
        .setLeftOperand(leftOperand)
        .setRightOperand(rightOperand)
        .build();
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
    Expression conditionExpression =
        contextRewriter.rewriteBooleanConversionContext(
            conditionalExpression.getConditionExpression());
    Expression trueExpression =
        rewriteTypeConversionContextWithoutDeclaration(
            typeDescriptor, conditionalExpression.getTrueExpression());
    Expression falseExpression =
        rewriteTypeConversionContextWithoutDeclaration(
            typeDescriptor, conditionalExpression.getFalseExpression());

    if (conditionExpression == conditionalExpression.getConditionExpression()
        && trueExpression == conditionalExpression.getTrueExpression()
        && falseExpression == conditionalExpression.getFalseExpression()) {
      return conditionalExpression;
    }
    return ConditionalExpression.Builder.from(conditionalExpression)
        .setConditionExpression(conditionExpression)
        .setTrueExpression(trueExpression)
        .setFalseExpression(falseExpression)
        .build();
  }

  @Override
  public Field rewriteField(Field field) {
    if (field.getInitializer() == null) {
      // Nothing to rewrite.
      return field;
    }

    // assignment context
    Expression initializer =
        contextRewriter.rewriteAssignmentContext(
            field.getDescriptor().getTypeDescriptor(),
            field.getDescriptor().getDeclarationDescriptor().getTypeDescriptor(),
            field.getInitializer());

    if (initializer == field.getInitializer()) {
      return field;
    }

    return Field.Builder.from(field).setInitializer(initializer).build();
  }

  @Nullable
  private Expression rewriteInstanceQualifier(
      Expression qualifier, MemberDescriptor memberDescriptor) {
    if (memberDescriptor.isStatic() || qualifier == null) {
      return qualifier;
    }

    DeclaredTypeDescriptor enclosingTypeDescriptor = memberDescriptor.getEnclosingTypeDescriptor();
    DeclaredTypeDescriptor declaredEnclosingTypeDescriptor =
        memberDescriptor.getDeclarationDescriptor().getEnclosingTypeDescriptor();
    if (memberDescriptor.isConstructor()) {
      if (!enclosingTypeDescriptor.getTypeDeclaration().isCapturingEnclosingInstance()) {
        return qualifier;
      }
      // This is a constuctor call of an inner class; hence the qualifier type is the enclosing
      // class of the class where the method is defined.
      enclosingTypeDescriptor = enclosingTypeDescriptor.getEnclosingTypeDescriptor();
      declaredEnclosingTypeDescriptor =
          declaredEnclosingTypeDescriptor.getEnclosingTypeDescriptor();
    }

    return contextRewriter.rewriteMemberQualifierContext(
        enclosingTypeDescriptor.toNonNullable(),
        declaredEnclosingTypeDescriptor.toNonNullable(),
        qualifier);
  }

  @Override
  public LoopStatement rewriteLoopStatement(LoopStatement loopStatement) {
    Expression conditionExpression =
        contextRewriter.rewriteBooleanConversionContext(loopStatement.getConditionExpression());

    if (conditionExpression == loopStatement.getConditionExpression()) {
      return loopStatement;
    }

    return LoopStatement.Builder.from(loopStatement)
        .setConditionExpression(conditionExpression)
        .build();
  }

  @Override
  public ForEachStatement rewriteForEachStatement(ForEachStatement forEachStatement) {
    Expression expression = forEachStatement.getIterableExpression();
    Expression iterableExpression =
        contextRewriter.rewriteTypeConversionContext(
            expression.getTypeDescriptor().toNonNullable(),
            expression.getDeclaredTypeDescriptor().toNonNullable(),
            expression);

    if (iterableExpression == forEachStatement.getIterableExpression()) {
      return forEachStatement;
    }

    return ForEachStatement.Builder.from(forEachStatement)
        .setIterableExpression(iterableExpression)
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
        || expression instanceof JsDocExpression
        // jsdoc casts
        || expression instanceof JsDocCastExpression
        // references
        || expression instanceof ThisOrSuperReference
        || expression instanceof VariableReference
        || expression instanceof ArrayCreationReference) {
      // These expressions do not need rewriting.
      return expression;
    }
    throw new IllegalStateException();
  }

  @Override
  public IfStatement rewriteIfStatement(IfStatement ifStatement) {
    Expression conditionExpression =
        contextRewriter.rewriteBooleanConversionContext(ifStatement.getConditionExpression());

    if (conditionExpression == ifStatement.getConditionExpression()) {
      return ifStatement;
    }

    return IfStatement.Builder.from(ifStatement)
        .setConditionExpression(conditionExpression)
        .build();
  }

  @Override
  public MemberReference rewriteInvocation(Invocation invocation) {
    List<Expression> rewrittenArguments = rewriteMethodInvocationContextArguments(invocation);
    if (!rewrittenArguments.equals(invocation.getArguments())) {
      invocation = Invocation.Builder.from(invocation).setArguments(rewrittenArguments).build();
    }
    return rewriteMemberReference(invocation);
  }

  @Override
  public MemberReference rewriteMemberReference(MemberReference memberReference) {
    Expression rewrittenQualifier =
        rewriteInstanceQualifier(memberReference.getQualifier(), memberReference.getTarget());

    if (rewrittenQualifier == memberReference.getQualifier()) {
      return memberReference;
    }
    return MemberReference.Builder.from(memberReference).setQualifier(rewrittenQualifier).build();
  }

  @Override
  public Node rewriteMethodReference(MethodReference methodReference) {
    return MethodReference.Builder.from(methodReference)
        .setQualifier(
            rewriteInstanceQualifier(
                methodReference.getQualifier(), methodReference.getReferencedMethodDescriptor()))
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
  public NewArray rewriteNewArray(NewArray newArray) {
    // unary numeric promotion context
    ImmutableList<Expression> dimensionExpressions =
        newArray.getDimensionExpressions().stream()
            .map(contextRewriter::rewriteUnaryNumericPromotionContext)
            .collect(toImmutableList());
    if (dimensionExpressions.equals(newArray.getDimensionExpressions())) {
      return newArray;
    }
    return NewArray.Builder.from(newArray).setDimensionExpressions(dimensionExpressions).build();
  }

  @Override
  public UnaryExpression rewritePostfixExpression(PostfixExpression postfixExpression) {
    Expression operand = postfixExpression.getOperand();
    // unary numeric promotion context
    if (AstUtils.matchesUnaryNumericPromotionContext(postfixExpression)) {
      operand = contextRewriter.rewriteUnaryNumericPromotionContext(postfixExpression.getOperand());
    }
    if (operand == postfixExpression.getOperand()) {
      return postfixExpression;
    }

    // TODO(b/206415539): Perform the appropriate rewriting for compound assignments.
    return PostfixExpression.Builder.from(postfixExpression).setOperand(operand).build();
  }

  @Override
  public UnaryExpression rewritePrefixExpression(PrefixExpression prefixExpression) {
    Expression operand = prefixExpression.getOperand();
    if (AstUtils.matchesBooleanConversionContext(prefixExpression.getOperator())) {
      // unary boolean promotion context
      operand = contextRewriter.rewriteBooleanConversionContext(prefixExpression.getOperand());
    } else if (AstUtils.matchesUnaryNumericPromotionContext(prefixExpression)) {
      // unary numeric promotion context
      operand = contextRewriter.rewriteUnaryNumericPromotionContext(prefixExpression.getOperand());
    }

    if (operand == prefixExpression.getOperand()) {
      return prefixExpression;
    }

    // TODO(b/206415539): Perform the appropriate rewriting for compound assignments.
    return PrefixExpression.Builder.from(prefixExpression).setOperand(operand).build();
  }

  @Override
  public ReturnStatement rewriteReturnStatement(ReturnStatement returnStatement) {
    if (returnStatement.getExpression() == null) {
      // Nothing to rewrite.
      return returnStatement;
    }

    // assignment context
    Expression expression =
        contextRewriter.rewriteTypeConversionContext(
            getEnclosingMethodLike().getDescriptor().getReturnTypeDescriptor(),
            getEnclosingMethodLike()
                .getDescriptor()
                .getDeclarationDescriptor()
                .getReturnTypeDescriptor(),
            returnStatement.getExpression());

    if (expression == returnStatement.getExpression()) {
      return returnStatement;
    }

    return ReturnStatement.Builder.from(returnStatement).setExpression(expression).build();
  }

  @Override
  public Statement rewriteStatement(Statement statement) {
    // Every statement needs to be handled explicitly or excluded here.
    if (statement instanceof ExpressionStatement
        || statement instanceof Block
        || statement instanceof BreakStatement
        || statement instanceof ContinueStatement
        || statement instanceof FieldDeclarationStatement
        || statement instanceof TryStatement
        || statement instanceof LabeledStatement
        || statement instanceof LocalClassDeclarationStatement) {
      // These statements do not need rewriting.
      return statement;
    }
    throw new IllegalStateException();
  }

  @Override
  public SwitchStatement rewriteSwitchStatement(SwitchStatement switchStatement) {

    Expression switchExpression =
        contextRewriter.rewriteSwitchExpressionContext(switchStatement.getSwitchExpression());

    if (switchExpression == switchStatement.getSwitchExpression()) {
      return switchStatement;
    }

    return SwitchStatement.Builder.from(switchStatement)
        .setSwitchExpression(switchExpression)
        .build();
  }

  @Override
  public SynchronizedStatement rewriteSynchronizedStatement(
      SynchronizedStatement synchronizedStatement) {
    // unary numeric promotion
    return SynchronizedStatement.Builder.from(synchronizedStatement)
        .setExpression(
            rewriteTypeConversionContextWithoutDeclaration(
                TypeDescriptors.get().javaLangObject.toNonNullable(),
                synchronizedStatement.getExpression()))
        .build();
  }

  @Override
  public ThrowStatement rewriteThrowStatement(ThrowStatement throwStatement) {
    Expression expression = throwStatement.getExpression();
    return ThrowStatement.Builder.from(throwStatement)
        .setExpression(
            rewriteTypeConversionContextWithoutDeclaration(
                TypeDescriptors.get().javaLangThrowable.toNonNullable(), expression))
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
    Expression initializer =
        rewriteTypeConversionContextWithoutDeclaration(
            variableDeclaration.getVariable().getTypeDescriptor(),
            variableDeclaration.getInitializer());

    if (initializer == variableDeclaration.getInitializer()) {
      return variableDeclaration;
    }

    return VariableDeclarationFragment.Builder.from(variableDeclaration)
        .setInitializer(initializer)
        .build();
  }

  private MethodLike getEnclosingMethodLike() {
    return (MethodLike) getParent(MethodLike.class::isInstance);
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
