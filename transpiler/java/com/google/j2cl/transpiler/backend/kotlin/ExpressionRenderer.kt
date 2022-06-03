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
import com.google.j2cl.transpiler.ast.ArrayTypeDescriptor
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
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.StringLiteral
import com.google.j2cl.transpiler.ast.SuperReference
import com.google.j2cl.transpiler.ast.ThisReference
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
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
    PrimitiveTypes.BOOLEAN -> render("kotlin.booleanArrayOf")
    PrimitiveTypes.CHAR -> render("kotlin.charArrayOf")
    PrimitiveTypes.BYTE -> render("kotlin.byteArrayOf")
    PrimitiveTypes.SHORT -> render("kotlin.shortArrayOf")
    PrimitiveTypes.INT -> render("kotlin.intArrayOf")
    PrimitiveTypes.LONG -> render("kotlin.longArrayOf")
    PrimitiveTypes.FLOAT -> render("kotlin.floatArrayOf")
    PrimitiveTypes.DOUBLE -> render("kotlin.doubleArrayOf")
    else -> {
      render("kotlin.arrayOf")
      renderInAngleBrackets {
        renderTypeDescriptor(componentTypeDescriptor, TypeDescriptorUsage.REFERENCE)
      }
    }
  }
  renderInParentheses {
    renderCommaSeparated(arrayLiteral.valueExpressions) { renderExpression(it) }
  }
}

private fun Renderer.renderBinaryExpression(expression: BinaryExpression) {
  // Java and Kotlin does not allow initializing static final fields with type qualifier, so it
  // needs to be rendered without the qualifier.
  val leftOperand = expression.leftOperand
  if (leftOperand is FieldAccess &&
      expression.isSimpleAssignment &&
      leftOperand.target.isStatic &&
      leftOperand.target.isFinal
  ) {
    renderIdentifier(leftOperand.target.ktName)
  } else {
    renderLeftSubExpression(expression, expression.leftOperand)
  }
  render(" ")
  renderBinaryOperator(expression.operator)
  render(" ")
  renderRightSubExpression(expression, expression.rightOperand)
}

private fun Renderer.renderCastExpression(expression: CastExpression) {
  renderExpression(expression.expression)
  render(" as ")
  renderTypeDescriptor(expression.castTypeDescriptor, TypeDescriptorUsage.REFERENCE)
}

private fun Renderer.renderBinaryOperator(operator: BinaryOperator) {
  render(operator.ktSymbol)
}

private val BinaryOperator.ktSymbol
  get() =
    when (this) {
      BinaryOperator.TIMES -> "*"
      BinaryOperator.DIVIDE -> "/"
      BinaryOperator.REMAINDER -> "%"
      BinaryOperator.PLUS -> "+"
      BinaryOperator.MINUS -> "-"
      BinaryOperator.LESS -> "<"
      BinaryOperator.GREATER -> ">"
      BinaryOperator.LESS_EQUALS -> "<="
      BinaryOperator.GREATER_EQUALS -> ">="
      BinaryOperator.EQUALS -> "==="
      BinaryOperator.NOT_EQUALS -> "!=="
      BinaryOperator.CONDITIONAL_AND -> "&&"
      BinaryOperator.CONDITIONAL_OR -> "||"
      BinaryOperator.ASSIGN -> "="
      else -> throw InternalCompilerError("$this.ktSymbol")
    }

private fun Renderer.renderExpressionWithComment(expressionWithComment: ExpressionWithComment) {
  // Comments do not count as operations, but parenthesis will be emitted by the
  // outer context if needed given that getPrecedence is just a passthrough to the inner
  // expression.
  renderExpression(expressionWithComment.expression)
}

private fun Renderer.renderFieldAccess(fieldAccess: FieldAccess) {
  renderQualifier(fieldAccess)
  renderIdentifier(fieldAccess.target.ktName)
}

private fun Renderer.renderFunctionExpression(functionExpression: FunctionExpression) {
  val functionalInterface = functionExpression.typeDescriptor.functionalInterface!!.toNonNullable()
  renderTypeDescriptor(functionalInterface, TypeDescriptorUsage.SUPER_TYPE)
  render(" ")
  renderInCurlyBrackets {
    val parameters = functionExpression.parameters
    if (parameters.isNotEmpty()) {
      render(" ")
      renderCommaSeparated(parameters) { renderVariable(it) }
      render(" ->")
    }
    renderNewLine()
    renderWithReturnLabelIdentifier(functionalInterface.simpleSourceName) {
      renderStartingWithNewLines(functionExpression.body.statements) { renderStatement(it) }
    }
  }
}

