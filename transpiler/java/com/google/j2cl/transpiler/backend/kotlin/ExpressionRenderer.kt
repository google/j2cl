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
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor
import com.google.j2cl.transpiler.ast.Expression
import com.google.j2cl.transpiler.ast.ExpressionWithComment
import com.google.j2cl.transpiler.ast.FieldAccess
import com.google.j2cl.transpiler.ast.FunctionExpression
import com.google.j2cl.transpiler.ast.InstanceOfExpression
import com.google.j2cl.transpiler.ast.Invocation
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
    is MethodCall -> renderMethodCall(expression)
    is MultiExpression -> renderMultiExpression(expression)
    is NewArray -> renderNewArray(expression)
    is NewInstance -> renderNewInstance(expression)
    is PostfixExpression -> renderPostfixExpression(expression)
    is PrefixExpression -> renderPrefixExpression(expression)
    is SuperReference -> renderSuperReference(expression)
    is ThisReference -> renderThisReference(expression)
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
      renderInAngleBrackets { renderTypeDescriptor(componentTypeDescriptor) }
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
  renderTypeDescriptor(expression.castTypeDescriptor)
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
  renderQualifier(fieldAccess)
  renderIdentifier(fieldAccess.target.name!!)
}

private fun Renderer.renderFunctionExpression(functionExpression: FunctionExpression) {
  renderTypeDescriptor(functionExpression.typeDescriptor.functionalInterface!!.toNonNullable())
  render(" ")
  renderInParentheses {
    render("fun")
    renderInParentheses {
      renderCommaSeparated(functionExpression.parameters) { renderVariable(it) }
    }
    renderMethodReturnType(functionExpression.descriptor)
    render(" ")
    renderInCurlyBrackets {
      renderStartingWithNewLines(functionExpression.body.statements) { renderStatement(it) }
    }
  }
}

private fun Renderer.renderInstanceOfExpression(instanceOfExpression: InstanceOfExpression) {
  renderExpression(instanceOfExpression.expression)
  render(" is ")
  renderTypeDescriptor(instanceOfExpression.testTypeDescriptor.toNonNullable())
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
  renderTypeDescriptor(typeLiteral.referencedTypeDescriptor, asName = true)
  render("::class.java")
}

private fun Renderer.renderNumberLiteral(numberLiteral: NumberLiteral) {
  when (numberLiteral.typeDescriptor.toUnboxedType()) {
    PrimitiveTypes.CHAR -> render("'${numberLiteral.value.toChar().escapedString}'")
    PrimitiveTypes.BYTE, PrimitiveTypes.SHORT, PrimitiveTypes.INT ->
      render("${numberLiteral.value.toInt()}")
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

private fun Renderer.renderMethodCall(expression: MethodCall) {
  renderQualifier(expression)
  renderIdentifier(expression.target.name!!)
  renderInvocationArguments(expression)
}

internal fun Renderer.renderInvocationArguments(invocation: Invocation) {
  renderInParentheses {
    val parameters = invocation.target.parameterDescriptors.zip(invocation.arguments)
    renderCommaSeparated(parameters) { (parameterDescriptor, argument) ->
      // TODO(b/216523245): Handle spread operator using a pass in the AST.
      if (parameterDescriptor.isVarargs) {
        render("*")
        renderInParentheses { renderExpression(argument) }
      } else {
        renderExpression(argument)
      }
    }
  }
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
      renderInAngleBrackets { renderTypeDescriptor(componentTypeDescriptor) }
    }
  }
  renderInParentheses { renderExpression(sizeExpression) }
}

