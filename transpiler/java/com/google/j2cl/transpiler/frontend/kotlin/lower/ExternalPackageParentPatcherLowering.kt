/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.classNameOverride
import org.jetbrains.kotlin.backend.jvm.createJvmFileFacadeClass
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.klibFileAnnotations
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/**
 * Replaces parent from package fragment to FileKt class for top-level callables (K2 only).
 *
 * Copied and modified from
 * compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/ExternalPackageParentPatcherLowering.kt
 */
// MODIFIED BY GOOGLE
// helper function
private fun IrAnnotation.getAnnotationStringValue(): String? =
  (arguments[0] as? IrConst)?.value as String?

// END OF MODIFICATIONS

internal class ExternalPackageParentPatcherLowering(val context: JvmBackendContext) :
  FileLoweringPass {
  override fun lower(irFile: IrFile) {
    irFile.acceptVoid(Visitor())
  }

  private inner class Visitor : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
      element.acceptChildrenVoid(this)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
      visitElement(expression)
      val callee = expression.symbol.owner as? IrMemberWithContainerSource ?: return
      // MODIFIED BY GOOGLE
      // The top-level declarations of KLIB dependencies that exist only in their IR are parented to
      // an IrFile, see `generateOrGetFacadeClass`.
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
      // MODIFIED BY GOOGLE
      // The Kotlin dependencies are KLIBs, never class files, so the container source of their
      // declarations is never a `FacadeClassSource`. Their facade class is computed below instead.
      // Original code:
      // val deserializedSource = declaration.containerSource
      // if (deserializedSource is FacadeClassSource) {
      //   val facadeName = deserializedSource.facadeClassName ?: deserializedSource.className
      //   return createJvmFileFacadeClass(
      //       if (deserializedSource.facadeClassName != null)
      // IrDeclarationOrigin.JVM_MULTIFILE_CLASS
      //       else IrDeclarationOrigin.FILE_CLASS,
      //       facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
      //       deserializedSource,
      //     )
      //     .also {
      //       it.createThisReceiverParameter()
      //       it.classNameOverride = facadeName
      //     }
      // }
      // END OF MODIFICATIONS

      // MODIFIED BY GOOGLE
      // Handle the top-level declarations of KLIB dependencies that exist only in their IR, such as
      // the synthetic accessors of private top-level members leaked through internal inline
      // functions. They are not in the KLIB metadata, so FIR2IR does not materialize them and they
      // have no FIR declaration recording their source file. `JKlibIrOnlyDeclarationDeserializer`
      // deserializes them from the IR of the KLIB instead and, like the IR linker, parents them to
      // their `IrFile`, which is what the facade class is computed from.
      val parentFile = declaration.parent as? IrFile
      if (parentFile != null) {
        val hasJvmName = parentFile.hasAnnotation(JvmStandardClassIds.JVM_NAME_CLASS_ID)
        var jvmClassName =
          JvmClassName.byFqNameWithoutInnerClasses(
            if (hasJvmName) {
              parentFile.packageFqName
                .child(
                  Name.identifier(
                    parentFile.annotations
                      .findAnnotation(JvmStandardClassIds.JVM_NAME)!!
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
          )
          .also {
            it.createThisReceiverParameter()
            it.classNameOverride = jvmClassName
          }
      }

      // Handle the declarations of KLIB dependencies that FIR2IR materializes lazily. Their parent
      // is an `IrExternalPackageFragment`, but the KLIB metadata records their source file and its
      // annotations, which is all that is needed to compute the facade class.
      val klibDeclaration = declaration.klibFirDeclaration()
      val klibSourceFile = klibDeclaration?.klibSourceFile
      if (klibSourceFile != null) {
        val fileAnnotations = klibDeclaration.klibFileAnnotations.orEmpty()
        val session = klibDeclaration.moduleData.session
        val packageFqName = (declaration.parent as IrPackageFragment).packageFqName
        val jvmName =
          fileAnnotations
            .firstOrNull {
              it.toAnnotationClassId(session) == JvmStandardClassIds.JVM_NAME_CLASS_ID
            }
            ?.getStringArgument(Name.identifier("name"))
        val jvmClassName =
          JvmClassName.byFqNameWithoutInnerClasses(
            if (jvmName != null) {
              packageFqName.child(Name.identifier(jvmName))
            } else {
              PackagePartClassUtils.getPackagePartFqName(packageFqName, klibSourceFile.name!!)
            }
          )
        val hasJvmMultifileClassAnnotation = fileAnnotations.any {
          it.toAnnotationClassId(session) == JvmStandardClassIds.JVM_MULTIFILE_CLASS_ID
        }
        val origin =
          if (hasJvmMultifileClassAnnotation) IrDeclarationOrigin.JVM_MULTIFILE_CLASS
          else IrDeclarationOrigin.FILE_CLASS

        return createJvmFileFacadeClass(
            origin,
            jvmClassName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
            SourceElement.NO_SOURCE,
          )
          .also {
            it.createThisReceiverParameter()
            it.classNameOverride = jvmClassName
          }
      }
      // END OF MODIFICATIONS
      return null
    }

    /**
     * The FIR declaration FIR2IR materialized [this] from, if [this] was read from KLIB metadata.
     * Accessors are not recorded in the metadata on their own, so their property is used instead.
     */
    private fun IrDeclaration.klibFirDeclaration(): FirDeclaration? {
      val declaration = (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: this
      return (declaration as? AbstractFir2IrLazyDeclaration<*>)?.fir
    }

    private fun handleProperty(property: IrProperty, newParent: IrClass) {
      property.parent = newParent
      property.getter?.parent = newParent
      property.setter?.parent = newParent
      property.backingField?.parent = newParent
    }
  }
}
