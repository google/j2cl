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
import com.google.j2cl.transpiler.ast.Expression.Associativity
import com.google.j2cl.transpiler.ast.Expression.Precedence
import com.google.j2cl.transpiler.ast.ExpressionWithComment
import com.google.j2cl.transpiler.ast.FieldAccess
import com.google.j2cl.transpiler.ast.FunctionExpression
import com.google.j2cl.transpiler.ast.InstanceOfExpression
import com.google.j2cl.transpiler.ast.IntersectionTypeDescriptor
import com.google.j2cl.transpiler.ast.Invocation
import com.google.j2cl.transpiler.ast.KtInfo
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
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeLiteral
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment
import com.google.j2cl.transpiler.ast.VariableReference
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.afterSpace
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.inNewLine

fun Renderer.expressionSource(expression: Expression): Source = renderedSource {
  renderExpression(expression)
}

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
    else -> render(todoSource(expression::class.java.simpleName))
  }
}

private fun Renderer.renderArrayAccess(arrayAccess: ArrayAccess) {
  renderGetOperator(arrayAccess.arrayExpression, arrayAccess.indexExpression)
}

private fun Renderer.renderGetOperator(qualifier: Expression, argument: Expression) {
  renderExpressionInParens(
    qualifier,
    Precedence.MEMBER_ACCESS.requiresParensOnLeft(qualifier.precedence)
  )
  renderInSquareBrackets { renderExpression(argument) }
}

private fun Renderer.renderArrayLength(arrayLength: ArrayLength) {
  renderLeftSubExpression(arrayLength.precedence, arrayLength.arrayExpression)
  render(".size")
}

