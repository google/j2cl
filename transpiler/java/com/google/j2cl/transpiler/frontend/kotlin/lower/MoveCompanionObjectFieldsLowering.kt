/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

/**
 * Move and/or copy companion object fields to static fields of companion's owner.
 *
 * Copied and modified from:
 * org.jetbrains.kotlin.backend.jvm.lower.MoveCompanionObjectFieldsLowering.kt.
 */
// MODIFIED BY GOOGLE
// Removed compiler phase definition and increased visibility of the lowering class itself.
// Use our backend context wrapping the JvmBackendContext.
internal class MoveOrCopyCompanionObjectFieldsLowering(val context: J2clBackendContext) :
  // END OF MODIFICATIONS
  ClassLoweringPass {
  override fun lower(irClass: IrClass) {
    if (irClass.isNonCompanionObject) {
      irClass.handle()
    } else {
      (irClass.declarations.singleOrNull { it is IrClass && it.isCompanion } as IrClass?)?.handle()
    }
  }

  private fun IrClass.handle() {
    val newDeclarations =
      declarations.map {
        when (it) {
          is IrProperty ->
            context.cachedDeclarations.getStaticBackingField(it)?.also { newField ->
              it.backingField = newField
              newField.correspondingPropertySymbol = it.symbol
            }
          else -> null
        }
      }

    val companionParent = if (isCompanion) parentAsClass else null
    // In case a companion contains no fields, move the anonymous initializers to the parent
    // anyway, as otherwise there will probably be no references to the companion class at all
    // and therefore its class initializer will never be invoked.
    val newParent =
      newDeclarations.firstOrNull { it != null }?.parentAsClass ?: companionParent ?: this
    if (newParent === this) {
      declarations.replaceAll {
        // Anonymous initializers must be made static to correctly initialize the new static
        // fields when the class is loaded.
        if (it is IrAnonymousInitializer) makeAnonymousInitializerStatic(it, newParent) else it
      }

      if (companionParent != null) {
        for (declaration in declarations) {
          if (declaration is IrProperty && declaration.isConst && declaration.hasPublicVisibility) {
            copyConstProperty(declaration, companionParent)
          }
        }
      }
    } else {
      // Anonymous initializers must also be moved and their ordering relative to the fields
      // must be preserved, as the fields can have expression initializers themselves.
      for ((i, newField) in newDeclarations.withIndex()) {
        if (newField != null) newParent.declarations += newField
        if (declarations[i] is IrAnonymousInitializer)
          newParent.declarations +=
            makeAnonymousInitializerStatic(declarations[i] as IrAnonymousInitializer, newParent)
      }
      declarations.removeAll { it is IrAnonymousInitializer }
    }
  }

  private val IrProperty.hasPublicVisibility: Boolean
    get() =
      !DescriptorVisibilities.isPrivate(visibility) &&
        visibility != DescriptorVisibilities.PROTECTED

  private fun makeAnonymousInitializerStatic(
    oldInitializer: IrAnonymousInitializer,
    newParent: IrClass,
  ): IrAnonymousInitializer =
    with(oldInitializer) {
      val oldParent = parentAsClass
      val newSymbol = IrAnonymousInitializerSymbolImpl(newParent.symbol)
      factory
        .createAnonymousInitializer(startOffset, endOffset, origin, newSymbol, isStatic = true)
        .apply {
          parent = newParent
          body = this@with.body.patchDeclarationParents(newParent)
          replaceThisByStaticReference(
            context.cachedDeclarations.fieldsForObjectInstances,
            oldParent,
            oldParent.thisReceiver!!,
          )
        }
    }

  private fun copyConstProperty(oldProperty: IrProperty, newParent: IrClass): IrField {
    val oldField = oldProperty.backingField!!
    return newParent
      .addField {
        updateFrom(oldField)
        name = oldField.name
        isStatic = true
      }
      .apply {
        parent = newParent
        correspondingPropertySymbol = oldProperty.symbol
        initializer =
          oldField.initializer?.run {
            context.irFactory.createExpressionBody(
              startOffset,
              endOffset,
              (expression as IrConst<*>).shallowCopy(),
            )
          }
        annotations += oldField.annotations
        if (oldProperty.parentAsClass.visibility == DescriptorVisibilities.PRIVATE) {
          context.createJvmIrBuilder(this.symbol).run {
            annotations =
              filterOutAnnotations(DeprecationResolver.JAVA_DEPRECATED, annotations) +
                irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag)
          }
        }
      }
  }
}

/**
 * Make IrGetField/IrSetField to objects' fields point to the static versions.
 *
 * Copied and modified from:
 * org.jetbrains.kotlin.backend.jvm.lower.MoveCompanionObjectFieldsLowering.kt.
 */
// MODIFIED BY GOOGLE
// Removed compiler phase definition and increased visibility of the lowering class itself.
// Use our backend context wrapping the JvmBackendContext.
internal class RemapObjectFieldAccesses(val context: J2clBackendContext) :
  FileLoweringPass, IrElementTransformerVoid() {
  // END OF MODIFICATIONS
  override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

  private fun IrField.remap(): IrField? =
    correspondingPropertySymbol
      ?.owner
      ?.let(context.cachedDeclarations::getStaticBackingField)
      // MODIFIED BY GOOGLE
      // Maintain a pointer to the original IrProperty. This will be important for resolving
      // annotations that are applied to the property and not the field.
      ?.also {
        if (it.correspondingPropertySymbol == null) {
          it.correspondingPropertySymbol = correspondingPropertySymbol
        }
      }

  // END OF MODIFICATIONS

  override fun visitGetField(expression: IrGetField): IrExpression =
    expression.symbol.owner.remap()?.let {
      with(expression) {
        IrGetFieldImpl(
          startOffset,
          endOffset,
          it.symbol,
          type,
          receiver = null,
          origin,
          superQualifierSymbol,
        )
      }
    } ?: super.visitGetField(expression)

  override fun visitSetField(expression: IrSetField): IrExpression =
    expression.symbol.owner.remap()?.let {
      with(expression) {
        val newValue = value.transform(this@RemapObjectFieldAccesses, null)
        IrSetFieldImpl(
          startOffset,
          endOffset,
          it.symbol,
          receiver = null,
          newValue,
          type,
          origin,
          superQualifierSymbol,
        )
      }
    } ?: super.visitSetField(expression)
}
