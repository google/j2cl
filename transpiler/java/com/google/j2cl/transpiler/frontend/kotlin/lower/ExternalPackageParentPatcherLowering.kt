/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.classNameOverride
import org.jetbrains.kotlin.backend.jvm.createJvmFileFacadeClass
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getAnnotationStringValue
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/**
 * Replaces parent from package fragment to FileKt class for top-level callables (K2 only).
 *
 * Copied and modified from
 * compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/ExternalPackageParentPatcherLowering.kt
 */
internal class ExternalPackageParentPatcherLowering(val context: JvmBackendContext) :
  FileLoweringPass {
  override fun lower(irFile: IrFile) {
    if (context.config.useFir) {
      irFile.acceptVoid(Visitor())
    }
  }

  private inner class Visitor : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
      element.acceptChildrenVoid(this)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
      visitElement(expression)
      val callee = expression.symbol.owner as? IrMemberWithContainerSource ?: return
      // MODIFIED BY GOOGLE
      // With klibs, parent of top level declarations is IrFile.
      // Original code:
      // if (callee.parent is IrExternalPackageFragment) {
      if (callee.parent is IrExternalPackageFragment || callee.parent is IrFile) {
        // END OF MODIFICATIONS
        val parentClass = generateOrGetFacadeClass(callee) ?: return
        parentClass.parent = callee.parent
        callee.parent = parentClass
        when (callee) {
          is IrProperty -> handleProperty(callee, parentClass)
          is IrSimpleFunction ->
            callee.correspondingPropertySymbol?.owner?.let { handleProperty(it, parentClass) }
        }
      }
    }

    private fun generateOrGetFacadeClass(declaration: IrMemberWithContainerSource): IrClass? {
      val deserializedSource = declaration.containerSource
      if (deserializedSource is FacadeClassSource) {
        val facadeName = deserializedSource.facadeClassName ?: deserializedSource.className
        return createJvmFileFacadeClass(
            if (deserializedSource.facadeClassName != null) IrDeclarationOrigin.JVM_MULTIFILE_CLASS
            else IrDeclarationOrigin.FILE_CLASS,
            facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
            deserializedSource,
            deserializeIr = { irClass -> deserializeTopLevelClass(irClass) },
          )
          .also {
            it.createThisReceiverParameter()
            it.classNameOverride = facadeName
          }
      }

      // MODIFIED BY GOOGLE
      // Handle klib case.
      val parentFile = declaration.parent as? IrFile
      if (parentFile != null) {
        val hasJvmName = parentFile.hasAnnotation(JvmStandardClassIds.JVM_NAME_CLASS_ID)
        var jvmClassName =
          JvmClassName.byFqNameWithoutInnerClasses(
            if (hasJvmName) {
              parentFile.packageFqName
                .child(
                  Name.identifier(
                    parentFile
                      .getAnnotation(JvmStandardClassIds.JVM_NAME)!!
                      .getAnnotationStringValue()!!
                  )
                )
                .asString()
            } else {
              parentFile.packagePartClassName
            }
          )

        val origin =
          if (parentFile.hasAnnotation(JvmStandardClassIds.JVM_MULTIFILE_CLASS_ID))
            IrDeclarationOrigin.JVM_MULTIFILE_CLASS
          else IrDeclarationOrigin.FILE_CLASS

        return createJvmFileFacadeClass(
            origin,
            jvmClassName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
            SourceElement.NO_SOURCE,
            // No need to deserialize IR as klib contains complete information.
            deserializeIr = { false },
          )
          .also {
            it.createThisReceiverParameter()
            it.classNameOverride = jvmClassName
          }
      }
      // END OF MODIFICATIONS
      return null
    }

    private fun deserializeTopLevelClass(irClass: IrClass): Boolean {
      return context.irDeserializer.deserializeTopLevelClass(
        irClass,
        context.irBuiltIns,
        context.symbolTable,
        context.irProviders,
        context.generatorExtensions,
      )
    }

    private fun handleProperty(property: IrProperty, newParent: IrClass) {
      property.parent = newParent
      property.getter?.parent = newParent
      property.setter?.parent = newParent
      property.backingField?.parent = newParent
    }
  }
}
