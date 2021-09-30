/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.transpiler.backend.kotlin

import com.google.j2cl.common.InternalCompilerError
import com.google.j2cl.transpiler.ast.ArrayAccess
import com.google.j2cl.transpiler.ast.ArrayLength
import com.google.j2cl.transpiler.ast.ArrayLiteral
import com.google.j2cl.transpiler.ast.BinaryExpression
import com.google.j2cl.transpiler.ast.BinaryOperator
import com.google.j2cl.transpiler.ast.CastExpression
import com.google.j2cl.transpiler.ast.ConditionalExpression
import com.google.j2cl.transpiler.ast.Expression
import com.google.j2cl.transpiler.ast.ExpressionWithComment
import com.google.j2cl.transpiler.ast.FieldAccess
import com.google.j2cl.transpiler.ast.FunctionExpression
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.MemberReference
import com.google.j2cl.transpiler.ast.MethodCall
import com.google.j2cl.transpiler.ast.MultiExpression
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.PostfixExpression
import com.google.j2cl.transpiler.ast.PrefixExpression
import com.google.j2cl.transpiler.ast.PrefixOperator
import com.google.j2cl.transpiler.ast.SuperReference
import com.google.j2cl.transpiler.ast.ThisReference
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment
import com.google.j2cl.transpiler.ast.VariableReference

fun Renderer.renderExpression(expression: Expression) {
  when (expression) {
    is ArrayAccess -> renderArrayAccess(expression)
    is ArrayLength -> renderArrayLength(expression)
    is ArrayLiteral -> renderArrayLiteral(expression)
    is BinaryExpression -> renderBinaryExpression(expression)
    is CastExpression -> renderCastExpression(expression)
    is ExpressionWithComment -> renderExpressionWithComment(expression)
    is FieldAccess -> renderFieldAccess(expression)
    is FunctionExpression -> renderFunctionExpression(expression)
    is ConditionalExpression -> renderConditionalExpression(expression)
    is Literal -> render(expression.sourceString)
    is MethodCall -> renderMethodCall(expression)
    is MultiExpression -> renderMultiExpression(expression)
    is NewInstance -> renderNewInstance(expression)
    is PostfixExpression -> renderPostfixExpression(expression)
    is PrefixExpression -> renderPrefixExpression(expression)
    is SuperReference -> render("super")
    is ThisReference -> render("this")
    is VariableDeclarationExpression -> renderVariableDeclarationExpression(expression)
    is VariableReference -> render(expression.target.name)
    // TODO(micapolos): Handle all expression types
    else -> throw InternalCompilerError("Unhandled ${expression::class}")
  }
}

private fun Renderer.renderArrayAccess(arrayAccess: ArrayAccess) {
  renderLeftSubExpression(arrayAccess, arrayAccess.arrayExpression)
  renderInSquareBrackets { renderExpression(arrayAccess.indexExpression) }
}

private fun Renderer.renderArrayLength(arrayLength: ArrayLength) {
  renderLeftSubExpression(arrayLength, arrayLength.arrayExpression)
  render(".size")
}

private fun Renderer.renderArrayLiteral(arrayLiteral: ArrayLiteral) {
  render("arrayOf")
  renderInParentheses {
    renderCommaSeparated(arrayLiteral.valueExpressions) { renderExpression(it) }
  }
}

private fun Renderer.renderBinaryExpression(expression: BinaryExpression) {
  renderLeftSubExpression(expression, expression.leftOperand)
  render(" ")
  renderBinaryOperator(expression.operator)
  render(" ")
  renderRightSubExpression(expression, expression.rightOperand)
}

private fun Renderer.renderCastExpression(expression: CastExpression) {
  renderExpression(expression.expression)
  render(" as ${expression.castTypeDescriptor.sourceString}")
}

private fun Renderer.renderBinaryOperator(operator: BinaryOperator) {
  render(operator.symbol)
}

private fun Renderer.renderExpressionWithComment(expressionWithComment: ExpressionWithComment) {
  // Comments do not count as operations, but parenthesis will be emitted by the
  // outer context if needed given that getPrecedence is just a passthrough to the inner
  // expression.
  renderExpression(expressionWithComment.expression)
}

