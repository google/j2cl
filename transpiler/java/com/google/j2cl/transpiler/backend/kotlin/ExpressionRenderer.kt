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
import com.google.j2cl.transpiler.ast.BooleanLiteral
import com.google.j2cl.transpiler.ast.CastExpression
import com.google.j2cl.transpiler.ast.ConditionalExpression
import com.google.j2cl.transpiler.ast.Expression
import com.google.j2cl.transpiler.ast.ExpressionWithComment
import com.google.j2cl.transpiler.ast.FieldAccess
import com.google.j2cl.transpiler.ast.FunctionExpression
import com.google.j2cl.transpiler.ast.InstanceOfExpression
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.MemberReference
import com.google.j2cl.transpiler.ast.MethodCall
import com.google.j2cl.transpiler.ast.MultiExpression
import com.google.j2cl.transpiler.ast.NewArray
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.NullLiteral
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PostfixExpression
import com.google.j2cl.transpiler.ast.PrefixExpression
import com.google.j2cl.transpiler.ast.PrefixOperator
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.StringLiteral
import com.google.j2cl.transpiler.ast.SuperReference
import com.google.j2cl.transpiler.ast.ThisReference
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeLiteral
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
    is ConditionalExpression -> renderConditionalExpression(expression)
    is ExpressionWithComment -> renderExpressionWithComment(expression)
    is FieldAccess -> renderFieldAccess(expression)
    is FunctionExpression -> renderFunctionExpression(expression)
    is InstanceOfExpression -> renderInstanceOfExpression(expression)
    is Literal -> renderLiteral(expression)
    is JavaScriptConstructorReference -> renderJavaScriptConstructorReference(expression)
    is MethodCall -> renderMethodCall(expression)
    is MultiExpression -> renderMultiExpression(expression)
    is NewArray -> renderNewArray(expression)
    is NewInstance -> renderNewInstance(expression)
    is PostfixExpression -> renderPostfixExpression(expression)
    is PrefixExpression -> renderPrefixExpression(expression)
    is SuperReference -> render("super")
    is ThisReference -> render("this")
    is VariableDeclarationExpression -> renderVariableDeclarationExpression(expression)
    is VariableReference -> renderVariableReference(expression)
    else -> renderTodo(expression::class.java.simpleName)
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
  when (val componentTypeDescriptor = arrayLiteral.typeDescriptor.componentTypeDescriptor!!) {
    PrimitiveTypes.BOOLEAN -> render("booleanArrayOf")
    PrimitiveTypes.CHAR -> render("charArrayOf")
    PrimitiveTypes.BYTE -> render("byteArrayOf")
    PrimitiveTypes.SHORT -> render("shortArrayOf")
    PrimitiveTypes.INT -> render("intArrayOf")
    PrimitiveTypes.LONG -> render("longArrayOf")
    PrimitiveTypes.FLOAT -> render("floatArrayOf")
    PrimitiveTypes.DOUBLE -> render("doubleArrayOf")
    else -> {
      render("arrayOf")
      renderInAngleBrackets { render(componentTypeDescriptor) }
    }
  }
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
  render(" as ")
  render(expression.castTypeDescriptor)
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

private fun Renderer.renderFunctionExpression(functionExpression: FunctionExpression) {
  renderInCurlyBrackets {
    functionExpression.parameters.takeIf { it.isNotEmpty() }?.let { parameters ->
      render(" ")
      renderCommaSeparated(parameters) { renderVariable(it) }
      render(" ->")
    }
    renderStartingWithNewLines(functionExpression.body.statements) { renderStatement(it) }
  }
}

private fun Renderer.renderInstanceOfExpression(instanceOfExpression: InstanceOfExpression) {
  renderExpression(instanceOfExpression.expression)
  render(" is ")
  render(instanceOfExpression.testTypeDescriptor.toNonNullable())
}

private fun Renderer.renderLiteral(literal: Literal) {
  when (literal) {
    is NullLiteral -> render("null")
    is BooleanLiteral -> renderBooleanLiteral(literal)
    is StringLiteral -> renderStringLiteral(literal)
    is TypeLiteral -> renderTypeLiteral(literal)
    is NumberLiteral -> renderNumberLiteral(literal)
    else -> throw InternalCompilerError("renderLiteral($literal)")
  }
}

private fun Renderer.renderBooleanLiteral(booleanLiteral: BooleanLiteral) {
  render("${booleanLiteral.value}")
}

