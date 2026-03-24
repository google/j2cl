@file:Suppress("CheckReturnValue")

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.getCompleteTypeSubstitutionMap
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.PreSerializationSymbols
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.isConsideredAsPrivateAndNotLocalForInlining
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name.identifier
import org.jetbrains.kotlin.resolve.ContractsDslNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Copied and modified from
 * compiler/ir/ir.inline/src/org/jetbrains/kotlin/ir/inline/FunctionInlining.kt
 */
class FunctionInlining(
  val context: LoweringContext,
  private val inlineFunctionResolver: InlineFunctionResolver,
) : IrTransformer<IrDeclaration>(), BodyLoweringPass {
  override fun lower(irBody: IrBody, container: IrDeclaration) {
    irBody.accept(this, container)
  }

  override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration): IrStatement {
    return when (declaration) {
      is IrFunction,
      is IrClass,
      is IrProperty ->
        context.irFactory.stageController.restrictTo(declaration) {
          super.visitDeclaration(declaration, declaration)
        }
      else -> super.visitDeclaration(declaration, declaration)
    }
  }

  override fun visitFunctionAccess(
    expression: IrFunctionAccessExpression,
    data: IrDeclaration,
  ): IrExpression {
    expression.transformChildren(this, data)

    if (expression is IrCall && PreSerializationSymbols.isTypeOfIntrinsic(expression.symbol)) {
      return expression
    }
    val actualCallee =
      inlineFunctionResolver.getFunctionDeclarationToInline(expression) ?: return expression
    if (actualCallee.body == null) {
      return expression
    }

    // MODIFIED BY GOOGLE.
    // The serialized IR from Kotlin/JVM compilation does not fully support
    // `IrRichCallableReference` nodes. Run `J2clUpgradeCallableReferences` to ensure the inline
    // function body uses the correct node types expected by the inliner.
    J2clUpgradeCallableReferences(context).lower(actualCallee)
    // END OF MODIFICATIONS.

    actualCallee.body?.transformChildren(this, actualCallee)
    actualCallee.parameters.forEachIndexed { index, param ->
      if (expression.arguments[index] == null) {
        // Default values can recursively reference [callee] - transform only needed.
        param.defaultValue = param.defaultValue?.transform(this@FunctionInlining, actualCallee)
      }
    }

    return CallInlining(context, data.file, parent = data as? IrDeclarationParent ?: data.parent)
      .inline(expression, actualCallee)
  }
}

