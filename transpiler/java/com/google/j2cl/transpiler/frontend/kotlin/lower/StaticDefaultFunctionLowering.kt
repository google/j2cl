/*
 * Copyright 2010-2017 JetBrains s.r.o.
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.getCompleteTypeSubstitutionMap
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.staticDefaultStub
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildTypeParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.util.classIfConstructor
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.remapTypeParameters
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

/**
 * Makes function adapters for default arguments static.
 *
 * Copied and modified from
 * compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/StaticDefaultFunctionLowering.kt
 */
internal class StaticDefaultFunctionLowering(val context: JvmBackendContext) :
  IrElementTransformerVoid(), FileLoweringPass {
  override fun lower(irFile: IrFile) {
    val unused = irFile.accept(this, null)
  }

  override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement =
    super.visitFunction(
      if (
        declaration.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER &&
          declaration.dispatchReceiverParameter != null
      )
        getStaticFunctionWithReceivers(declaration).also { it.body = declaration.moveBodyTo(it) }
      else declaration
    )

  override fun visitReturn(expression: IrReturn): IrExpression {
    val irFunction = (expression.returnTargetSymbol.owner as? IrSimpleFunction)?.staticDefaultStub
    return super.visitReturn(
      if (irFunction != null) {
        with(expression) { IrReturnImpl(startOffset, endOffset, type, irFunction.symbol, value) }
      } else {
        expression
      }
    )
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val callee = expression.symbol.owner
    if (
      callee.origin !== IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
        expression.dispatchReceiver == null
    ) {
      return super.visitCall(expression)
    }

    val newCallee = getStaticFunctionWithReceivers(callee)

    // MODIFIED BY GOOGLE
    // Original code didn't remap the type arguments of the call sites.
    //
    // Original code:
    // val newCall = irCall(expression, newCallee)
    val extractedTypeParameters = extractTypeParameters(callee.parentAsClass)

    // TODO(b/445955020): There are still cases where we're not resolving all type variables
    //  correctly.
    val capturedTypeMapping = expression.getCompleteTypeSubstitutionMap(callee)

    val newCall =
      context.createJvmIrBuilder(callee.symbol, expression).irCall(newCallee.symbol).apply {
        arguments.assignFrom(expression.arguments)
        // Specialize captured type variable using the types from the dispatch receiver.
        for ((index, typeParameter) in extractedTypeParameters.withIndex()) {
          typeArguments[index] =
            capturedTypeMapping[typeParameter.symbol]?.typeOrFail ?: typeParameter.defaultType
        }
        // Specialize remaining type parameters using the ones on the original call.
        for ((index, typeArgument) in expression.typeArguments.withIndex()) {
          typeArguments[extractedTypeParameters.size + index] = typeArgument
        }
      }
    // END OF MODIFICATIONS
    return super.visitCall(newCall)
  }

  private fun getStaticFunctionWithReceivers(function: IrSimpleFunction): IrSimpleFunction =
    function::staticDefaultStub.getOrSetIfNull {
      context.irFactory.createStaticFunctionWithReceivers(
        function.parent,
        function.name,
        function,
        // MODIFIED BY GOOGLE
        // Copy the type parameters from the enclosing context.
        typeParametersFromContext = extractTypeParameters(function.parentAsClass),
        // Lie about the visibility of the bridge so that we can elide clinit calls.
        visibility = DescriptorVisibilities.PRIVATE,
        // END OF MODIFICATIONS
        remapMultiFieldValueClassStructure = context::remapMultiFieldValueClassStructure,
      )
    }
}