private fun Renderer.renderArrayLiteral(arrayLiteral: ArrayLiteral) {
  val typeArgument = arrayLiteral.typeDescriptor.typeArgument
  when (typeArgument.typeDescriptor) {
    PrimitiveTypes.BOOLEAN -> render(topLevelQualifiedNameSource("kotlin.booleanArrayOf"))
    PrimitiveTypes.CHAR -> render(topLevelQualifiedNameSource("kotlin.charArrayOf"))
    PrimitiveTypes.BYTE -> render(topLevelQualifiedNameSource("kotlin.byteArrayOf"))
    PrimitiveTypes.SHORT -> render(topLevelQualifiedNameSource("kotlin.shortArrayOf"))
    PrimitiveTypes.INT -> render(topLevelQualifiedNameSource("kotlin.intArrayOf"))
    PrimitiveTypes.LONG -> render(topLevelQualifiedNameSource("kotlin.longArrayOf"))
    PrimitiveTypes.FLOAT -> render(topLevelQualifiedNameSource("kotlin.floatArrayOf"))
    PrimitiveTypes.DOUBLE -> render(topLevelQualifiedNameSource("kotlin.doubleArrayOf"))
    else -> {
      render(topLevelQualifiedNameSource("kotlin.arrayOf"))
      render(typeArgumentsSource(listOf(typeArgument)))
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
  val rightOperand = expression.rightOperand
  if (
    leftOperand is FieldAccess &&
      expression.isSimpleAssignment &&
      leftOperand.target.isStatic &&
      leftOperand.target.isFinal
  ) {
    render(identifierSource(leftOperand.target.ktMangledName))
  } else {
    renderLeftSubExpression(expression.precedence, leftOperand)
  }
  render(" ")
  renderBinaryOperator(
    expression.operator,
    useEquality =
      leftOperand is NullLiteral ||
        rightOperand is NullLiteral ||
        (leftOperand.typeDescriptor.isPrimitive && rightOperand.typeDescriptor.isPrimitive)
  )
  render(" ")
  renderRightSubExpression(expression.precedence, rightOperand)
}

private fun Renderer.renderCastExpression(castExpression: CastExpression) {
  val castTypeDescriptor = castExpression.castTypeDescriptor
  if (castTypeDescriptor is IntersectionTypeDescriptor) {
    // Render cast to intersection type descriptor: (A & B & C) x
    // using smart casts: (x).let { it as A; it as B; it as C; it }
    renderInParentheses { renderExpression(castExpression.expression) }
    render(".")
    render(extensionMemberQualifiedNameSource("kotlin.let"))
    render(" { ")
    castTypeDescriptor.intersectionTypeDescriptors.forEach {
      render("it as ")
      render(typeDescriptorSource(it))
      render("; ")
    }
    render("it }")
  } else {
    renderLeftSubExpression(castExpression.precedence, castExpression.expression)
    render(" as ")
    render(typeDescriptorSource(castExpression.castTypeDescriptor))
  }
}

private fun Renderer.renderBinaryOperator(operator: BinaryOperator, useEquality: Boolean) {
  render(operator.ktSymbol(useEquality))
}

private fun BinaryOperator.ktSymbol(useEquality: Boolean) =
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
    BinaryOperator.EQUALS -> if (useEquality) "==" else "==="
    BinaryOperator.NOT_EQUALS -> if (useEquality) "!=" else "!=="
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
  render(identifierSource(fieldAccess.target.ktMangledName))
}

private fun Renderer.renderFunctionExpression(functionExpression: FunctionExpression) {
  val functionalInterface = functionExpression.typeDescriptor.functionalInterface!!
  renderNewInstanceTypeDescriptor(functionalInterface)
  render(" ")
  renderInCurlyBrackets {
    val parameters = functionExpression.parameters
    if (parameters.isNotEmpty()) {
      render(" ")
      renderCommaSeparated(parameters) { renderVariable(it) }
      render(" ->")
    }
    val returnLabelIdentifier =
      with(functionalInterface.typeDeclaration) { ktBridgeSimpleName ?: ktSimpleName }
    copy(currentReturnLabelIdentifier = returnLabelIdentifier).run {
      render(statementsSource(functionExpression.body.statements).ifNotEmpty(::inNewLine))
    }
  }
}

private fun Renderer.renderInstanceOfExpression(instanceOfExpression: InstanceOfExpression) {
  renderLeftSubExpression(instanceOfExpression.precedence, instanceOfExpression.expression)
  render(" is ")
  val testTypeDescriptor = instanceOfExpression.testTypeDescriptor
  if (
    testTypeDescriptor is ArrayTypeDescriptor &&
      !testTypeDescriptor.componentTypeDescriptor!!.isPrimitive
  ) {
    render(topLevelQualifiedNameSource("kotlin.Array"))
    render("<*>")
  } else {
    render(
      typeDescriptorSource(
        instanceOfExpression.testTypeDescriptor.toNonNullable(),
        projectRawToWildcards = true
      )
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
  render(literalSource(stringLiteral.value))
}

private fun Renderer.renderTypeLiteral(typeLiteral: TypeLiteral) {
  render(qualifiedNameSource(typeLiteral.referencedTypeDescriptor))
  render("::class")
  render(".")
  if (typeLiteral.referencedTypeDescriptor.isPrimitive) {
    render(extensionMemberQualifiedNameSource("kotlin.jvm.javaPrimitiveType"))
    renderNonNullAssertion()
  } else {
    render(extensionMemberQualifiedNameSource("kotlin.jvm.javaObjectType"))
  }
}

private fun Renderer.renderNumberLiteral(numberLiteral: NumberLiteral) {
  when (numberLiteral.typeDescriptor.toUnboxedType()) {
    PrimitiveTypes.CHAR -> render(literalSource(numberLiteral.value.toChar()))
    PrimitiveTypes.BYTE -> render(literalSource(numberLiteral.value.toByte()))
    PrimitiveTypes.SHORT -> render(literalSource(numberLiteral.value.toShort()))
    PrimitiveTypes.INT -> render(literalSource(numberLiteral.value.toInt()))
    PrimitiveTypes.LONG -> render(literalSource(numberLiteral.value.toLong()))
    PrimitiveTypes.FLOAT -> render(literalSource(numberLiteral.value.toFloat()))
    PrimitiveTypes.DOUBLE -> render(literalSource(numberLiteral.value.toDouble()))
    else -> throw InternalCompilerError("renderNumberLiteral($numberLiteral)")
  }
}

private fun Renderer.renderConditionalExpression(conditionalExpression: ConditionalExpression) {
  render("if ")
  renderInParentheses { renderExpression(conditionalExpression.conditionExpression) }
  render(" ")
  renderExpression(conditionalExpression.trueExpression)
  render(" else ")
  renderExpression(conditionalExpression.falseExpression)
}

private fun Renderer.renderMethodCall(expression: MethodCall) {
  renderQualifier(expression)

  val methodDescriptor = expression.target
  if (methodDescriptor.isProtoExtensionGetter()) {
    render(extensionMemberQualifiedNameSource("com.google.protobuf.kotlin.get"))
    renderInvocation(expression)
  } else if (methodDescriptor.isProtobufGetter()) {
    render(identifierSource(KtInfo.computePropertyName(expression.target.name)))
  } else if (methodDescriptor.isProtoExtensionChecker()) {
    render(extensionMemberQualifiedNameSource("com.google.protobuf.kotlin.contains"))
    renderInvocation(expression)
  } else {
    render(identifierSource(expression.target.ktMangledName))
    if (!expression.target.isKtProperty) {
      renderInvocationTypeArguments(methodDescriptor.typeArguments)
      renderInvocation(expression)
    }
  }
}

private fun Renderer.renderInvocationTypeArguments(typeArguments: List<TypeArgument>) {
  if (typeArguments.isNotEmpty() && typeArguments.all { it.isDenotable }) {
    render(typeArgumentsSource(typeArguments))
  }
}

internal fun Renderer.invocationSource(invocation: Invocation) = renderedSource {
  renderInvocation(invocation)
}

internal fun Renderer.renderInvocation(invocation: Invocation) {
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
          if (argument.typeDescriptor.isNullable) renderNonNullAssertion()
        } else {
          renderExpression(argument)
        }
      }
    }
  }
}