private class CallInlining(
  private val context: LoweringContext,
  private val currentFile: IrFile,
  private val parent: IrDeclarationParent,
) {
  private val parents = (parent as? IrDeclaration)?.parentsWithSelf?.toSet() ?: setOf(parent)

  // Callee can be different from callSite.symbol if resolver returned a non-trivial result.
  // For example, if it is a call of function from another module, callSite.symbol can be equal to
  // lazy function,
  // while callee is a function loaded from klib.
  fun inline(callSite: IrFunctionAccessExpression, callee: IrFunction) =
    inlineFunction(
        callSite = callSite,
        callee = callee,
        inlinedFunctionSymbol =
          ((callee as? IrSimpleFunction)?.originalOfPreparedInlineFunctionCopy ?: callee).symbol,
      )
      .patchDeclarationParents(parent)

  private fun inlineFunction(
    callSite: IrFunctionAccessExpression,
    callee: IrFunction,
    inlinedFunctionSymbol: IrFunctionSymbol?,
  ): IrExpression {
    val copiedCallee = run {

      // MODIFIED BY GOOGLE.
      // Collects all type parameters and their substitutions that could be referenced by the
      // inline function, allowing them to be substituted during inlining. The original code failed
      // to collect type arguments from the receiver of the inline function, which could result in
      // type parameters not being fully substituted and lead to invalid JavaScript code after
      // inlining.
      // Also, substitute type parameters rather than erase them.
      // original code:
      // val allTypeParameters = extractTypeParameters(callee)
      // val notAccessibleTypeParameters = allTypeParameters.filter { it.parent !in parents }
      // val typeArgumentsMap = buildMap {
      //   // Erase type parameters that are not accessible after inlining.
      //   // This includes non-reified parameters of the inline function itself and some type
      //   // parameters of the outer classes.
      //   notAccessibleTypeParameters.filter { !it.isReified }.forEach { put(it.symbol, null) }

      //   // Substitute reified type parameters with concrete type arguments
      //   for ((index, typeArgument) in callSite.typeArguments.withIndex()) {
      //     if (!callee.typeParameters[index].isReified || typeArgument == null) continue
      //     put(callee.typeParameters[index].symbol, typeArgument)
      //   }

      //   // Leave every other parameter as is, they are visible in the inlined scope.
      // }

      // InlineFunctionBodyPreprocessor(typeArgumentsMap).preprocess(callee)
      InlineFunctionBodyPreprocessor(
          callSite.getCompleteTypeSubstitutionMap(callee, context.irBuiltIns)
        )
        .preprocess(callee)
      // END OF MODIFICATIONS.
    }

    val outerIrBuilder =
      context.createIrBuilder(copiedCallee.symbol, callSite.startOffset, callSite.endOffset)

    val parameterToTempVariable = mutableMapOf<IrValueParameterSymbol, IrValueSymbol>()
    val parameterToLambda = mutableMapOf<IrValueParameterSymbol, IrRichCallableReference<*>>()
    // MODIFIED BY GOOGLE.
    // Collects parameters that can be substituted with pure expressions. This avoids generating
    // unused temporary variables, which JSC might not always inline,leading to unnecessary code
    // size increase.
    val parameterToPureExpression = mutableMapOf<IrValueParameterSymbol, IrExpression>()
    // END OF MODIFICATIONS.
    val functionStatements =
      (copiedCallee.body as? IrBlockBody)?.statements
        ?: error("Body not found for function ${callee.render()}")

    val returnType = callSite.type

    return outerIrBuilder.irBlockOrSingleExpression(
      origin = IrStatementOrigin.INLINE_ARGS_CONTAINER
    ) {
      +irReturnableBlock(returnType) {
        val inlinedFunctionBlock =
          irInlinedFunctionBlock(
            inlinedFunctionStartOffset = callee.startOffset,
            inlinedFunctionEndOffset = callee.endOffset,
            resultType = returnType,
            inlinedFunctionSymbol = inlinedFunctionSymbol,
            inlinedFunctionFileEntry = callee.fileEntry,
            origin = null,
          ) {
            evaluateArguments(
              callSiteBuilder = this@irBlockOrSingleExpression,
              inlinedBlockBuilder = this@irInlinedFunctionBlock,
              callSite,
              copiedCallee,
              parameterToTempVariable,
              parameterToLambda,
              // MODIFIED BY GOOGLE.
              // Pass the map to collect parameters that need to be substituted with pure
              // expressions. This avoids generating unused temporary variables, which JSC might not
              // always inline,leading to unnecessary code size increase.
              parameterToPureExpression,
              // END OF MODIFICATIONS.
            )
            +functionStatements
            // Insert a return statement for the function that is supposed to return Unit
            if (callee.returnType.isUnit()) {
              val potentialReturn = functionStatements.lastOrNull() as? IrReturn
              if (potentialReturn == null) {
                at(callee.endOffset, callee.endOffset)
                +irReturn(irGetObject(context.irBuiltIns.unitClass))
              }
            }
          }
        val transformer =
          InlinePostprocessor(
            parameterToTempVariable,
            parameterToLambda,
            // MODIFIED BY GOOGLE.
            // Pass the map to substitute parameters with pure expressions. This avoids generating
            // unused temporary variables, which JSC might not always inline,leading to unnecessary
            // code size increase.
            parameterToPureExpression,
            // END OF MODIFICATIONS.
            returnType,
            copiedCallee.symbol,
            returnableBlockSymbol,
          )
        inlinedFunctionBlock.transformChildrenVoid(transformer)
        +inlinedFunctionBlock
      }
      at(callSite) // block is using offsets at the end, let's restore them just in case
    }
  }

  // ---------------------------------------------------------------------//

  /**
   * The inlining itself just copies function body into call-site. To follow language semantics and
   * IR consistency, the following transformations needed:
   * * Replace references to non-inlineable function parameters with corresponding local variables
   * * Replace returns from function with returns from corresponding returnable block
   * * Replace invoke calls on inlineable function parameters with recursive inlining
   */
  private inner class InlinePostprocessor(
    val parameterToTempVariable: Map<IrValueParameterSymbol, IrValueSymbol>,
    val parameterToLambda: Map<IrValueParameterSymbol, IrRichCallableReference<*>>,
    // MODIFIED BY GOOGLE.
    // Substitute parameters with pure expressions. This avoids generating unused temporary
    // variables, which JSC might not always inline,leading to unnecessary code size increase.
    val parameterToPureExpression: Map<IrValueParameterSymbol, IrExpression>,
    // END OF MODIFICATIONS.
    val returnType: IrType,
    val inlinedFunctionSymbol: IrFunctionSymbol,
    val returnableBlockSymbol: IrReturnableBlockSymbol,
  ) : IrElementTransformerVoid() {
    override fun visitInlinedFunctionBlock(
      inlinedBlock: IrInlinedFunctionBlock
    ): IrInlinedFunctionBlock {
      inlinedBlock.transformChildrenVoid(this)
      if (currentFile.fileEntry == inlinedBlock.inlinedFunctionFileEntry) return inlinedBlock

      val symbol = inlinedBlock.inlinedFunctionSymbol
      if (symbol != null && symbol.isConsideredAsPrivateAndNotLocalForInlining()) {
        inlinedBlock.inlinedFunctionSymbol = null
      }
      return inlinedBlock
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
      expression.transformChildrenVoid(this)
      if (expression.returnTargetSymbol == inlinedFunctionSymbol) {
        expression.returnTargetSymbol = returnableBlockSymbol
        expression.value = expression.value.doImplicitCastIfNeededTo(returnType)
      }
      return expression
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
      val newExpression = super.visitGetValue(expression) as IrGetValue
      parameterToTempVariable[newExpression.symbol]?.let {
        return IrGetValueImpl(
          newExpression.startOffset,
          newExpression.endOffset,
          it,
          newExpression.origin,
        )
      }

      // MODIFIED BY GOOGLE.
      // Substitute parameters with pure expressions. This avoids generating unused temporary
      // variables, which JSC might not always inline,leading to unnecessary code size increase.
      parameterToPureExpression[newExpression.symbol]?.let {
        return it
      }
      // END OF MODIFICATIONS.

      parameterToLambda[newExpression.symbol]?.let {
        val copy = it.deepCopyWithSymbols()
        copy.transformChildrenVoid()
        return copy
      }

      return newExpression
    }

    private fun IrCall.bindInlineReference(inlineReference: IrRichCallableReference<*>): IrCall {
      return IrCallImpl.fromSymbolOwner(
          startOffset,
          endOffset,
          type,
          inlineReference.invokeFunction.symbol,
        )
        .also {
          // For invokeFunction arguments are: (bound values, other arguments)
          // For Function::invoke, call of this is IrCall, arguments are (dispatchReceiver = lambda,
          // other arguments).
          // So, we need to first add bound values, than all arguments of call, except dispatch
          // receiver.
          val arguments =
            inlineReference.boundValues.map { it.deepCopyWithSymbols() } + arguments.drop(1)
          require(arguments.size == it.arguments.size)
          it.arguments.assignFrom(arguments)
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
      // TODO extract to common utils OR reuse ContractDSLRemoverLowering
      if (expression.isContractCall()) {
        return IrCompositeImpl(
          expression.startOffset,
          expression.endOffset,
          context.irBuiltIns.unitType,
        )
      }

      if (!isFunctionInvokeCall(expression)) return super.visitCall(expression)

      // Here `isFunctionInvokeCall` guarantees that `expression` is call of `Function.invoke`.
      // So `expression.arguments.first()` is exactly dispatch receiver and not some other
      // parameter.
      val dispatchReceiver =
        expression.arguments.firstOrNull()?.unwrapAdditionalImplicitCastsIfNeeded() as? IrGetValue
      val parameterSymbolToInline = dispatchReceiver?.symbol ?: return super.visitCall(expression)
      val lambdaToInline =
        parameterToLambda[parameterSymbolToInline] ?: return super.visitCall(expression)
      val callToInline = expression.bindInlineReference(lambdaToInline)

      // Inline the lambda. Lambda parameters will be substituted with lambda arguments.
      val newExpression =
        inlineFunction(
          callSite = callToInline,
          callee = callToInline.symbol.owner,
          inlinedFunctionSymbol = null,
        )

      // Substitute lambda arguments with target function arguments.
      return newExpression.transform(this, null)
    }

    override fun visitElement(element: IrElement) = element.accept(this, null)
  }

  // Contracts can appear only in K1 mode. In K2, they are dropped on the FIR2IR phase.
  private fun IrCall.isContractCall(): Boolean {
    return symbol.isBound &&
      symbol.owner.annotations.any {
        it.symbol.isBound &&
          it.symbol.owner.parentAsClass.hasEqualFqName(
            ContractsDslNames.CONTRACTS_DSL_ANNOTATION_FQN
          )
      }
  }

  private fun IrExpression.doImplicitCastIfNeededTo(type: IrType): IrExpression {
    return when {
      type.isUnit() -> this.coerceToUnit(context.irBuiltIns)
      else -> this.implicitCastIfNeededTo(type)
    }
  }

  // We sometimes insert casts to inline lambda parameters before calling `invoke` on them.
  // Unwrapping these casts helps us satisfy inline lambda call detection logic.
  private fun IrExpression.unwrapAdditionalImplicitCastsIfNeeded(): IrExpression {
    if (this is IrTypeOperatorCall && this.operator == IrTypeOperator.IMPLICIT_CAST) {
      return this.argument.unwrapAdditionalImplicitCastsIfNeeded()
    }
    return this
  }

  private fun isFunctionInvokeCall(irCall: IrCall): Boolean {
    val symbol = irCall.symbol
    if (symbol.isBound) {
      val callee = symbol.owner
      val dispatchReceiver = callee.dispatchReceiverParameter ?: return false
      // Uncomment or delete depending on KT-57249 status
      // assert(!dispatchReceiver.type.isKFunction())

      return (dispatchReceiver.type.isFunctionOrKFunction() ||
        dispatchReceiver.type.isSuspendFunctionOrKFunction()) &&
        callee.name == OperatorNameConventions.INVOKE
    }

    fun IdSignature.isFunctionOrKFunction(): Boolean {
      return with(this as? IdSignature.CommonSignature ?: return false) {
        packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString() &&
          shortName.startsWith("Function") ||
          packageFqName == StandardNames.KOTLIN_REFLECT_FQ_NAME.asString() &&
            shortName.startsWith("KFunction")
      }
    }

    fun IdSignature.isSuspendFunctionOrKFunction(): Boolean {
      return with(this as? IdSignature.CommonSignature ?: return false) {
        packageFqName == StandardNames.COROUTINES_PACKAGE_FQ_NAME.asString() &&
          shortName.startsWith("SuspendFunction") ||
          packageFqName == StandardNames.KOTLIN_REFLECT_FQ_NAME.asString() &&
            shortName.startsWith("KSuspendFunction")
      }
    }

    val signature = symbol.signature?.asPublic() ?: return false
    return signature.shortName == OperatorNameConventions.INVOKE.asString() &&
      with(signature.topLevelSignature()) {
        isFunctionOrKFunction() || isSuspendFunctionOrKFunction()
      }
  }

  // -------------------------------------------------------------------------//

  private fun IrExpression.tryGetLoadedInlineParameter() =
    (this as? IrGetValue)?.symbol?.takeIf {
      it is IrValueParameterSymbol && it.owner.isInlineParameter()
    }

  private fun IrBuilder.emptyVararg(parameter: IrValueParameter): IrVararg {
    return IrVarargImpl(
      startOffset = startOffset,
      endOffset = endOffset,
      type = parameter.type,
      varargElementType = parameter.varargElementType!!,
    )
  }

  private fun evaluateArguments(
    callSiteBuilder: IrStatementsBuilder<*>,
    inlinedBlockBuilder: IrStatementsBuilder<*>,
    callSite: IrFunctionAccessExpression,
    callee: IrFunction,
    parameterToTempVariable: MutableMap<IrValueParameterSymbol, IrValueSymbol>,
    parameterToLambda: MutableMap<IrValueParameterSymbol, IrRichCallableReference<*>>,
    // MODIFIED BY GOOGLE
    // Map containing the parameter of the inline function that can be replaced with pure
    // expressions. This avoids generating unused temporary variables, which JSC might not always
    // inline,leading to unnecessary code size increase.
    parameterToPureExpression: MutableMap<IrValueParameterSymbol, IrExpression>,
    // END OF MODIFICATIONS
  ) {
    for ((parameter, argument) in callee.parameters.zip(callSite.arguments)) {
      val isDefaultArg = argument == null && parameter.defaultValue != null
      val argumentValue =
        when {
          argument != null -> argument
          parameter.defaultValue != null -> parameter.defaultValue!!.expression
          parameter.varargElementType != null -> callSiteBuilder.emptyVararg(parameter)
          else ->
            error(
              "Incomplete expression: call to ${callee.render()} has no argument at index ${parameter.indexInParameters}"
            )
        }.unwrapAdditionalImplicitCastsIfNeeded()
      /*
       * We need to create temporary variable for each argument except inlinable lambda arguments.
       * For simplicity and to produce simpler IR we don't create temporaries for every immutable variable,
       * not only for those referring to inlinable lambdas.
       */
      if (parameter.isInlineParameter() && argumentValue is IrRichCallableReference<*>) {
        val evaluationBuilder = if (isDefaultArg) inlinedBlockBuilder else callSiteBuilder
        // If function reference has bound values, they need to be computed in advance, not at
        // call-site
        // So, we store them to local variables, if they are untrivial
        parameterToLambda[parameter.symbol] = argumentValue
        for (index in argumentValue.boundValues.indices) {
          val irExpression = argumentValue.boundValues[index]
          val boundParameter = argumentValue.invokeFunction.parameters[index]
          val variableSymbol =
            if (
              irExpression is IrGetValue &&
                irExpression.symbol.owner.isImmutable &&
                irExpression.type == boundParameter.type
            ) {
              irExpression.symbol
            } else {
              evaluationBuilder
                .at(irExpression)
                .irTemporary(
                  value = irExpression.doImplicitCastIfNeededTo(boundParameter.type),
                  nameHint =
                    callee.symbol.owner.name.asStringStripSpecialMarkers() +
                      "_${boundParameter.name.asStringStripSpecialMarkers()}",
                  isMutable = false,
                )
                .symbol
            }
          argumentValue.boundValues[index] = irGetValueWithoutLocation(variableSymbol)
        }
        continue
      }

      // inline parameters should never be stored to temporaries, as it would prevent their inlining
      argumentValue.tryGetLoadedInlineParameter()?.let {
        parameterToTempVariable[parameter.symbol] = it
        continue
      }

      val castedArgumentValue = argumentValue.doImplicitCastIfNeededTo(parameter.type)

      // MODIFIED BY GOOGLE
      // Skip creation of temporary variables for arguments that does not have side effects when
      // evaluated. This avoids generating unused temporary variables, which JSC might not always
      // inline,leading to unnecessary code size increase.
      if (argumentValue.isPure(false, symbols = context.symbols as Symbols)) {
        parameterToPureExpression[parameter.symbol] = castedArgumentValue
        continue
      }
      // END OF MODIFICATIONS

      val valueForTmpVar =
        if (isDefaultArg) {
          castedArgumentValue
        } else {
          // This variable is required to trigger argument evaluation outside the scope of the
          // inline function
          val tempVarOutsideInlineBlock =
            callSiteBuilder
              .at(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
              .irTemporary(
                castedArgumentValue,
                // MODIFIED BY GOOGLE
                // Make the output a bit easier to read by hinting the name of tmp variable with the
                // parameter name.
                nameHint = parameter.name.asString(),
                // END OF MODIFICATIONS
              )
          inlinedBlockBuilder
            .at(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            .irGet(tempVarOutsideInlineBlock)
        }

      val tempVarInsideInlineBlock =
        inlinedBlockBuilder
          .irTemporary(
            value = valueForTmpVar,
            origin =
              if (parameter.kind == IrParameterKind.ExtensionReceiver) {
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER
              } else {
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER
              },
          )
          .apply { name = identifier(parameter.name.asStringStripSpecialMarkers()) }

      parameterToTempVariable[parameter.symbol] = tempVarInsideInlineBlock.symbol
    }
  }

  private fun irGetValueWithoutLocation(symbol: IrValueSymbol): IrGetValue {
    return IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, null)
  }
}
