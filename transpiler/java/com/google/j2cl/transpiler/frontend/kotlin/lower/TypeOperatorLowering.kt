/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.* // ktlint-disable
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.* // ktlint-disable
import org.jetbrains.kotlin.ir.declarations.* // ktlint-disable
import org.jetbrains.kotlin.ir.expressions.* // ktlint-disable
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.* // ktlint-disable
import org.jetbrains.kotlin.ir.util.* // ktlint-disable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Transforms IrTypeOperatorCalls to (implicit) casts and instanceof checks.
 *
 * After this pass runs there are only four kinds of IrTypeOperatorCalls left:
 * - IMPLICIT_CAST
 * - SAFE_CAST with reified type parameters
 * - INSTANCEOF with non-nullable type operand or reified type parameters
 * - CAST with non-nullable argument, nullable type operand, or reified type parameters
 *
 * The latter two correspond to the instanceof/checkcast instructions on the JVM, except for the
 * presence of reified type parameters.
 *
 * Based on org/jetbrains/kotlin/backend/jvm/lower/TypeOperatorLowering.kt. We removed everything
 * related to java.lang.invoke.LambdaMetafactory.metafactory (jvmIndyLambdaMetafactoryIntrinsic) and
 * lambda method serialization. We also had some comments to ease the understanding of the code.
 */
