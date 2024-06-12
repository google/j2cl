/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.filterOutAnnotations
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

/**
 * Adds a static field for object declarations (singletons) to hold the unique instance for the
 * class.
 *
 * Copied and modified from org.jetbrains.kotlin.ir.backend.jvm.lower.ObjectClassLowering.kt
 */
internal class ObjectClassLowering(val context: JvmBackendContext) : ClassLoweringPass {
  private val pendingTransformations = mutableListOf<Function0<Unit>>()

  override fun lower(irFile: IrFile) {
    super.lower(irFile)
    for (transformation in pendingTransformations) {
      transformation.invoke()
    }
  }

  override fun lower(irClass: IrClass) {
    if (!irClass.isObject) return

    val publicInstanceField = context.cachedDeclarations.getFieldForObjectInstance(irClass)
    // MODIFIED BY GOOGLE
    // For interfaces, Kotlin/JVM generates a private static field containing the unique instance of
    // the companion on the companion itself and a public static field on the interface exposing the
    // companion private field. This is not needed for J2CL and removing this construct will help
    // our optimization of companions.
    // Original code:
    // val privateInstanceField =
    //     context.cachedDeclarations.getPrivateFieldForObjectInstance(irClass)
    //
    // val constructor =
    //   irClass.constructors.find { it.isPrimary }
    //     ?: throw AssertionError("Object should have a primary constructor: ${irClass.name}")
    //
    // if (privateInstanceField != publicInstanceField) {
    //   with(context.createIrBuilder(privateInstanceField.symbol)) {
    //     privateInstanceField.initializer = irExprBody(irCall(constructor.symbol))
    //   }
    //   with(context.createIrBuilder(publicInstanceField.symbol)) {
    //     publicInstanceField.initializer = irExprBody(irCall(constructor.symbol))
    //   }
    //   pendingTransformations.add {
    //     (privateInstanceField.parent as IrDeclarationContainer)
    //       .declarations
    //       .add(0, privateInstanceField)
    //   }
    // } else {
    //   with(context.createIrBuilder(publicInstanceField.symbol)) {
    //     publicInstanceField.initializer = irExprBody(irCall(constructor.symbol))
    //   }
    // }

    val constructor =
      irClass.constructors.find { it.isPrimary }
        ?: throw AssertionError("Object should have a primary constructor: ${irClass.name}")

    with(context.createIrBuilder(publicInstanceField.symbol)) {
      publicInstanceField.initializer = irExprBody(irCall(constructor.symbol))
    }
    // END OF MODIFICATIONS

    // Mark object instance field as deprecated if the object visibility is private or protected,
    // and ProperVisibilityForCompanionObjectInstanceField language feature is not enabled.
    if (
      !context.config.languageVersionSettings.supportsFeature(
        LanguageFeature.ProperVisibilityForCompanionObjectInstanceField
      ) &&
        (irClass.visibility == DescriptorVisibilities.PRIVATE ||
          irClass.visibility == DescriptorVisibilities.PROTECTED)
    ) {
      context.createJvmIrBuilder(irClass.symbol).run {
        publicInstanceField.annotations =
          filterOutAnnotations(
            DeprecationResolver.JAVA_DEPRECATED,
            publicInstanceField.annotations,
          ) + irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag)
      }
    }

    pendingTransformations.add {
      // MODIFIED BY GOOGLE
      // Kotlinc does not care about the order of static fields as they are not coming from the
      // source code. All static fields being generated, it can order the fields when it creates the
      // class initialization method. Unlike Koltinc, J2CL uses the order the fields appear in the
      // IrTree for ordering their initialization in the clinit method.
      // For now, only enums will have static fields corresponding to enum entries. We need to
      // ensure that the static field for objects is defined after the enum entries.
      // Original code:
      // (publicInstanceField.parent as IrDeclarationContainer)
      //   .declarations.add(0, publicInstanceField)
      val parentClass = publicInstanceField.parentClassOrNull
      var index = 0
      if (parentClass?.isEnumClass == true) {
        // Find the last enum entry and insert the new field just after it.
        var previousDeclarationWasEnumEntry = false
        parentClass.declarations.withIndex().forEach { (i: Int, declaration: IrDeclaration) ->
          if (previousDeclarationWasEnumEntry && declaration !is IrEnumEntry) {
            index = i
          }
          previousDeclarationWasEnumEntry = declaration is IrEnumEntry
        }
      }

      (publicInstanceField.parent as IrDeclarationContainer)
        .declarations
        .add(index, publicInstanceField)
      // END OF MODIFICATIONS
    }
  }
}