private fun Renderer.renderInstanceOfExpression(instanceOfExpression: InstanceOfExpression) {
  renderExpression(instanceOfExpression.expression)
  render(" is ")
  val testTypeDescriptor = instanceOfExpression.testTypeDescriptor
  if (testTypeDescriptor is ArrayTypeDescriptor &&
      !testTypeDescriptor.componentTypeDescriptor!!.isPrimitive
  ) {
    render("kotlin.Array<*>")
  } else {
    renderTypeDescriptor(
      instanceOfExpression.testTypeDescriptor.toNonNullable(),
      TypeDescriptorUsage.REFERENCE
    )
  }
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
  renderTypeDescriptor(typeLiteral.referencedTypeDescriptor, TypeDescriptorUsage.QUALIFIED_NAME)
  render("::class.java")
}

private fun Renderer.renderNumberLiteral(numberLiteral: NumberLiteral) {
  when (numberLiteral.typeDescriptor.toUnboxedType()) {
    PrimitiveTypes.CHAR -> render("'${numberLiteral.value.toChar().escapedString}'")
    PrimitiveTypes.BYTE,
    PrimitiveTypes.SHORT,
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

private fun Renderer.renderMethodCall(expression: MethodCall) {
  val methodDescriptor = expression.target
  if (isJavaLangObject(methodDescriptor.enclosingTypeDescriptor) &&
      methodDescriptor.signature == "getClass()"
  ) {
    renderInParentheses { renderExpression(expression.qualifier) }
    render("::class.java")
    return
  }

  renderQualifier(expression)
  renderIdentifier(expression.target.ktName)
  if (!expression.target.isKtProperty) {
    renderInvocationArguments(expression)
  }
}

private fun Renderer.renderTypeArguments(typeArguments: List<TypeDescriptor>) {
  if (typeArguments.isNotEmpty() && !typeArguments.any(TypeDescriptor::isInferred)) {
    renderInAngleBrackets {
      renderCommaSeparated(typeArguments) { renderTypeDescriptor(it, TypeDescriptorUsage.ARGUMENT) }
    }
  }
}

internal fun Renderer.renderInvocationArguments(invocation: Invocation) {
  renderInParentheses {
    // Take last argument if it's an array literal passed as a vararg parameter.
    val varargArrayLiteral =
      (invocation.arguments.lastOrNull() as? ArrayLiteral).takeIf {
        invocation.target.parameterDescriptors.lastOrNull()?.isVarargs == true
      }

    if (varargArrayLiteral != null) {
      val expandedArguments = invocation.arguments.dropLast(1) + varargArrayLiteral.valueExpressions
      renderCommaSeparated(expandedArguments) { renderExpression(it) }
    } else {
      val parameters = invocation.target.parameterDescriptors.zip(invocation.arguments)
      renderCommaSeparated(parameters) { (parameterDescriptor, argument) ->
        // TODO(b/216523245): Handle spread operator using a pass in the AST.
        if (parameterDescriptor.isVarargs) {
          render("*")
          renderInParentheses { renderExpression(argument) }
          // Spread operator requires non-null array.
          if (argument.typeDescriptor.isNullable) render("!!")
        } else {
          renderExpression(argument)
        }
      }
    }
  }
}

private fun Renderer.renderMultiExpression(multiExpression: MultiExpression) {
  render("kotlin.run ")
  renderInCurlyBrackets {
    renderStartingWithNewLines(multiExpression.expressions) { expression ->
      renderExpression(expression)
    }
  }
}

private fun Renderer.renderNewArray(newArray: NewArray) {
  require(newArray.arrayLiteral == null)
  val dimensions = newArray.dimensionExpressions.iterator()
  val firstDimension = dimensions.next()
  renderNewArray(newArray.typeDescriptor, firstDimension, dimensions)
}

private fun Renderer.renderNewArray(
  arrayTypeDescriptor: ArrayTypeDescriptor,
  firstDimension: Expression,
  remainingDimensions: Iterator<Expression>
) {
  val componentTypeDescriptor = arrayTypeDescriptor.componentTypeDescriptor!!
  if (!remainingDimensions.hasNext()) {
    if (componentTypeDescriptor is PrimitiveTypeDescriptor) {
      renderPrimitiveArrayOf(componentTypeDescriptor, firstDimension)
    } else {
      renderArrayOfNulls(componentTypeDescriptor, firstDimension)
    }
  } else {
    val nextDimension = remainingDimensions.next()
    if (nextDimension is NullLiteral) {
      renderArrayOfNulls(componentTypeDescriptor, firstDimension)
    } else {
      render("kotlin.Array")
      renderInAngleBrackets {
        renderTypeDescriptor(componentTypeDescriptor, TypeDescriptorUsage.ARGUMENT)
      }
      renderInParentheses { renderExpression(firstDimension) }
      render(" ")
      renderInCurlyBrackets {
        renderNewLine()
        renderNewArray(
          componentTypeDescriptor as ArrayTypeDescriptor,
          nextDimension,
          remainingDimensions
        )
      }
    }
  }
}

private fun Renderer.renderPrimitiveArrayOf(
  componentTypeDescriptor: PrimitiveTypeDescriptor,
  dimension: Expression
) {
  when (componentTypeDescriptor) {
    PrimitiveTypes.BOOLEAN -> render("kotlin.BooleanArray")
    PrimitiveTypes.CHAR -> render("kotlin.CharArray")
    PrimitiveTypes.BYTE -> render("kotlin.ByteArray")
    PrimitiveTypes.SHORT -> render("kotlin.ShortArray")
    PrimitiveTypes.INT -> render("kotlin.IntArray")
    PrimitiveTypes.LONG -> render("kotlin.LongArray")
    PrimitiveTypes.FLOAT -> render("kotlin.FloatArray")
    PrimitiveTypes.DOUBLE -> render("kotlin.DoubleArray")
    else -> throw InternalCompilerError("renderPrimitiveArrayOf($componentTypeDescriptor)")
  }
  renderInParentheses { renderExpression(dimension) }
}

private fun Renderer.renderArrayOfNulls(
  componentTypeDescriptor: TypeDescriptor,
  dimension: Expression
) {
  render("kotlin.arrayOfNulls")
  renderInAngleBrackets {
    renderTypeDescriptor(componentTypeDescriptor.toNonNullable(), TypeDescriptorUsage.ARGUMENT)
  }
  renderInParentheses { renderExpression(dimension) }
}

private fun Renderer.renderNewInstance(expression: NewInstance) {
  renderQualifier(expression)

  var typeDescriptor = expression.typeDescriptor.nonAnonymousTypeDescriptor.toNonNullable()
  if (expression.anonymousInnerClass != null) {
    render("object : ")
  }

  // Render fully-qualified type name if there's no qualifier, otherwise render qualifier and
  // simple type name.
  val typeDeclaration = expression.typeDescriptor.typeDeclaration
  renderTypeDescriptor(
    typeDescriptor,
    TypeDescriptorUsage.SUPER_TYPE,
    asSimple = typeDeclaration.isCapturingEnclosingInstance
  )

  // Render invocation for classes only - interfaces don't need it.
  if (typeDescriptor.isClass) {
    renderInvocationArguments(expression)
  }

  expression.anonymousInnerClass?.let { renderTypeBody(it) }
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
  renderIdentifier(typeDescriptor.typeDeclaration.simpleSourceName)
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

  val typeDescriptor = variable.typeDescriptor
  if (!typeDescriptor.isInferred) {
    render(": ")
    renderTypeDescriptor(typeDescriptor, TypeDescriptorUsage.REFERENCE)
  }
}

private fun Renderer.renderQualifier(memberReference: MemberReference) {
  val qualifier = memberReference.qualifier
  if (qualifier == null) {
    if (memberReference.target.isStatic) {
      // TODO(b/206482966): Move the checks in the backend to a verifier pass.
      renderTypeDescriptor(
        memberReference.target.enclosingTypeDescriptor,
        TypeDescriptorUsage.QUALIFIED_NAME
      )
      render(".")
    }
  } else {
    if (memberReference is NewInstance && memberReference.typeDescriptor.typeDeclaration.isLocal) {
      // Don't render qualifier for local classes.
      // TODO(b/219950593): Implement a pass which will remove unnecessary qualifiers, and then
      // remove this `if` branch.
    } else if (memberReference.target.isInstanceMember || !qualifier.isNonQualifiedThisReference) {
      renderLeftSubExpression(memberReference, qualifier)
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