private fun Renderer.renderNewInstance(expression: NewInstance) {
  renderQualifier(expression)

  var typeDescriptor = expression.typeDescriptor.nonAnonymousTypeDescriptor.toNonNullable()
  if (mapsToKotlin(typeDescriptor)) {
    // If Java type maps to a Kotlin type (ie: java.lang.Double -> kotlin.Double), create an
    // instance of the Java type and cast to Kotlin type, like:
    // - (java.lang.Double("123") as kotlin.Double).
    // TODO(b/216924456): Remove when proper Java-Kotlin member translation is implemented
    renderInParentheses {
      if (expression.anonymousInnerClass != null) {
        render("object : ")
      }
      renderTypeDescriptor(typeDescriptor, asJava = true, asName = true, projectBounds = true)
      if (typeDescriptor.isClass) {
        renderInvocationArguments(expression)
      }
      expression.anonymousInnerClass?.let { renderTypeBody(it) }
      render(" as ")
      renderTypeDescriptor(typeDescriptor, asName = true)
    }
  } else {
    if (expression.anonymousInnerClass != null) {
      render("object : ")
    }

    // Render fully-qualified type name if there's no qualifier, otherwise render qualifier and
    // simple type name.
    renderTypeDescriptor(
      typeDescriptor,
      asSimple = expression.qualifier != null,
      projectBounds = true
    )

    // Render invocation for classes only - interfaces don't need it.
    if (typeDescriptor.isClass) {
      renderInvocationArguments(expression)
    }

    expression.anonymousInnerClass?.let { renderTypeBody(it) }
  }
}

private val DeclaredTypeDescriptor.nonAnonymousTypeDescriptor: DeclaredTypeDescriptor
  get() =
    if (typeDeclaration.isAnonymous) interfaceTypeDescriptors.firstOrNull() ?: superTypeDescriptor!!
    else this

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

private fun Renderer.renderSuperReference(superReference: SuperReference) {
  render("super")
  // TODO(b/214453506): Render optional qualifier
}

private fun Renderer.renderThisReference(thisReference: ThisReference) {
  render("this")
  if (thisReference.isQualified) {
    renderLabelReference(thisReference.typeDescriptor)
  }
}

private fun Renderer.renderLabelReference(typeDescriptor: DeclaredTypeDescriptor) {
  render("@")
  renderIdentifier(typeDescriptor.typeDeclaration.classComponents.last())
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
  renderName(variableReference.target)
}

private fun Renderer.renderVariableDeclarationFragment(fragment: VariableDeclarationFragment) {
  renderVariable(fragment.variable)
  fragment.initializer?.let {
    render(" = ")
    renderExpression(it)
  }
}

fun Renderer.renderVariable(variable: Variable) {
  renderName(variable)

  // Don't render anonymous types, since they are not denotable. They can be created from "var".
  val typeDescriptor = variable.typeDescriptor
  if (typeDescriptor is DeclaredTypeDescriptor && typeDescriptor.typeDeclaration.isAnonymous) {
    return
  }

  render(": ")
  renderTypeDescriptor(typeDescriptor)
}

private fun Renderer.renderQualifier(memberReference: MemberReference) {
  val qualifier = memberReference.qualifier
  if (qualifier == null) {
    if (memberReference.target.isStatic) {
      // TODO(b/206482966): Move the checks in the backend to a verifier pass.
      renderTypeDescriptor(
        memberReference.target.enclosingTypeDescriptor,
        asJava = true,
        asName = true
      )
      render(".")
    }
  } else {
    if (memberReference is NewInstance && memberReference.typeDescriptor.typeDeclaration.isLocal) {
      // Don't render qualifier for local classes.
      // TODO(b/219950593): Implement a pass which will remove unnecessary qualifiers, and then
      // remove this `if` branch.
    } else if (memberReference.target.isInstanceMember || !qualifier.isNonQualifiedThisReference) {
      // TODO(b/216924456): Remove when Java-Kotlin member translation is implemented
      if (mapsToKotlin(memberReference.target.enclosingTypeDescriptor)) {
        renderInParentheses {
          renderLeftSubExpression(memberReference, qualifier)
          render(" as ")
          renderTypeDescriptor(
            memberReference.target.enclosingTypeDescriptor.toNonNullable(),
            asJava = true
          )
        }
      } else {
        renderLeftSubExpression(memberReference, qualifier)
      }
      render(".")
    }
  }
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

private val Expression.isNonQualifiedThisReference
  get() = this is ThisReference && !isQualified
