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
import com.google.j2cl.transpiler.ast.JsDocCastExpression
import com.google.j2cl.transpiler.ast.JsDocExpression
import com.google.j2cl.transpiler.ast.Literal
import com.google.j2cl.transpiler.ast.MemberReference
import com.google.j2cl.transpiler.ast.MethodCall
import com.google.j2cl.transpiler.ast.MultiExpression
import com.google.j2cl.transpiler.ast.NewArray
import com.google.j2cl.transpiler.ast.NewInstance
import com.google.j2cl.transpiler.ast.NullLiteral
import com.google.j2cl.transpiler.ast.NumberLiteral
import com.google.j2cl.transpiler.ast.PostfixExpression
import com.google.j2cl.transpiler.ast.PostfixOperator
import com.google.j2cl.transpiler.ast.PrefixExpression
import com.google.j2cl.transpiler.ast.PrefixOperator
import com.google.j2cl.transpiler.ast.PrimitiveTypeDescriptor
import com.google.j2cl.transpiler.ast.PrimitiveTypes
import com.google.j2cl.transpiler.ast.StringLiteral
import com.google.j2cl.transpiler.ast.SuperReference
import com.google.j2cl.transpiler.ast.ThisReference
import com.google.j2cl.transpiler.ast.TypeDeclaration
import com.google.j2cl.transpiler.ast.TypeDescriptor
import com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject
import com.google.j2cl.transpiler.ast.TypeLiteral
import com.google.j2cl.transpiler.ast.Variable
import com.google.j2cl.transpiler.ast.VariableDeclarationExpression
import com.google.j2cl.transpiler.ast.VariableDeclarationFragment
import com.google.j2cl.transpiler.ast.VariableReference
import com.google.j2cl.transpiler.backend.kotlin.common.letIf
import com.google.j2cl.transpiler.backend.kotlin.source.Source
import com.google.j2cl.transpiler.backend.kotlin.source.block
import com.google.j2cl.transpiler.backend.kotlin.source.colonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.commaSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.dotSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.emptySource
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotEmpty
import com.google.j2cl.transpiler.backend.kotlin.source.ifNotNullSource
import com.google.j2cl.transpiler.backend.kotlin.source.inAngleBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inInlineCurlyBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inRoundBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.inSquareBrackets
import com.google.j2cl.transpiler.backend.kotlin.source.infix
import com.google.j2cl.transpiler.backend.kotlin.source.join
import com.google.j2cl.transpiler.backend.kotlin.source.newLineSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.semicolonSeparated
import com.google.j2cl.transpiler.backend.kotlin.source.source
import com.google.j2cl.transpiler.backend.kotlin.source.sourceIf
import com.google.j2cl.transpiler.backend.kotlin.source.spaceSeparated

fun Renderer.expressionSource(expression: Expression): Source =
  when (expression) {
    is ArrayAccess -> arrayAccessSource(expression)
    is ArrayLength -> arrayLengthSource(expression)
    is ArrayLiteral -> arrayLiteralSource(expression)
    is BinaryExpression -> binaryExpressionSource(expression)
    is CastExpression -> castExpressionSource(expression)
    is ConditionalExpression -> conditionalExpressionSource(expression)
    is ExpressionWithComment -> expressionWithCommentSource(expression)
    is FieldAccess -> fieldAccessSource(expression)
    is FunctionExpression -> functionExpressionSource(expression)
    is InstanceOfExpression -> instanceOfExpressionSource(expression)
    is JsDocExpression -> jsDocExpressionSource(expression)
    is JsDocCastExpression -> jsDocCastExpressionSource(expression)
    is Literal -> literalSource(expression)
    is MethodCall -> methodCallSource(expression)
    is MultiExpression -> multiExpressionSource(expression)
    is NewArray -> newArraySource(expression)
    is NewInstance -> newInstanceSource(expression)
    is PostfixExpression -> postfixExpressionSource(expression)
    is PrefixExpression -> prefixExpressionSource(expression)
    is SuperReference -> superReferenceSource(expression)
    is ThisReference -> thisReferenceSource(expression)
    is VariableDeclarationExpression -> variableDeclarationExpressionSource(expression)
    is VariableReference -> variableReferenceSource(expression)
    else -> throw InternalCompilerError("Unexpected ${expression::class.java.simpleName}")
  }