internal class TypeOperatorLowering(private val backendContext: JvmBackendContext) :
  FileLoweringPass, IrBuildingTransformer(backendContext) {

  override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

  private fun IrExpression.transformVoid() = transform(this@TypeOperatorLowering, null)

  private fun lowerInstanceOf(argument: IrExpression, type: IrType) =
    with(builder) {
      when {
        type.isReifiedTypeParameter -> irIs(argument, type)
        // var f: Foo?
        // f instanceof Bar?
        // lowered to: f.let { f == null || f is Bar }
        argument.type.isNullable() && type.isNullable() -> {
          // MODIFIED BY GOOGLE:
          // Previously the type of the variable was always set to kotlin.Any causing a loss of
          // type information.
          irLetS(argument) { valueSymbol ->
            // END OF MODIFICATIONS
            context.oror(
              irEqualsNull(irGet(valueSymbol.owner)),
              irIs(irGet(valueSymbol.owner), type.makeNotNull()),
            )
          }
        }
        // var f: Foo?
        // f instanceof Foo
        // lowered to: f.let { f != null }
        argument.type.isNullable() &&
          !type.isNullable() &&
          argument.type.erasedUpperBound == type.erasedUpperBound -> irNotEquals(argument, irNull())
        else -> irIs(argument, type.makeNotNull())
      }
    }

  private fun lowerCast(argument: IrExpression, type: IrType): IrExpression =
    when {
      // MODIFIED BY GOOGLE:
      // Skip cast operations when the argument type and the cast to type match.
      type == argument.type -> argument
      // END OF MODIFICATIONS
      type.isReifiedTypeParameter -> builder.irAs(argument, type)
      argument.type.isInlineClassType() &&
        argument.type.isSubtypeOfClass(type.erasedUpperBound.symbol) -> argument
      type.isNullable() || argument.isDefinitelyNotNull() -> builder.irAs(argument, type)
      // var f: Foo?
      // f as Bar
      // lowered to: f.let { if (it == null) throw ClassCastException() else f as Bar? }
      // Note that in the expression f as Bar?, Bar is now nullable
      else ->
        with(builder) {
          // MODIFIED BY GOOGLE:
          // The original code uses a let block to perform a not null check as a statement and then
          // return the cast value. We can simplify the output by instead rewriting it to:
          //   f!! as Bar?
          //
          // Original code:
          // irLetS(argument, irType = context.irBuiltIns.anyNType) { tmp ->
          //   // END OF MODIFICATIONS
          //   val message = irString("null cannot be cast to non-null type ${type.render()}")
          //   if (backendContext.config.unifiedNullChecks) {
          //     // Avoid branching to improve code coverage (KT-27427).
          //     // We have to generate a null check here, because even if argument is of non-null
          //     // type, it can be uninitialized value, which is 'null' for reference types in JMM.
          //     // Most of such null checks will never actually throw, but we can't do anything
          // about
          //     // it.
          //     irBlock(resultType = type) {
          //       +irCall(backendContext.ir.symbols.checkNotNullWithMessage).apply {
          //         putValueArgument(0, irGet(tmp.owner))
          //         putValueArgument(1, message)
          //       }
          //       +irAs(irGet(tmp.owner), type.makeNullable())
          //     }
          //   } else {
          //     irIfNull(
          //       type,
          //       irGet(tmp.owner),
          //       irCall(throwTypeCastException).apply { putValueArgument(0, message) },
          //       irAs(irGet(tmp.owner), type.makeNullable()),
          //     )
          //   }
          // }
          val transformedArgument = argument.transformVoid()
          irAs(
            irCall(context.irBuiltIns.checkNotNullSymbol).apply {
              this.type = transformedArgument.type.makeNotNull()
              putTypeArgument(0, transformedArgument.type.makeNotNull())
              putValueArgument(0, transformedArgument)
            },
            type,
          )
          // END OF MODIFICATIONS
        }
    }

  // TODO extract null check elimination on IR somewhere?
  private fun IrExpression.isDefinitelyNotNull(): Boolean =
    when (this) {
      is IrGetValue -> this.symbol.owner.isDefinitelyNotNullVal()
      is IrGetClass,
      is IrConstructorCall -> true
      is IrCall -> this.symbol == backendContext.irBuiltIns.checkNotNullSymbol
      // MODIFIED BY GOOGLE:
      // Handle additional cases that are guaranteed to be non-null.
      // A const expression is only null if it's literally null.
      is IrConst<*> -> this.value != null
      // If all result branches are definitely non-null, then the when-expression must be non-null.
      is IrWhen -> this.branches.all { it.result.isDefinitelyNotNull() }
      // END OF MODIFICATIONS
      else -> false
    }

  private fun IrValueDeclaration.isDefinitelyNotNullVal(): Boolean {
    val irVariable = this as? IrVariable ?: return false
    return !irVariable.isVar && irVariable.initializer?.isDefinitelyNotNull() == true
  }

  override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression =
    with(builder) {
      at(expression)
      return when (expression.operator) {
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
          irComposite(resultType = expression.type) {
            +expression.argument.transformVoid()
            // TODO: Don't generate these casts in the first place
            if (
              !expression.argument.type.isSubtypeOf(
                expression.type.makeNullable(),
                backendContext.typeSystem,
              )
            ) {
              +IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, expression.type)
            }
          }

        // There is no difference between IMPLICIT_CAST and IMPLICIT_INTEGER_COERCION on JVM_IR.
        // Instead, this is handled in StackValue.coerce.
        IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
          irImplicitCast(expression.argument.transformVoid(), expression.typeOperand)
        IrTypeOperator.CAST ->
          lowerCast(expression.argument.transformVoid(), expression.typeOperand)
        IrTypeOperator.SAFE_CAST ->
          if (expression.typeOperand.isReifiedTypeParameter) {
            expression.transformChildrenVoid()
            expression
          } else {
            // MODIFIED BY GOOGLE:
            // Previously the type of the variable was always set to kotlin.Any causing a loss of
            // type information.
            irLetS(expression.argument.transformVoid(), IrStatementOrigin.SAFE_CALL) { valueSymbol
              ->
              // END OF MODIFICATIONS
              // MODIFIED BY GOOGLE:
              // Ensure the "then" part is wrapped in an implicit cast. Later on this will ensure
              // we emit a JsDoc cast.
              val thenPart =
                irImplicitCast(
                  if (valueSymbol.owner.type.isInlineClassType())
                    lowerCast(irGet(valueSymbol.owner), expression.typeOperand)
                  else irGet(valueSymbol.owner),
                  expression.typeOperand.makeNotNull(),
                )
              // END OF MODIFICATIONS
              irIfThenElse(
                expression.type,
                lowerInstanceOf(irGet(valueSymbol.owner), expression.typeOperand.makeNotNull()),
                thenPart,
                irNull(expression.type),
              )
            }
          }
        IrTypeOperator.INSTANCEOF ->
          lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand)
        IrTypeOperator.NOT_INSTANCEOF ->
          irNot(lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand))
        IrTypeOperator.IMPLICIT_NOTNULL -> {
          // MODIFIED BY GOOGLE:
          // The original implementation lowered these into a composite which applies a null check
          // as a separate statement. Instead, we've opted to treat it like !! which just wraps the
          // expression in a checkNotNull() call, greatly simplifying the output code.
          //
          // val text = computeNotNullAssertionText(expression)
          //
          // irLetS(expression.argument.transformVoid(), irType = context.irBuiltIns.anyNType) {
          //   valueSymbol ->
          //   irComposite(resultType = expression.type) {
          //     if (text != null) {
          //       +irCall(checkExpressionValueIsNotNull).apply {
          //         putValueArgument(0, irGet(valueSymbol.owner))
          //         putValueArgument(1, irString(text.trimForRuntimeAssertion()))
          //       }
          //     } else {
          //       +irCall(backendContext.ir.symbols.checkNotNull).apply {
          //         putValueArgument(0, irGet(valueSymbol.owner))
          //       }
          //     }
          //     +irGet(valueSymbol.owner)
          //   }
          // }
          val argument = expression.argument.transformVoid()
          irCall(context.irBuiltIns.checkNotNullSymbol).apply {
            type = expression.type
            putTypeArgument(0, argument.type.makeNotNull())
            putValueArgument(0, argument)
          }
          // END OF MODIFICATIONS
        }
        else -> {
          expression.transformChildrenVoid()
          expression
        }
      }
    }

  private fun IrBuilderWithScope.computeNotNullAssertionText(
    typeOperatorCall: IrTypeOperatorCall
  ): String? {
    if (backendContext.config.noSourceCodeInNotNullAssertionExceptions) {
      return when (val argument = typeOperatorCall.argument) {
        is IrCall -> "${argument.symbol.owner.name.asString()}(...)"
        is IrGetField -> {
          val field = argument.symbol.owner
          field.name.asString().takeUnless { field.origin.isSynthetic }
        }
        else -> null
      }
    }

    val owner = scope.scopeOwnerSymbol.owner
    if (owner is IrFunction && owner.isDelegated()) return "${owner.name.asString()}(...)"

    val declarationParent = parent as? IrDeclaration
    val sourceView = declarationParent?.let(::sourceViewFor)
    val (startOffset, endOffset) = typeOperatorCall.extents()
    return if (sourceView?.validSourcePosition(startOffset, endOffset) == true) {
      sourceView.subSequence(startOffset, endOffset).toString()
    } else {
      // Fallback for inconsistent line numbers
      (declarationParent as? IrDeclarationWithName)?.name?.asString() ?: "Unknown Declaration"
    }
  }

  private fun String.trimForRuntimeAssertion() = StringUtil.trimMiddle(this, 50)

  private fun IrFunction.isDelegated() =
    origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR ||
      origin == IrDeclarationOrigin.DELEGATED_MEMBER

  private fun CharSequence.validSourcePosition(startOffset: Int, endOffset: Int): Boolean =
    startOffset in 0 until endOffset && endOffset < length

  private fun IrElement.extents(): Pair<Int, Int> {
    var startOffset = Int.MAX_VALUE
    var endOffset = 0
    acceptVoid(
      object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
          element.acceptChildrenVoid(this)
          if (element.startOffset in 0 until startOffset) startOffset = element.startOffset
          if (endOffset < element.endOffset) endOffset = element.endOffset
        }
      }
    )
    return startOffset to endOffset
  }

  private fun sourceViewFor(declaration: IrDeclaration): CharSequence? =
    declaration.fileParent.getKtFile()?.viewProvider?.contents

  private val throwTypeCastException: IrSimpleFunctionSymbol =
    backendContext.ir.symbols.throwTypeCastException

  private val checkExpressionValueIsNotNull: IrSimpleFunctionSymbol =
    if (backendContext.config.unifiedNullChecks)
      backendContext.ir.symbols.checkNotNullExpressionValue
    else backendContext.ir.symbols.checkExpressionValueIsNotNull
}