private fun Renderer.renderStringLiteral(stringLiteral: StringLiteral) {
  render("\"${stringLiteral.value.escapedString}\"")
}

private fun Renderer.renderTypeLiteral(typeLiteral: TypeLiteral) {
  render(typeLiteral.referencedTypeDescriptor.toNonNullable())
  render("::class.java")
}

private fun Renderer.renderNumberLiteral(numberLiteral: NumberLiteral) {
  when (numberLiteral.typeDescriptor.toUnboxedType()) {
    PrimitiveTypes.CHAR -> render("'${numberLiteral.value.toChar().escapedString}'")
    PrimitiveTypes.INT -> render("${numberLiteral.value.toInt()}")
    PrimitiveTypes.LONG -> render("${numberLiteral.value.toLong()}L")
    PrimitiveTypes.FLOAT -> render("${numberLiteral.value.toFloat()}f")
    PrimitiveTypes.DOUBLE -> render("${numberLiteral.value.toDouble()}")
    else -> throw InternalCompilerError("renderNumberLiteral($numberLiteral)")
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

private fun Renderer.renderJavaScriptConstructorReference(
  javaScriptConstructorReference: JavaScriptConstructorReference
) {
  render(javaScriptConstructorReference.referencedTypeDeclaration)
}

private fun Renderer.renderMethodCall(expression: MethodCall) {
  renderMethodCallHeader(expression)
  renderInParentheses { renderCommaSeparated(expression.arguments) { renderExpression(it) } }
}

private fun Renderer.renderQualifiedName(expression: Expression, name: String) {
  renderLeftSubExpression(expression, (expression as MemberReference).qualifier)
  render(".${name.identifierSourceString}")
}

private fun Renderer.renderMethodCallHeader(expression: MethodCall) {
  renderQualifiedName(expression, expression.target.name!!)
}

private fun Renderer.renderMultiExpression(multiExpression: MultiExpression) {
  render("run ")
  renderInCurlyBrackets {
    renderStartingWithNewLines(multiExpression.expressions) { expression ->
      renderExpression(expression)
    }
  }
}

private fun Renderer.renderNewArray(newArray: NewArray) {
  val literalOrNull = newArray.arrayLiteral
  if (literalOrNull != null) {
    renderArrayLiteral(literalOrNull)
  } else {
    renderNewArrayOfSize(
      newArray.typeDescriptor.componentTypeDescriptor!!,
      newArray.dimensionExpressions.first()
    )
  }
}

private fun Renderer.renderNewArrayOfSize(
  componentTypeDescriptor: TypeDescriptor,
  sizeExpression: Expression
) {
  when (componentTypeDescriptor) {
    PrimitiveTypes.BOOLEAN -> render("BooleanArray")
    PrimitiveTypes.CHAR -> render("CharArray")
    PrimitiveTypes.BYTE -> render("ByteArray")
    PrimitiveTypes.SHORT -> render("ShortArray")
    PrimitiveTypes.INT -> render("IntArray")
    PrimitiveTypes.LONG -> render("LongArray")
    PrimitiveTypes.FLOAT -> render("FloatArray")
    PrimitiveTypes.DOUBLE -> render("DoubleArray")
    else -> {
      render("arrayOfNulls")
      renderInAngleBrackets { render(componentTypeDescriptor) }
    }
  }
  renderInParentheses { renderExpression(sizeExpression) }
}

private fun Renderer.renderNewInstance(expression: NewInstance) {
  if (expression.qualifier != null) {
    renderTodo("expression.qualify needs rendering: ${expression.qualifier})")
    return
  }
  render(expression.typeDescriptor)
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
  renderSeparatedWith(expression.fragments, "\n") {
    render(if (it.variable.isFinal) "val " else "var ")
    renderVariableDeclarationFragment(it)
  }
}

private fun Renderer.renderVariableReference(variableReference: VariableReference) {
  render(variableReference.target.name.identifierSourceString)
}

private fun Renderer.renderVariableDeclarationFragment(fragment: VariableDeclarationFragment) {
  renderVariable(fragment.variable)
  fragment.initializer?.let {
    render(" = ")
    renderExpression(it)
  }
}

fun Renderer.renderVariable(variable: Variable) {
  render(variable.name.identifierSourceString)
  render(": ")
  render(variable.typeDescriptor)
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