private fun Renderer.arrayAccessSource(arrayAccess: ArrayAccess): Source =
  getOperatorSource(arrayAccess.arrayExpression, arrayAccess.indexExpression)

private fun Renderer.getOperatorSource(qualifier: Expression, argument: Expression): Source =
  join(
    expressionInParensSource(
      qualifier,
      Precedence.MEMBER_ACCESS.requiresParensOnLeft(qualifier.precedence)
    ),
    inSquareBrackets(expressionSource(argument))
  )

private fun Renderer.arrayLengthSource(arrayLength: ArrayLength): Source =
  dotSeparated(
    leftSubExpressionSource(arrayLength.precedence, arrayLength.arrayExpression),
    source("size")
  )

private fun Renderer.arrayLiteralSource(arrayLiteral: ArrayLiteral): Source =
  arrayLiteral.typeDescriptor.typeArgument.let { typeArgument ->
    join(
      when (typeArgument.typeDescriptor) {
        PrimitiveTypes.BOOLEAN -> topLevelQualifiedNameSource("kotlin.booleanArrayOf")
        PrimitiveTypes.CHAR -> topLevelQualifiedNameSource("kotlin.charArrayOf")
        PrimitiveTypes.BYTE -> topLevelQualifiedNameSource("kotlin.byteArrayOf")
        PrimitiveTypes.SHORT -> topLevelQualifiedNameSource("kotlin.shortArrayOf")
        PrimitiveTypes.INT -> topLevelQualifiedNameSource("kotlin.intArrayOf")
        PrimitiveTypes.LONG -> topLevelQualifiedNameSource("kotlin.longArrayOf")
        PrimitiveTypes.FLOAT -> topLevelQualifiedNameSource("kotlin.floatArrayOf")
        PrimitiveTypes.DOUBLE -> topLevelQualifiedNameSource("kotlin.doubleArrayOf")
        else ->
          join(
            topLevelQualifiedNameSource("kotlin.arrayOf"),
            typeArgumentsSource(listOf(typeArgument))
          )
      },
      inRoundBrackets(commaSeparated(arrayLiteral.valueExpressions.map(::expressionSource)))
    )
  }

private fun Renderer.binaryExpressionSource(expression: BinaryExpression): Source =
  infix(
    leftOperandSource(expression),
    expression.operator.ktSymbol(expression.useEquality),
    rightOperandSource(expression)
  )

private fun Renderer.leftOperandSource(expression: BinaryExpression): Source =
  // Java and Kotlin does not allow initializing static final fields with type qualifier, so it
  // needs to be rendered without the qualifier.
  expression.leftOperand.let { leftOperand ->
    if (
      leftOperand is FieldAccess &&
        expression.isSimpleAssignment &&
        leftOperand.target.isStatic &&
        leftOperand.target.isFinal
    )
      identifierSource(leftOperand.target.ktMangledName)
    else leftSubExpressionSource(expression.precedence, leftOperand)
  }

private fun Renderer.rightOperandSource(expression: BinaryExpression): Source =
  rightSubExpressionSource(expression.precedence, expression.rightOperand)

private val BinaryExpression.useEquality: Boolean
  get() =
    leftOperand is NullLiteral ||
      rightOperand is NullLiteral ||
      (leftOperand.typeDescriptor.isPrimitive && rightOperand.typeDescriptor.isPrimitive)

