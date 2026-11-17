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
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.isKotlinDataClass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isMethodOfAny
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_RECORD_ANNOTATION_FQ_NAME

/**
 * Removes the synthetic java.lang.Object overrides from data classes.
 *
 * This pass relies on the backend reimplementing the removed methods via the ValueType
 * optimization: `ImplementRecordClasses` for @JvmRecord data classes (runs in both the Closure and
 * Wasm backends) and `OptimizeStandardDataClass` for standard data classes (runs in the Closure
 * backend only, which is fine for now since the Kotlin frontend does not target Wasm).
 *
 * TODO(b/582872366): Unify the optimizability check in this pass and `OptimizeStandardDataClass`.
 */
internal class RemoveDataClassSyntheticOverrides : IrVisitorVoid(), FileLoweringPass {
  override fun lower(irFile: IrFile) = irFile.acceptVoid(this)

  override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

  override fun visitClass(declaration: IrClass) {
    if (
      declaration.isKotlinDataClass &&
        // TODO(b/576822823): Support local data classes (whose synthetic capture fields are added
        // later by ResolveCaptures).
        !declaration.isLocal &&
        // TODO(b/576824526): Support data classes that extend non-Object types.
        (declaration.superClass == null || declaration.hasAnnotation(JVM_RECORD_ANNOTATION_FQ_NAME))
    ) {
      declaration.declarations.removeIf {
        it.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER &&
          it is IrSimpleFunction &&
          it.isMethodOfAny()
      }
    }
    super.visitClass(declaration)
  }
}
