/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SimpleTypeRemapper
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.withinScope
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.utils.memoryOptimizedMap

/**
 * Wraps top level inline function to access through them from inline functions (legacy lowering).
 *
 * Copied and modified from
 * org.jetbrains.kotlin.ir.backend.js.lower.inline.LegacySyntheticAccessorLowering.kt
 *
 * TODO(b/449153897): This file has been removed in Koltin 2.2.20. Take a look at the new pass and
 *   try to reuse it.
 */
class SyntheticAccessorLowering(private val context: LoweringContext) : BodyLoweringPass {

  private class CandidatesCollector(val candidates: MutableCollection<IrSimpleFunction>) :
    IrVisitorVoid() {

    private fun IrSimpleFunction.isTopLevelPrivate(): Boolean {
      if (visibility != DescriptorVisibilities.PRIVATE) return false
      return parent is IrFile
    }

    override fun visitElement(element: IrElement) {
      element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
      if (declaration.isInline) {
        super.visitSimpleFunction(declaration)
      }
    }

    override fun visitConstructor(declaration: IrConstructor) {}

    override fun visitProperty(declaration: IrProperty) {}

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {}

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {

      super.visitMemberAccess(expression)

      val callee = expression.symbol.owner as? IrSimpleFunction

      if (callee != null) {
        if (callee.isInline) return

        if (callee.isTopLevelPrivate()) {
          candidates.add(callee)
        }
      }
    }
  }

  private class FunctionCopier(fileHash: String) {
    fun copy(function: IrSimpleFunction): IrSimpleFunction {
      function.acceptVoid(symbolRemapper)
      return function.transform(copier, data = null) as IrSimpleFunction
    }

    private val symbolRemapper =
      object : DeepCopySymbolRemapper() {
        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
          remapSymbol(functions, declaration) { IrSimpleFunctionSymbolImpl() }
          declaration.typeParameters.forEach { it.acceptVoid(this) }
          declaration.nonDispatchParameters.forEach { it.acceptVoid(this) }
        }
      }

    private val typeRemapper = SimpleTypeRemapper(symbolRemapper)

    private val copier =
      object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
          // MODIFIED BY GOOGLE.
          // Use function's sanitized name when building the name of the new function. The name of
          // property's getter and setter in  Kotlin AST are invalid in javascript.
          // Original code:
          // val newName = Name.identifier("${declaration.name.asString()}\$accessor\$$fileHash")
          // TODO(b/228454104): Remove this code and use the original one when we fully sanitize
          //  name
          val newName =
            Name.identifier(
              "${NameUtils.sanitizeAsJavaIdentifier(declaration.name.asStringStripSpecialMarkers())}\$accessor\$$fileHash"
            )
          // END OF MODIFICATION.
          return declaration.factory
            .createSimpleFunction(
              startOffset = declaration.startOffset,
              endOffset = declaration.endOffset,
              origin = declaration.origin,
              name = newName,
              visibility = DescriptorVisibilities.INTERNAL,
              isInline = declaration.isInline,
              isExpect = declaration.isExpect,
              returnType = null,
              modality = declaration.modality,
              symbol = symbolRemapper.getDeclaredSimpleFunction(declaration.symbol),
              isTailrec = declaration.isTailrec,
              isSuspend = declaration.isSuspend,
              isOperator = declaration.isOperator,
              isInfix = declaration.isInfix,
              isExternal = declaration.isExternal,
              containerSource = declaration.containerSource,
              isFakeOverride = declaration.isFakeOverride,
            )
            .apply {
              assert(declaration.overriddenSymbols.isEmpty()) {
                "Top level function overrides nothing"
              }
              transformFunctionChildren(declaration)
            }
        }

        private fun IrSimpleFunction.transformFunctionChildren(declaration: IrSimpleFunction) {
          typeParameters = declaration.typeParameters.memoryOptimizedMap { it.transform() }
          typeRemapper.withinScope(this) {
            assert(declaration.dispatchReceiverParameter == null) {
              "Top level functions do not have dispatch receiver"
            }

            parameters = parameters.memoryOptimizedMap { it.transform() }
            parameters.forEach { it.parent = this }
            returnType = typeRemapper.remapType(declaration.returnType)
            typeParameters.forEach { it.parent = this }
          }
        }
      }
  }

  private fun IrSimpleFunction.createAccessor(fileHash: String): IrSimpleFunction {
    val copier = FunctionCopier(fileHash)
    val newFunction = copier.copy(this)

    val irCall =
      IrCallImpl(startOffset, endOffset, newFunction.returnType, symbol, typeParameters.size)

    newFunction.typeParameters.forEachIndexed { i, tp -> irCall.typeArguments[i] = tp.defaultType }

    assert(newFunction.dispatchReceiverParameter == null) {
      "Top level functions do not have dispatch receiver"
    }

    for (p in newFunction.parameters) {
      irCall.arguments[p] = IrGetValueImpl(startOffset, endOffset, p.type, p.symbol)
    }

    val irReturn =
      IrReturnImpl(
        startOffset,
        endOffset,
        context.irBuiltIns.nothingType,
        newFunction.symbol,
        irCall,
      )

    newFunction.body = context.irFactory.createBlockBody(startOffset, endOffset, listOf(irReturn))

    return newFunction
  }

  class CallSiteTransformer(private val functionMap: Map<IrSimpleFunction, IrSimpleFunction>) :
    IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
      expression.transformChildrenVoid()

      val callee = expression.symbol.owner

      functionMap[callee]?.let { newFunction ->
        val newExpression =
          expression.run {
            IrCallImpl(startOffset, endOffset, type, newFunction.symbol, typeArguments.size, origin)
          }

        newExpression.copyTypeArgumentsFrom(expression)
        expression.arguments.forEachIndexed { i, argument -> newExpression.arguments[i] = argument }

        return newExpression
      }

      return expression
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
      expression.transformChildrenVoid()

      val callee = expression.symbol.owner

      functionMap[callee]?.let { newFunction ->
        val newExpression =
          expression.run {
            // TODO: What has to be done with `reflectionTarget`?
            IrFunctionReferenceImpl(
              startOffset,
              endOffset,
              type,
              newFunction.symbol,
              typeArguments.size,
            )
          }

        newExpression.copyTypeArgumentsFrom(expression)
        expression.arguments.forEachIndexed { i, argument -> newExpression.arguments[i] = argument }

        return newExpression
      }

      return expression
    }
  }

  override fun lower(irBody: IrBody, container: IrDeclaration) {}

  override fun lower(irFile: IrFile) {
    val candidates = mutableListOf<IrSimpleFunction>()
    irFile.acceptChildrenVoid(CandidatesCollector(candidates))

    if (candidates.isEmpty()) return

    // MODIFIED BY GOOGLE
    // The suffix used for building the name of the public method was based on the path of the
    // file that is not stable and can change between compilations. We modify the code to
    // compute the suffix based on the package + name of the file.
    // Original code:
    // val fileHash = irFile.fileEntry.name.hashCode().toUInt().toString(Character.MAX_RADIX)
    val fileHash =
      "${irFile.packageFqName}.${irFile.name}".hashCode().toUInt().toString(Character.MAX_RADIX)
    // END OF MODIFICATIONS
    val accessors =
      candidates.map {
        context.irFactory.stageController.restrictTo(it) { it.createAccessor(fileHash) }
      }
    val candidatesMap = candidates.zip(accessors).toMap()

    irFile.transformChildrenVoid(CallSiteTransformer(candidatesMap))

    accessors.forEach { it.parent = irFile }
    irFile.declarations.addAll(accessors)
  }
}