private fun Renderer.castExpressionSource(castExpression: CastExpression): Source =
  castExpression.castTypeDescriptor.let { castTypeDescriptor ->
    if (castTypeDescriptor is IntersectionTypeDescriptor)
    // Render cast to intersection type descriptor: (A & B & C) x
    // using smart casts: (x).let { it as A; it as B; it as C; it }
    dotSeparated(
        inRoundBrackets(expressionSource(castExpression.expression)),
        spaceSeparated(
          extensionMemberQualifiedNameSource("kotlin.let"),
          inInlineCurlyBrackets(
            semicolonSeparated(
              castTypeDescriptor.intersectionTypeDescriptors
                .map { asExpression(itSource(), castTypeDescriptorSource(it)) }
                .plus(itSource())
            )
          )
        )
      )
    else
      asExpression(
        leftSubExpressionSource(castExpression.precedence, castExpression.expression),
        castTypeDescriptorSource(castExpression.castTypeDescriptor)
      )
  }

private fun Renderer.castTypeDescriptorSource(typeDescriptor: TypeDescriptor): Source =
  typeDescriptorSource(typeDescriptor).letIf(typeDescriptor.variableHasAmpersandAny) {
    inRoundBrackets(it)
  }

private fun BinaryOperator.ktSymbol(useEquality: Boolean): String =
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
    BinaryOperator.LEFT_SHIFT,
    BinaryOperator.RIGHT_SHIFT_SIGNED,
    BinaryOperator.RIGHT_SHIFT_UNSIGNED,
    BinaryOperator.BIT_XOR,
    BinaryOperator.BIT_AND,
    BinaryOperator.BIT_OR,
    BinaryOperator.PLUS_ASSIGN,
    BinaryOperator.MINUS_ASSIGN,
    BinaryOperator.TIMES_ASSIGN,
    BinaryOperator.DIVIDE_ASSIGN,
    BinaryOperator.BIT_AND_ASSIGN,
    BinaryOperator.BIT_OR_ASSIGN,
    BinaryOperator.BIT_XOR_ASSIGN,
    BinaryOperator.REMAINDER_ASSIGN,
    BinaryOperator.LEFT_SHIFT_ASSIGN,
    BinaryOperator.RIGHT_SHIFT_SIGNED_ASSIGN,
    BinaryOperator.RIGHT_SHIFT_UNSIGNED_ASSIGN -> throw InternalCompilerError("$this.ktSymbol")
  }

private val PrefixOperator.ktSymbol: String
  get() =
    when (this) {
      PrefixOperator.PLUS -> "+"
      PrefixOperator.MINUS -> "-"
      PrefixOperator.NOT -> "!"
      PrefixOperator.SPREAD -> "*"
      PrefixOperator.INCREMENT -> "++"
      PrefixOperator.DECREMENT -> "--"
      PrefixOperator.COMPLEMENT -> throw InternalCompilerError("$this.ktSymbol")
    }

private val PostfixOperator.ktSymbol: String
  get() =
    when (this) {
      PostfixOperator.DECREMENT -> "--"
      PostfixOperator.INCREMENT -> "++"
      PostfixOperator.NOT_NULL_ASSERTION -> "!!"
    }

private fun Renderer.conditionalExpressionSource(
  conditionalExpression: ConditionalExpression
): Source =
  spaceSeparated(
    source("if"),
    inRoundBrackets(expressionSource(conditionalExpression.conditionExpression)),
    expressionSource(conditionalExpression.trueExpression),
    source("else"),
    expressionSource(conditionalExpression.falseExpression)
  )

private fun Renderer.expressionWithCommentSource(
  expressionWithComment: ExpressionWithComment
): Source = expressionSource(expressionWithComment.expression)

private fun Renderer.fieldAccessSource(fieldAccess: FieldAccess): Source =
  dotSeparated(qualifierSource(fieldAccess), identifierSource(fieldAccess.target.ktMangledName))

private val FunctionExpression.renderAsLambda: Boolean
  get() = typeDescriptor.functionalInterface!!.typeDeclaration.isKtFunctionalInterface

private fun Renderer.functionExpressionSource(functionExpression: FunctionExpression): Source =
  if (functionExpression.renderAsLambda) functionExpressionLambdaSource(functionExpression)
  else functionExpressionObjectSource(functionExpression)

