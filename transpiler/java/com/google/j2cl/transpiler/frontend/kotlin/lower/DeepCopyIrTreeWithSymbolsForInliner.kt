/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.* // ktlint-disable
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.* // ktlint-disable
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.memoryOptimizedMap

/**
 * Helper class for copying a part of the IR tree.
 *
 * Based on org.jetbrains.kotlin.backend.common.lower.inline.DeepCopyIrTreeWithSymbolsForInliner.kt
 */
internal class DeepCopyIrTreeWithSymbolsForInliner(
  val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
  val parent: IrDeclarationParent?
) {

  fun copy(irElement: IrElement): IrElement {
    // Create new symbols.
    irElement.acceptVoid(symbolRemapper)

    // Make symbol remapper aware of the callsite's type arguments.
    symbolRemapper.typeArguments = typeArguments

    // Copy IR.
    val result = irElement.transform(copier, data = null)

    result.patchDeclarationParents(parent)
    return result
  }

  private inner class InlinerTypeRemapper(
    val symbolRemapper: SymbolRemapper,
    val typeArguments: Map<IrTypeParameterSymbol, IrType?>?
  ) : TypeRemapper {

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

    override fun leaveScope() {}

    private fun remapTypeArguments(
      arguments: List<IrTypeArgument>,
      erasedParameters: MutableSet<IrTypeParameterSymbol>?
    ) =
      arguments.memoryOptimizedMap { argument ->
        (argument as? IrTypeProjection)?.let { proj ->
          remapTypeAndOptionallyErase(proj.type, erasedParameters)?.let { newType ->
            makeTypeProjection(newType, proj.variance)
          }
            ?: IrStarProjectionImpl
        }
          ?: argument
      }

    override fun remapType(type: IrType) = remapTypeAndOptionallyErase(type, erase = false)

    fun remapTypeAndOptionallyErase(type: IrType, erase: Boolean): IrType {
      val erasedParams = if (erase) mutableSetOf<IrTypeParameterSymbol>() else null
      return remapTypeAndOptionallyErase(type, erasedParams)
        ?: error("Cannot substitute type ${type.render()}")
    }

    private fun remapTypeAndOptionallyErase(
      type: IrType,
      erasedParameters: MutableSet<IrTypeParameterSymbol>?
    ): IrType? {
      if (type !is IrSimpleType) return type

      val classifier = type.classifier
      val substitutedType = typeArguments?.get(classifier)

      // Erase non-reified type parameter if asked to.
      if (
        erasedParameters != null &&
          substitutedType != null &&
          (classifier as? IrTypeParameterSymbol)?.owner?.isReified == false
      ) {

        if (classifier in erasedParameters) {
          return null
        }

        erasedParameters.add(classifier)

        // Pick the (necessarily unique) non-interface upper bound if it exists.
        val superTypes = classifier.owner.superTypes
        val superClass = superTypes.firstOrNull { it.classOrNull?.owner?.isInterface == false }

        val upperBound = superClass ?: superTypes.first()

        // TODO: Think about how to reduce complexity from k^N to N^k
        val erasedUpperBound =
          remapTypeAndOptionallyErase(upperBound, erasedParameters)
            ?: error("Cannot erase upperbound ${upperBound.render()}")

        erasedParameters.remove(classifier)

        return erasedUpperBound.mergeNullability(type)
      }

      if (substitutedType is IrDynamicType) return substitutedType

      if (substitutedType is IrSimpleType) {
        return substitutedType.mergeNullability(type)
      }

      // MODIFIED BY GOOGLE
      //
      // Strip any type arguments for types that refer to a local class. The only way to create a
      // local class in an inline function is with an `object {}` definition which cannot define its
      // own type parameters. However, they can implicitly capture type parameters from the
      // enclosing inline function which will be represented in the type references.
      // After inlining occurs these captured types will be resolved and replaced based on the call
      // site's type arguments. There's no longer any type arguments involved and should be dropped
      // when we updated type references.
      val argumentsToRemap =
        if (classifier is IrClassSymbol && classifier.owner.isLocal) {
          check(classifier.owner.typeParameters.isEmpty())
          emptyList()
        } else {
          type.arguments
        }
      // END OF MODIFICATIONS

      return type.buildSimpleType {
        kotlinType = null
        this.classifier = symbolRemapper.getReferencedClassifier(classifier)
        // MODIFIED BY GOOGLE
        // Reference argumentsToRemap instead of type.arguments to honor the modification above.
        arguments = remapTypeArguments(argumentsToRemap, erasedParameters)
        // END OF MODIFICATIONS
        annotations =
          type.annotations.memoryOptimizedMap { it.transform(copier, null) as IrConstructorCall }
      }
    }
  }

  private class SymbolRemapperImpl(descriptorsRemapper: DescriptorsRemapper) :
    DeepCopySymbolRemapper(descriptorsRemapper) {

    var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
      set(value) {
        if (field != null) return
        field =
          value?.asSequence()?.associate {
            (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
          }
      }

    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
      val result = super.getReferencedClassifier(symbol)
      if (result !is IrTypeParameterSymbol) return result
      return typeArguments?.get(result)?.classifierOrNull ?: result
    }
  }

  private val symbolRemapper = SymbolRemapperImpl(NullDescriptorsRemapper)
  // MODIFIED BY GOOGLE
  //
  // The original code only performs type erasure for type operations to ensure correct runtime
  // behavior for non-reified type variables. However, this partial erasure ends up generating
  // inconsistent types which can lead to boxing/unboxing issues later on.
  //
  // TODO(b/274670726): As a workaround we opted to just not perform type erasure, but this isn't
  //   semantically correct for non-reified types. We should revisit this behavior.
  //
  // Original Code:
  // private val typeRemapper = InlinerTypeRemapper(symbolRemapper, typeArguments)
  // private val copier =
  //   object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
  //     private fun IrType.remapTypeAndErase() =
  //       typeRemapper.remapTypeAndOptionallyErase(this, erase = true)

  //     override fun visitTypeOperator(expression: IrTypeOperatorCall) =
  //       IrTypeOperatorCallImpl(
  //           expression.startOffset,
  //           expression.endOffset,
  //           expression.type.remapTypeAndErase(),
  //           expression.operator,
  //           expression.typeOperand.remapTypeAndErase(),
  //           expression.argument.transform()
  //         )
  //         .copyAttributes(expression)
  //   }
  private val copier =
    DeepCopyIrTreeWithSymbols(symbolRemapper, InlinerTypeRemapper(symbolRemapper, typeArguments))
  // END OF MODIFICATIONS

}
