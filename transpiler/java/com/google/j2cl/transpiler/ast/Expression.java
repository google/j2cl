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
package com.google.j2cl.transpiler.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.common.visitor.Processor;
import com.google.j2cl.common.visitor.Visitable;
import java.util.List;

/** Base class for expressions. */
@Visitable
public abstract class Expression extends Node implements Cloneable<Expression> {

  /** Returns the type descriptor of the value that is returned by this expression. */
  public abstract TypeDescriptor getTypeDescriptor();

  /** Returns the declared type for the expression if it is different from the inferred type. */
  public TypeDescriptor getDeclaredTypeDescriptor() {
    return getTypeDescriptor();
  }

  /**
   * Returns true if the expression can be evaluated multiple times and always results in the same
   * value.
   *
   * <p>Note: that the expression might have side effects (e.g. cause some class initializers to
   * run). An expression is idempotent if when evaluated in the same state multiple times yields the
   * same resulting state and value.
   */
  public boolean isIdempotent() {
    return false;
  }

  /**
   * Returns true if the expression does not have side effects (including triggering class
   * initializers) and its evaluation results on the same value if it is moved forward in the same
   * or an enclosed scope.
   */
  public boolean isEffectivelyInvariant() {
    return false;
  }

  /** Returns true if the expression has side effects. */
  public boolean hasSideEffects() {
    return true;
  }

  /** Returns true if the expression is an assignment (excluding compound assignments). */
  public boolean isSimpleAssignment() {
    return false;
  }

  /**
   * Returns true if the expression is an assignment (including compound assignments, pre/post
   * increments and decrements).
   */
  public boolean isSimpleOrCompoundAssignment() {
    return false;
  }

  /** Returns true if the expression value can be computed at compile time. */
  public boolean isCompileTimeConstant() {
    return false;
  }

  /**
   * Returns true if the expression can be used in the left hand side of an assignment. {@see JLS
   * 15.26}
   */
  public boolean isLValue() {
    return false;
  }

  /** Returns whether value of this expression can be null. */
  public boolean canBeNull() {
    return getTypeDescriptor().canBeNull();
  }

  /** Creates an ExpressionStatement with this expression as its code */
  public ExpressionStatement makeStatement(SourcePosition sourcePosition) {
    return new ExpressionStatement(sourcePosition, this);
  }

  /** Returns the expression enclosed as an expression with a comment. */
  public ExpressionWithComment withComment(String comment) {
    return new ExpressionWithComment(this, comment);
  }

  /** Prefix expression with a spread unary operator. */
  public Expression prefixSpread() {
    return prefix(PrefixOperator.SPREAD);
  }

  /** Prefix expression with a plus unary operator. */
  public Expression prefixPlus() {
    return prefix(PrefixOperator.PLUS);
  }

  /** Returns the logically negated expression. */
  public Expression prefixNot() {
    return prefix(PrefixOperator.NOT);
  }

  /** Returns expression prefixed with unary operator {@code prefixOperator}. */
  public Expression prefix(PrefixOperator prefixOperator) {
    return PrefixExpression.newBuilder().setOperator(prefixOperator).setOperand(this).build();
  }

  /** Returns expression with not-null assertion operator. */
  public Expression postfixNotNullAssertion() {
    return postfix(PostfixOperator.NOT_NULL_ASSERTION);
  }

  /** Returns expression postfixed with unary operator {@code postfixOperator}. */
  public Expression postfix(PostfixOperator postfixOperator) {
    return PostfixExpression.newBuilder().setOperator(postfixOperator).setOperand(this).build();
  }

  /** Return the logical or of this expression and {@code rhs}. */
  public Expression infixOr(Expression rhs) {
    return infix(BinaryOperator.CONDITIONAL_OR, this, rhs);
  }

  /** Return the logical or of {@code expressions}. */
  public static Expression infixOrAll(List<? extends Expression> expressions) {
    checkArgument(!expressions.isEmpty());
    Expression result = null;
    for (Expression e : expressions) {
      result = result == null ? e : result.infixOr(e);
    }
    return result;
  }

  /** Return the logical and of this expression and {@code rhs}. */
  public BinaryExpression infixAnd(Expression rhs) {
    return infix(BinaryOperator.CONDITIONAL_AND, this, rhs);
  }

  /** Return an expression representing {@code this | rhs}. */
  public BinaryExpression infixBitwiseOr(Expression rhs) {
    return infix(BinaryOperator.BIT_OR, this, rhs);
  }

  /** Return an expression representing {@code this ^ rhs}. */
  public BinaryExpression infixBitwiseXor(Expression rhs) {
    return infix(BinaryOperator.BIT_XOR, this, rhs);
  }

  /** Return an expression representing {@code this == rhs}. */
  public BinaryExpression infixEquals(Expression rhs) {
    return infix(BinaryOperator.EQUALS, this, rhs);
  }

  /** Return an expression representing {@code this < rhs}. */
  public BinaryExpression infixLessThan(Expression rhs) {
    return infix(BinaryOperator.LESS, this, rhs);
  }

  /** Return an expression representing {@code this != rhs}. */
  public BinaryExpression infixNotEquals(Expression rhs) {
    return infix(BinaryOperator.NOT_EQUALS, this, rhs);
  }