private fun Renderer.functionExpressionLambdaSource(
  functionExpression: FunctionExpression
): Source =
  spaceSeparated(
    newInstanceTypeDescriptorSource(functionExpression.typeDescriptor.functionalInterface!!),
    block(parametersSource(functionExpression), lambdaBodySource(functionExpression))
  )

private fun Renderer.lambdaBodySource(functionExpression: FunctionExpression): Source =
  copy(
      currentReturnLabelIdentifier =
        functionExpression.typeDescriptor.functionalInterface!!
          .typeDeclaration
          .returnLabelIdentifier
    )
    .statementsSource(functionExpression.body.statements)

private fun Renderer.functionExpressionObjectSource(
  functionExpression: FunctionExpression
): Source =
  spaceSeparated(
    source("object"),
    source(":"),
    newInstanceTypeDescriptorSource(functionExpression.typeDescriptor.functionalInterface!!),
    block(
      spaceSeparated(
        methodHeaderSource(functionExpression),
        block(objectBodySource(functionExpression))
      )
    )
  )

private fun Renderer.objectBodySource(functionExpression: FunctionExpression): Source =
  copy(renderThisReferenceWithLabel = true).statementsSource(functionExpression.body.statements)

private fun Renderer.parametersSource(functionExpression: FunctionExpression): Source =
  commaSeparated(functionExpression.parameters.map(::variableSource)).ifNotEmpty {
    spaceSeparated(it, source("->"))
  }

private val TypeDeclaration.returnLabelIdentifier: String
  get() = ktBridgeSimpleName ?: ktSimpleName

private fun Renderer.instanceOfExpressionSource(
  instanceOfExpression: InstanceOfExpression
): Source =
  isExpression(
    leftSubExpressionSource(instanceOfExpression.precedence, instanceOfExpression.expression),
    instanceOfTestTypeDescriptorSource(instanceOfExpression.testTypeDescriptor)
  )

private fun Renderer.jsDocExpressionSource(expression: JsDocExpression): Source =
  expressionSource(expression.expression)

private fun Renderer.jsDocCastExpressionSource(expression: JsDocCastExpression): Source =
  expressionSource(expression.expression)

private fun Renderer.instanceOfTestTypeDescriptorSource(typeDescriptor: TypeDescriptor): Source =
  if (typeDescriptor is ArrayTypeDescriptor && !typeDescriptor.isPrimitiveArray)
    join(topLevelQualifiedNameSource("kotlin.Array"), inAngleBrackets(source("*")))
  else typeDescriptorSource(typeDescriptor.toNonNullable(), projectRawToWildcards = true)

private fun Renderer.literalSource(literal: Literal): Source =
  when (literal) {
    is NullLiteral -> source("null")
    is BooleanLiteral -> booleanLiteralSource(literal)
    is StringLiteral -> stringLiteralSource(literal)
    is TypeLiteral -> typeLiteralSource(literal)
    is NumberLiteral -> numberLiteralSource(literal)
    else -> throw InternalCompilerError("renderLiteral($literal)")
  }

private fun booleanLiteralSource(booleanLiteral: BooleanLiteral): Source =
  literalSource(booleanLiteral.value)

private fun stringLiteralSource(stringLiteral: StringLiteral): Source =
  literalSource(stringLiteral.value)

private fun Renderer.typeLiteralSource(typeLiteral: TypeLiteral): Source =
  dotSeparated(
    classLiteral(qualifiedNameSource(typeLiteral.referencedTypeDescriptor)),
    if (typeLiteral.referencedTypeDescriptor.isPrimitive)
      nonNull(extensionMemberQualifiedNameSource("kotlin.jvm.javaPrimitiveType"))
    else extensionMemberQualifiedNameSource("kotlin.jvm.javaObjectType")
  )

private fun numberLiteralSource(numberLiteral: NumberLiteral): Source =
  when (numberLiteral.typeDescriptor.toUnboxedType()) {
    PrimitiveTypes.CHAR -> literalSource(numberLiteral.value.toChar())
    PrimitiveTypes.INT -> literalSource(numberLiteral.value.toInt())
    PrimitiveTypes.LONG -> literalSource(numberLiteral.value.toLong())
    PrimitiveTypes.FLOAT -> literalSource(numberLiteral.value.toFloat())
    PrimitiveTypes.DOUBLE -> literalSource(numberLiteral.value.toDouble())
    else -> throw InternalCompilerError("renderNumberLiteral($numberLiteral)")
  }