private fun Renderer.renderFieldAccess(fieldAccess: FieldAccess) {
  renderQualifiedName(fieldAccess, fieldAccess.target.name!!)
}

private fun Renderer.renderFunctionExpression(expression: FunctionExpression) {
  renderInCurlyBrackets {
    renderCommaSeparated(expression.parameters) { renderVariable(it) }
    render(" -> ")
    renderStatement(expression.body)
  }
}

private fun Renderer.renderConditionalExpression(conditionalExpression: ConditionalExpression) {
  // Conditional expressions are in its own precedence class. So when they are nested in the
  // in the condition position they need parenthesis, but not in the second or third position.
  render("if ")
  renderInParentheses {
    renderLeftSubExpression(conditionalExpression, conditionalExpression.conditionExpression)
  }
  render(" ")
  renderExpression(conditionalExpression.trueExpression)
  render(" else ")
  renderExpression(conditionalExpression.falseExpression)
}

private fun Renderer.renderMethodCall(expression: MethodCall) {
  require(!expression.isStaticDispatch) { "$expression not currently supported." }
  renderMethodCallHeader(expression)
  renderInParentheses { renderCommaSeparated(expression.arguments) { renderExpression(it) } }
}

private fun Renderer.renderQualifiedName(enclosingExpression: Expression, propertyName: String) {
  renderLeftSubExpression(enclosingExpression, (enclosingExpression as MemberReference).qualifier)
  render(".$propertyName")
}

private fun Renderer.renderMethodCallHeader(expression: MethodCall) {
  require(!expression.isStaticDispatch)
  val target = expression.target
  if (target.isConstructor) render("super") else renderQualifiedName(expression, target.name!!)
}

private fun Renderer.renderMultiExpression(multiExpression: MultiExpression) {
  renderInParentheses { renderCommaSeparated(multiExpression.expressions) { renderExpression(it) } }
}

private fun Renderer.renderNewInstance(expression: NewInstance) {
  require(expression.qualifier == null)
  val targetTypeDescriptor = expression.target.enclosingTypeDescriptor.toRawTypeDescriptor()!!
  render("${targetTypeDescriptor.sourceString}")
  renderInParentheses { renderCommaSeparated(expression.arguments) { renderExpression(it) } }
}

private fun Renderer.renderPostfixExpression(expression: PostfixExpression) {
  renderLeftSubExpression(expression, expression.operand)
  render(expression.operator.symbol)
}

private fun Renderer.renderPrefixExpression(expression: PrefixExpression) {
  expression.operator.let {
    render(it.symbol)
    // Emit a space after + and minus to avoid emitting + + as ++ and - -  and --.
    if (it == PrefixOperator.PLUS || it == PrefixOperator.MINUS) sourceBuilder.append(" ")
  }
  renderRightSubExpression(expression, expression.operand)
}

private fun Renderer.renderVariableDeclarationExpression(
  expression: VariableDeclarationExpression
) {
  render("var ")
  renderCommaSeparated(expression.fragments) { renderVariableDeclarationFragment(it) }
}

private fun Renderer.renderVariableDeclarationFragment(fragment: VariableDeclarationFragment) {
  renderVariable(fragment.variable)
  fragment.initializer?.let {
    render(" = ")
    renderExpression(it)
  }
}

fun Renderer.renderVariable(variable: Variable) {
  render("${variable.name}: ${variable.typeDescriptor.sourceString}")
}

private fun Renderer.renderLeftSubExpression(expression: Expression, operand: Expression) {
  renderExpressionInParens(operand, expression.requiresParensOnLeft(operand))
}

private fun Renderer.renderRightSubExpression(expression: Expression, operand: Expression) {
  renderExpressionInParens(operand, expression.requiresParensOnRight(operand))
}

private fun Renderer.renderExpressionInParens(expression: Expression, needsParentheses: Boolean) {
  if (needsParentheses) renderInParentheses { renderExpression(expression) }
  else renderExpression(expression)
}