  /** Return an expression representing {@code this == null}. */
  public BinaryExpression infixEqualsNull() {
    checkState(!getTypeDescriptor().isPrimitive());
    return infix(BinaryOperator.EQUALS, this, getTypeDescriptor().getNullValue());
  }

  /** Return an expression representing {@code this != null}. */
  public BinaryExpression infixNotEqualsNull() {
    checkState(!getTypeDescriptor().isPrimitive());
    return infix(BinaryOperator.NOT_EQUALS, this, getTypeDescriptor().getNullValue());
  }

  /** Return an expression representing {@code this - rhs}. */
  public BinaryExpression infixMinus(Expression rhs) {
    return infix(BinaryOperator.MINUS, this, rhs);
  }

  private static BinaryExpression infix(BinaryOperator operator, Expression lhs, Expression rhs) {
    return BinaryExpression.newBuilder()
        .setOperator(operator)
        .setLeftOperand(lhs)
        .setRightOperand(rhs)
        .build();
  }

  /** Returns a member reference to the prototype field using this expression as its qualifier. */
  public Expression getPrototypeFieldAccess() {
    FieldDescriptor prototypeFieldDescriptor =
        FieldDescriptor.newBuilder()
            .setOriginalJsInfo(JsInfo.RAW_FIELD)
            .setEnclosingTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .setTypeDescriptor(TypeDescriptors.get().javaLangObject)
            .setName("prototype")
            .build();
    return FieldAccess.Builder.from(prototypeFieldDescriptor).setQualifier(this).build();
  }

  /**
   * The JavaScript precedence of this expression.
   *
   * <p>Used to decide whether parenthesis are needed when expressions are nested. e.g.
   *
   * <pre>
   *         *
   *        / \
   *       +   -
   *      / \   \
   *     1   2   4
   * </pre>
   *
   * <p>Should be rendered as {@code (1 + 2) * -4}. Since the lhs is of lower precedence, therefore
   * needs parenthesis; while the rhs is of higher precedence and does not need parenthesis.
   *
   * <p>The operator precedence is implied by the JavaScript spec. See {@link
   * http://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Operator_Precedence}
   */
  public abstract Precedence getPrecedence();

  /** Returns whether the expression needs parenthesis if it is emitted as the left operand. */
  public final boolean requiresParensOnLeft(Expression expression) {
    if (getPrecedence().getValue() > expression.getPrecedence().getValue()) {
      return true;
    }
    return getPrecedence().getAssociativity() != Associativity.LEFT
        && getPrecedence() == expression.getPrecedence();
  }

  /** Returns whether the expression needs parenthesis if it is emitted as the right operand. */
  public final boolean requiresParensOnRight(Expression expression) {
    if (getPrecedence().getValue() > expression.getPrecedence().getValue()) {
      return true;
    }
    return getPrecedence().getAssociativity() != Associativity.RIGHT
        && getPrecedence() == expression.getPrecedence();
  }

  /**
   * The associativity of an expression.
   *
   * <p>Used to decide whether parenthesis are needed when expressions of the same precedence are
   * nested. e.g.
   *
   * <pre>
   *         +
   *        / \
   *       -   -
   *      / \ / \
   *     1  2 3  4
   * </pre>
   *
   * <p>Should be rendered as {@code (1 - 2) + 3 - 4}. Because + and - are of the same precedence
   * and left associative, the left operand needs parenthesis but not the right. Note that all
   * expression that have the same precedence have the same associativity.
   */
  public enum Associativity {
    LEFT,
    RIGHT,
    NONE
  }

  /**
   * Precedence and associativity of expressions.
   *
   * <p>Details of Java precednce can be found in <a
   * href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.7">JLS 15.7 -
   * 15.26</a>
   */
  public enum Precedence {
    HIGHEST(21, Associativity.NONE),
    NOT_NULL_ASSERTION(20, Expression.Associativity.LEFT),
    MEMBER_ACCESS(20, Expression.Associativity.LEFT),
    FUNCTION(19, Expression.Associativity.RIGHT),
    POSTFIX(18, Expression.Associativity.NONE),
    PREFIX(17, Expression.Associativity.RIGHT),
    CAST(16, Expression.Associativity.RIGHT),
    MULTIPLICATIVE(15, Expression.Associativity.LEFT),
    ADDITIVE(14, Expression.Associativity.LEFT),
    SHIFT_OPERATOR(13, Expression.Associativity.LEFT),
    RELATIONAL(12, Expression.Associativity.LEFT),
    EQUALITY(11, Expression.Associativity.LEFT),
    BITWISE_AND(10, Expression.Associativity.LEFT),
    BITWISE_XOR(9, Expression.Associativity.LEFT),
    BITWISE_OR(8, Expression.Associativity.LEFT),
    LOGICAL_AND(7, Expression.Associativity.LEFT),
    LOGICAL_OR(6, Expression.Associativity.LEFT),
    CONDITIONAL(4, Expression.Associativity.RIGHT),
    ASSIGNMENT(3, Expression.Associativity.RIGHT);

    Precedence(int value, Expression.Associativity associativity) {
      this.value = value;
      this.associativity = associativity;
    }

    private final int value;
    private final Expression.Associativity associativity;

    public int getValue() {
      return value;
    }

    public Expression.Associativity getAssociativity() {
      return associativity;
    }
  }

  @Override
  public abstract Expression clone();

  @Override
  Node acceptInternal(Processor processor) {
    return Visitor_Expression.visit(processor, this);
  }
}