private fun Renderer.methodCallSource(expression: MethodCall): Source =
  dotSeparated(qualifierSource(expression), methodInvocationSource(expression))

private fun Renderer.methodInvocationSource(expression: MethodCall): Source =
  expression.target.let { methodDescriptor ->
    when {
      methodDescriptor.isProtoExtensionGetter() ->
        join(
          extensionMemberQualifiedNameSource("com.google.protobuf.kotlin.get"),
          invocationSource(expression)
        )
      methodDescriptor.isProtobufGetter() ->
        identifierSource(computeProtobufPropertyName(expression.target.name!!))
      methodDescriptor.isProtoExtensionChecker() ->
        join(
          extensionMemberQualifiedNameSource("com.google.protobuf.kotlin.contains"),
          invocationSource(expression)
        )
      else ->
        join(
          identifierSource(expression.target.ktMangledName),
          sourceIf(!expression.target.isKtProperty) {
            join(
              invocationTypeArgumentsSource(methodDescriptor.typeArguments),
              invocationSource(expression)
            )
          }
        )
    }
  }

private fun Renderer.invocationTypeArgumentsSource(typeArguments: List<TypeArgument>): Source =
  sourceIf(typeArguments.isNotEmpty() && typeArguments.all { it.isDenotable }) {
    typeArgumentsSource(typeArguments)
  }

internal fun Renderer.invocationSource(invocation: Invocation) =
  inRoundBrackets(commaSeparated(invocation.arguments.map(::expressionSource)))

private fun Renderer.multiExpressionSource(multiExpression: MultiExpression): Source =
  spaceSeparated(
    extensionMemberQualifiedNameSource("kotlin.run"),
    block(newLineSeparated(multiExpression.expressions.map(::expressionSource)))
  )

private fun Renderer.newArraySource(newArray: NewArray): Source =
  newArraySource(
    newArray.typeDescriptor,
    newArray.dimensionExpressions.first(),
    newArray.dimensionExpressions.drop(1)
  )

private fun Renderer.newArraySource(
  arrayTypeDescriptor: ArrayTypeDescriptor,
  firstDimension: Expression,
  remainingDimensions: List<Expression>
): Source =
  arrayTypeDescriptor.typeArgument.let { typeArgument ->
    typeArgument.typeDescriptor.let { componentTypeDescriptor ->
      if (remainingDimensions.isEmpty())
        if (componentTypeDescriptor is PrimitiveTypeDescriptor)
          primitiveArrayOfSource(componentTypeDescriptor, firstDimension)
        else arrayOfNullsSource(typeArgument, firstDimension)
      else
        remainingDimensions.first().let { nextDimension ->
          if (nextDimension is NullLiteral) arrayOfNullsSource(typeArgument, firstDimension)
          else
            spaceSeparated(
              join(
                topLevelQualifiedNameSource("kotlin.Array"),
                typeArgumentsSource(listOf(typeArgument)),
                inRoundBrackets(expressionSource(firstDimension))
              ),
              block(
                newArraySource(
                  componentTypeDescriptor as ArrayTypeDescriptor,
                  nextDimension,
                  remainingDimensions.drop(1)
                )
              )
            )
        }
    }
  }

private fun Renderer.primitiveArrayOfSource(
  componentTypeDescriptor: PrimitiveTypeDescriptor,
  dimension: Expression
): Source =
  join(
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
    ),
    inRoundBrackets(expressionSource(dimension))
  )

private fun Renderer.arrayOfNullsSource(typeArgument: TypeArgument, dimension: Expression): Source =
  join(
    if (typeArgument.typeDescriptor.isNullable)
      join(
        extensionMemberQualifiedNameSource("kotlin.arrayOfNulls"),
        typeArgumentsSource(listOf(typeArgument.toNonNullable()))
      )
    else
      join(
        extensionMemberQualifiedNameSource("javaemul.lang.uninitializedArrayOf"),
        typeArgumentsSource(listOf(typeArgument))
      ),
    inRoundBrackets(expressionSource(dimension))
  )