private fun Renderer.renderMultiExpression(multiExpression: MultiExpression) {
  render(extensionMemberQualifiedNameSource("kotlin.run"))
  render(" ")
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
  val typeArgument = arrayTypeDescriptor.typeArgument
  val componentTypeDescriptor = typeArgument.typeDescriptor
  if (!remainingDimensions.hasNext()) {
    if (componentTypeDescriptor is PrimitiveTypeDescriptor) {
      renderPrimitiveArrayOf(componentTypeDescriptor, firstDimension)
    } else {
      renderArrayOfNulls(typeArgument, firstDimension)
    }
  } else {
    val nextDimension = remainingDimensions.next()
    if (nextDimension is NullLiteral) {
      renderArrayOfNulls(typeArgument, firstDimension)
    } else {
      render(topLevelQualifiedNameSource("kotlin.Array"))
      render(typeArgumentsSource(listOf(typeArgument)))
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
  render(
    topLevelQualifiedNameSource(
      when (componentTypeDescriptor) {
        PrimitiveTypes.BOOLEAN -> "kotlin.BooleanArray"
        PrimitiveTypes.CHAR -> "kotlin.CharArray"
        PrimitiveTypes.BYTE -> "kotlin.ByteArray"
        PrimitiveTypes.SHORT -> "kotlin.ShortArray"
        PrimitiveTypes.INT -> "kotlin.IntArray"
        PrimitiveTypes.LONG -> "kotlin.LongArray"
        PrimitiveTypes.FLOAT -> "kotlin.FloatArray"
        PrimitiveTypes.DOUBLE -> "kotlin.DoubleArray"
        else -> throw InternalCompilerError("renderPrimitiveArrayOf($componentTypeDescriptor)")
      }
    )
  )
  renderInParentheses { renderExpression(dimension) }
}

private fun Renderer.renderArrayOfNulls(typeArgument: TypeArgument, dimension: Expression) {
  if (typeArgument.typeDescriptor.isNullable) {
    render(extensionMemberQualifiedNameSource("kotlin.arrayOfNulls"))
    render(typeArgumentsSource(listOf(typeArgument.toNonNullable())))
  } else {
    render(extensionMemberQualifiedNameSource("javaemul.lang.uninitializedArrayOf"))
    render(typeArgumentsSource(listOf(typeArgument)))
  }
  renderInParentheses { renderExpression(dimension) }
}

private fun Renderer.renderNewInstance(expression: NewInstance) {
  renderQualifier(expression)

  var typeDescriptor = expression.typeDescriptor.nonAnonymousTypeDescriptor.toNonNullable()
  if (expression.anonymousInnerClass != null) {
    render("object : ")
  }

  renderNewInstanceTypeDescriptor(typeDescriptor)

  // Render invocation arguments for classes only - interfaces don't need it.
  if (typeDescriptor.isClass) {
    // Explicit label is necessary to workaround https://youtrack.jetbrains.com/issue/KT-54349
    copy(renderThisReferenceWithLabel = true).renderInvocation(expression)
  }

  expression.anonymousInnerClass?.let { render(typeBodySource(it).ifNotEmpty(::afterSpace)) }
}

private fun Renderer.renderNewInstanceTypeDescriptor(typeDescriptor: DeclaredTypeDescriptor) {
  // Render qualified name if there's no qualifier, otherwise render simple name.
  val typeDeclaration = typeDescriptor.typeDeclaration
  if (typeDeclaration.isCapturingEnclosingInstance) {
    render(identifierSource(typeDeclaration.ktSimpleName(asSuperType = true)))
  } else {
    render(qualifiedNameSource(typeDescriptor, asSuperType = true))
  }

  renderInvocationTypeArguments(typeDescriptor.typeArguments())
}

private val DeclaredTypeDescriptor.nonAnonymousTypeDescriptor: DeclaredTypeDescriptor
  get() =
    if (typeDeclaration.isAnonymous) interfaceTypeDescriptors.firstOrNull() ?: superTypeDescriptor!!
    else this

private fun Renderer.renderPostfixExpression(expression: PostfixExpression) {
  renderLeftSubExpression(expression.precedence, expression.operand)
  render(expression.operator.symbol)
}

private fun Renderer.renderPrefixExpression(expression: PrefixExpression) {
  expression.operator.let {
    render(it.symbol)
    // Emit a space after + and minus to avoid emitting + + as ++ and - -  and --.
    if (it == PrefixOperator.PLUS || it == PrefixOperator.MINUS) sourceBuilder.append(" ")
  }
  renderRightSubExpression(expression.precedence, expression.operand)
}

private fun Renderer.renderSuperReference(superReference: SuperReference) {
  renderSuperReference(superTypeDescriptor = null, qualifierTypeDescriptor = null)
}

private fun Renderer.renderSuperReference(
  superTypeDescriptor: DeclaredTypeDescriptor?,
  qualifierTypeDescriptor: DeclaredTypeDescriptor?
) {
  render("super")
  if (superTypeDescriptor != null) {
    renderInAngleBrackets { render(qualifiedNameSource(superTypeDescriptor, asSuperType = true)) }
  }
  if (qualifierTypeDescriptor != null) {
    renderLabelReference(qualifierTypeDescriptor)
  }
}

private fun Renderer.renderThisReference(thisReference: ThisReference) {
  render("this")
  if (thisReference.isQualified || renderThisReferenceWithLabel) {
    renderLabelReference(thisReference.typeDescriptor)
  }
}

private fun Renderer.renderLabelReference(typeDescriptor: DeclaredTypeDescriptor) {
  render(at(identifierSource(typeDescriptor.typeDeclaration.ktSimpleName)))
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
  if (typeDescriptor.isKtDenotable && !typeDescriptor.isProtobufBuilder()) {
    render(": ")
    render(typeDescriptorSource(typeDescriptor))
  }
}

private fun Renderer.renderQualifier(memberReference: MemberReference) {
  val qualifier = memberReference.qualifier
  if (qualifier == null) {
    if (memberReference.target.isStatic) {
      // TODO(b/206482966): Move the checks in the backend to a verifier pass.
      val enclosingTypeDescriptor = memberReference.target.enclosingTypeDescriptor!!
      val ktCompanionQualifiedName =
        enclosingTypeDescriptor.typeDeclaration.ktCompanionQualifiedName
      render(
        if (ktCompanionQualifiedName != null) topLevelQualifiedNameSource(ktCompanionQualifiedName)
        else qualifiedNameSource(enclosingTypeDescriptor)
      )
      render(".")
    }
  } else {
    if (memberReference is MethodCall && qualifier is SuperReference) {
      val qualifierTypeDescriptor = qualifier.typeDescriptor
      renderSuperReference(
        superTypeDescriptor =
          qualifierTypeDescriptor
            .directSuperTypeForMethodCall(memberReference.target)
            // Don't render <Any> (see: KT-54346)
            ?.takeIf { !isJavaLangObject(it) },
        qualifierTypeDescriptor =
          qualifierTypeDescriptor.takeIf { it.typeDeclaration != currentType!!.declaration }
      )
      render(".")
    } else if (
      memberReference is NewInstance && memberReference.typeDescriptor.typeDeclaration.isLocal
    ) {
      // Don't render qualifier for local classes.
      // TODO(b/219950593): Implement a pass which will remove unnecessary qualifiers, and then
      // remove this `if` branch.
    } else if (memberReference.target.isInstanceMember || !qualifier.isNonQualifiedThisReference) {
      renderLeftSubExpression(memberReference.precedence, qualifier)
      render(".")
    }
  }
}

private fun Renderer.renderLeftSubExpression(precedence: Precedence, operand: Expression) {
  renderExpressionInParens(operand, precedence.requiresParensOnLeft(operand.precedence))
}

private fun Renderer.renderRightSubExpression(precedence: Precedence, operand: Expression) {
  renderExpressionInParens(operand, precedence.requiresParensOnRight(operand.precedence))
}

private fun Renderer.renderExpressionInParens(expression: Expression, needsParentheses: Boolean) {
  if (needsParentheses) renderInParentheses { renderExpression(expression) }
  else renderExpression(expression)
}

private fun Renderer.renderNonNullAssertion() {
  render("!!")
}

private val Expression.isNonQualifiedThisReference
  get() = this is ThisReference && !isQualified

private fun Precedence.requiresParensOnLeft(operand: Precedence) =
  operand == Precedence.CONDITIONAL ||
    value > operand.value ||
    (associativity != Associativity.LEFT && this == operand)

private fun Precedence.requiresParensOnRight(operand: Precedence) =
  value > operand.value || (associativity != Associativity.RIGHT && this == operand)