// Copied from compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/util/IrUtils.kt
fun IrFactory.createStaticFunctionWithReceivers(
  irParent: IrDeclarationParent,
  name: Name,
  oldFunction: IrFunction,
  dispatchReceiverType: IrType? = oldFunction.dispatchReceiverParameter?.type,
  origin: IrDeclarationOrigin = oldFunction.origin,
  modality: Modality = Modality.FINAL,
  visibility: DescriptorVisibility = oldFunction.visibility,
  isFakeOverride: Boolean = oldFunction.isFakeOverride,
  copyMetadata: Boolean = true,
  typeParametersFromContext: List<IrTypeParameter> = listOf(),
  remapMultiFieldValueClassStructure:
    (IrFunction, IrFunction, Map<IrValueParameter, IrValueParameter>?) -> Unit,
): IrSimpleFunction {
  return createSimpleFunction(
      startOffset = oldFunction.startOffset,
      endOffset = oldFunction.endOffset,
      origin = origin,
      name = name,
      visibility = visibility,
      isInline = oldFunction.isInline,
      isExpect = oldFunction.isExpect,
      returnType = oldFunction.returnType,
      modality = modality,
      symbol = IrSimpleFunctionSymbolImpl(),
      isTailrec = false,
      isSuspend = oldFunction.isSuspend,
      isOperator = oldFunction is IrSimpleFunction && oldFunction.isOperator,
      isInfix = oldFunction is IrSimpleFunction && oldFunction.isInfix,
      isExternal = false,
      containerSource = oldFunction.containerSource,
      isFakeOverride = isFakeOverride,
    )
    .apply {
      parent = irParent

      val newTypeParametersFromContext =
        copyAndRenameConflictingTypeParametersFrom(
          typeParametersFromContext,
          oldFunction.typeParameters,
        )
      val newTypeParametersFromFunction = copyTypeParametersFrom(oldFunction)
      val typeParameterMap =
        (typeParametersFromContext + oldFunction.typeParameters)
          .zip(newTypeParametersFromContext + newTypeParametersFromFunction)
          .toMap()

      fun remap(type: IrType): IrType =
        type.remapTypeParameters(oldFunction, this, typeParameterMap)

      typeParameters.forEach { it.superTypes = it.superTypes.memoryOptimizedMap(::remap) }

      // MODIFIED BY GOOGLE
      // Also remap the return type so that we don't have stale references.
      returnType = remap(returnType)
      // END OF MODIFICATIONS

      annotations = oldFunction.annotations

      parameters =
        oldFunction.parameters.map { oldParam ->
          val name =
            when (oldParam.kind) {
              IrParameterKind.DispatchReceiver -> Name.identifier("\$this")
              IrParameterKind.ExtensionReceiver -> Name.identifier("\$receiver")
              IrParameterKind.Context,
              IrParameterKind.Regular -> oldParam.name
            }
          val origin =
            when (oldParam.kind) {
              IrParameterKind.DispatchReceiver -> IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER
              IrParameterKind.ExtensionReceiver -> IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
              IrParameterKind.Context -> IrDeclarationOrigin.MOVED_CONTEXT_RECEIVER
              IrParameterKind.Regular -> oldParam.origin
            }
          val type =
            if (oldParam.kind == IrParameterKind.DispatchReceiver) {
              remap(dispatchReceiverType!!)
            } else {
              oldParam.type.remapTypeParameters(
                oldFunction.classIfConstructor,
                this.classIfConstructor,
                typeParameterMap,
              )
            }

          oldParam.copyTo(
            this@apply,
            name = name,
            type = type,
            origin = origin,
            kind = IrParameterKind.Regular,
          )
        }

      remapMultiFieldValueClassStructure(oldFunction, this, null)

      if (copyMetadata) metadata = oldFunction.metadata

      copyAttributes(oldFunction)
    }
}

/**
 * Appends the parameters in [contextParameters] to the type parameters of [this] function, renaming
 * those that may clash with a provided collection of [existingParameters] (e.g. type parameters of
 * the function itself, when creating DefaultImpls).
 *
 * @returns List of newly created, possibly renamed, copies of type parameters in order of the
 *   corresponding parameters in [context].
 */
// Copied from compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/util/IrUtils.kt
private fun IrSimpleFunction.copyAndRenameConflictingTypeParametersFrom(
  contextParameters: List<IrTypeParameter>,
  existingParameters: Collection<IrTypeParameter>,
): List<IrTypeParameter> {
  val newParameters = mutableListOf<IrTypeParameter>()

  val existingNames =
    (contextParameters.map { it.name.asString() } + existingParameters.map { it.name.asString() })
      .toMutableSet()

  contextParameters.forEachIndexed { i, contextType ->
    val newName =
      if (existingParameters.any { it.name.asString() == contextType.name.asString() }) {
        val newNamePrefix = contextType.name.asString() + "_I"
        val newName =
          newNamePrefix +
            generateSequence(1) { x -> x + 1 }.first { n -> (newNamePrefix + n) !in existingNames }
        existingNames.add(newName)
        newName
      } else {
        contextType.name.asString()
      }

    newParameters.add(
      buildTypeParameter(this) {
        updateFrom(contextType)
        index = i
        name = Name.identifier(newName)
      }
    )
  }

  val zipped = contextParameters.zip(newParameters)
  val parameterMap = zipped.toMap()
  for ((oldParameter, newParameter) in zipped) {
    newParameter.copySuperTypesFrom(oldParameter, parameterMap)
  }

  typeParameters = typeParameters memoryOptimizedPlus newParameters

  return newParameters
}

// Copied from compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/util/IrUtils.kt
private fun IrTypeParameter.copySuperTypesFrom(
  source: IrTypeParameter,
  srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>,
) {
  val target = this
  val sourceParent = source.parent as IrTypeParametersContainer
  val targetParent = target.parent as IrTypeParametersContainer
  target.superTypes =
    source.superTypes.memoryOptimizedMap {
      it.remapTypeParameters(sourceParent, targetParent, srcToDstParameterMap)
    }
}