private fun Renderer.newInstanceSource(expression: NewInstance): Source =
  expression.typeDescriptor.nonAnonymousTypeDescriptor.toNonNullable().let { typeDescriptor ->
    dotSeparated(
      qualifierSource(expression),
      spaceSeparated(
        sourceIf(expression.anonymousInnerClass != null) {
          spaceSeparated(source("object"), source(":"))
        },
        join(
          newInstanceTypeDescriptorSource(typeDescriptor),
          // Render invocation arguments for classes only - interfaces don't need it.
          sourceIf(typeDescriptor.isClass) {
            // Explicit label is necessary to workaround
            // https://youtrack.jetbrains.com/issue/KT-54349
            copy(renderThisReferenceWithLabel = true).invocationSource(expression)
          }
        ),
        expression.anonymousInnerClass.ifNotNullSource { typeBodySource(it) }
      )
    )
  }

private fun Renderer.newInstanceTypeDescriptorSource(
  typeDescriptor: DeclaredTypeDescriptor
): Source =
  // Render qualified name if there's no qualifier, otherwise render simple name.
  typeDescriptor.typeDeclaration.let { typeDeclaration ->
    join(
      if (typeDeclaration.isCapturingEnclosingInstance)
        identifierSource(typeDeclaration.ktSimpleName(asSuperType = true))
      else qualifiedNameSource(typeDescriptor, asSuperType = true),
      invocationTypeArgumentsSource(typeDescriptor.typeArguments())
    )
  }

private val DeclaredTypeDescriptor.nonAnonymousTypeDescriptor: DeclaredTypeDescriptor
  get() =
    if (typeDeclaration.isAnonymous) interfaceTypeDescriptors.firstOrNull() ?: superTypeDescriptor!!
    else this

private fun Renderer.postfixExpressionSource(expression: PostfixExpression): Source =
  join(
    leftSubExpressionSource(expression.precedence, expression.operand),
    source(expression.operator.ktSymbol)
  )

private fun Renderer.prefixExpressionSource(expression: PrefixExpression): Source =
  expression.operator.let { operator ->
    source(operator.ktSymbol).let { symbolSource ->
      rightSubExpressionSource(expression.precedence, expression.operand).let { operandSource ->
        if (operator.needsSpace) spaceSeparated(symbolSource, operandSource)
        else join(symbolSource, operandSource)
      }
    }
  }

private val PrefixOperator.needsSpace: Boolean
  get() = this == PrefixOperator.PLUS || this == PrefixOperator.MINUS

private fun Renderer.superReferenceSource(superReference: SuperReference): Source =
  superReferenceSource(superTypeDescriptor = null, qualifierTypeDescriptor = null)

private fun Renderer.superReferenceSource(
  superTypeDescriptor: DeclaredTypeDescriptor?,
  qualifierTypeDescriptor: DeclaredTypeDescriptor?
): Source =
  join(
    source("super"),
    superTypeDescriptor.ifNotNullSource {
      inAngleBrackets(qualifiedNameSource(it, asSuperType = true))
    },
    qualifierTypeDescriptor.ifNotNullSource { labelReferenceSource(it) }
  )

private fun Renderer.thisReferenceSource(thisReference: ThisReference): Source =
  join(
    source("this"),
    sourceIf(needsQualifier(thisReference)) { labelReferenceSource(thisReference.typeDescriptor) }
  )

private fun Renderer.needsQualifier(thisReference: ThisReference): Boolean =
  renderThisReferenceWithLabel || thisReference.isQualified

private fun labelReferenceSource(typeDescriptor: DeclaredTypeDescriptor): Source =
  at(identifierSource(typeDescriptor.typeDeclaration.ktSimpleName))

