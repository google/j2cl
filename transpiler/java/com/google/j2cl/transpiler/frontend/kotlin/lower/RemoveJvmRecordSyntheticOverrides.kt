/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_RECORD_ANNOTATION_FQ_NAME

/**
 * Removes the synthetic java.lang.Object overrides from @JvmRecord data classes.
 *
 * The regular pipeline for Java record classes is responsible for the implementation of these, in
 * this case via the ValueType optimization.
 */
internal class RemoveJvmRecordSyntheticOverrides : IrVisitorVoid(), FileLoweringPass {
  override fun lower(irFile: IrFile) = irFile.acceptVoid(this)

  override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

  override fun visitClass(declaration: IrClass) {
    if (declaration.isData && declaration.hasAnnotation(JVM_RECORD_ANNOTATION_FQ_NAME)) {
      declaration.declarations.removeIf {
        it.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER &&
          it is IrSimpleFunction &&
          // The only supertypes that are classes are `java.lang.Object` and `java.lang.Record`.
          // The only methods that would override a method in a super class are `equals`, `hashCode`
          // and `toString`.
          it.overriddenSymbols
            .filter { it.owner.parentClassOrNull?.isInterface == false }
            .isNotEmpty()
      }
    }
    super.visitClass(declaration)
  }
}