private fun Renderer.variableDeclarationExpressionSource(
  expression: VariableDeclarationExpression
): Source =
  newLineSeparated(
    expression.fragments.map {
      spaceSeparated(
        source(if (it.variable.isFinal) "val" else "var"),
        variableDeclarationFragmentSource(it)
      )
    }
  )

private fun Renderer.variableReferenceSource(variableReference: VariableReference): Source =
  nameSource(variableReference.target)

private fun Renderer.variableDeclarationFragmentSource(
  fragment: VariableDeclarationFragment
): Source =
  assignment(
    variableSource(fragment.variable),
    fragment.initializer.ifNotNullSource(::expressionSource)
  )

fun Renderer.variableSource(variable: Variable): Source =
  colonSeparated(
    nameSource(variable),
    variable.typeDescriptor.let {
      sourceIf(it.isKtDenotableNonWildcard) { typeDescriptorSource(it) }
    }
  )

private fun Renderer.qualifierSource(memberReference: MemberReference): Source =
  memberReference.qualifier.let { qualifier ->
    if (qualifier == null) {
      if (memberReference.target.isStatic) {
        // TODO(b/206482966): Move the checks in the backend to a verifier pass.
        val enclosingTypeDescriptor = memberReference.target.enclosingTypeDescriptor!!
        val ktCompanionQualifiedName =
          enclosingTypeDescriptor.typeDeclaration.ktCompanionQualifiedName
        if (ktCompanionQualifiedName != null) topLevelQualifiedNameSource(ktCompanionQualifiedName)
        else qualifiedNameSource(enclosingTypeDescriptor)
      } else {
        emptySource
      }
    } else {
      if (memberReference is MethodCall && qualifier is SuperReference) {
        qualifier.typeDescriptor.let { qualifierTypeDescriptor ->
          superReferenceSource(
            superTypeDescriptor =
              qualifierTypeDescriptor
                .directSuperTypeForMethodCall(memberReference.target)
                // Don't render <Any> (see: KT-54346)
                ?.takeIf { !isJavaLangObject(it) },
            qualifierTypeDescriptor =
              qualifierTypeDescriptor.takeIf { it.typeDeclaration != currentType!!.declaration }
          )
        }
      } else if (memberReference.isLocalNewInstance) {
        // Don't render qualifier for local classes.
        // TODO(b/219950593): Implement a pass which will remove unnecessary qualifiers, and then
        // remove this `if` branch.
        emptySource
      } else if (qualifier.isAnonymousThisReference) {
        emptySource
      } else if (
        memberReference.target.isInstanceMember || !qualifier.isNonQualifiedThisReference
      ) {
        leftSubExpressionSource(memberReference.precedence, qualifier)
      } else {
        emptySource
      }
    }
  }

private fun Renderer.leftSubExpressionSource(precedence: Precedence, operand: Expression) =
  expressionInParensSource(operand, precedence.requiresParensOnLeft(operand.precedence))

private fun Renderer.rightSubExpressionSource(precedence: Precedence, operand: Expression) =
  expressionInParensSource(operand, precedence.requiresParensOnRight(operand.precedence))

private fun Renderer.expressionInParensSource(expression: Expression, needsParentheses: Boolean) =
  expressionSource(expression).letIf(needsParentheses, ::inRoundBrackets)

private val Expression.isNonQualifiedThisReference: Boolean
  get() = this is ThisReference && (!isQualified || this.typeDescriptor.typeDeclaration.isAnonymous)

private val Expression.isAnonymousThisReference: Boolean
  get() = this is ThisReference && typeDescriptor.typeDeclaration.isAnonymous

private val MemberReference.isLocalNewInstance: Boolean
  get() = this is NewInstance && typeDescriptor.typeDeclaration.isLocal

private fun Precedence.requiresParensOnLeft(operand: Precedence): Boolean =
  operand == Precedence.CONDITIONAL ||
    value > operand.value ||
    (associativity != Associativity.LEFT && this == operand)

private fun Precedence.requiresParensOnRight(operand: Precedence): Boolean =
  value > operand.value || (associativity != Associativity.RIGHT && this == operand)
