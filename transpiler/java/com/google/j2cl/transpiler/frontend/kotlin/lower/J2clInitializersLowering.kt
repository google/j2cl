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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.InitializersLoweringBase
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Lowers the field initializers and init blocks of classes with a primary constructor into it.
 *
 * Unlike `InitializersLowering`, the initializers are moved rather than copied. Copying a local
 * class declared in an initializer would create a new class, while other declarations, like the
 * type of a property initialized with an anonymous object, still refer to the original one.
 *
 * In classes without a primary constructor, every constructor calling the super constructor runs
 * the field initializers and init blocks. Instead of copying them into each of these constructors,
 * their init blocks, which include their field initializers after [J2clFieldInitializersLowering],
 * are kept and become initializer blocks in the J2CL AST that J2CL implements like for Java.
 */
internal class J2clInitializersLowering(context: CommonBackendContext) :
  InitializersLoweringBase(context), BodyLoweringPass {
  override fun lower(irFile: IrFile) {
    runOnFilePostfix(irFile, true)
  }

  override fun lower(irBody: IrBody, container: IrDeclaration) {
    if (container !is IrConstructor) {
      return
    }

    val irClass = container.parentAsClass
    if (irClass.primaryConstructor == null) {
      // Classes without primary constructor keep their init blocks. The J2CL backend gathers them
      // into an `$init` method and calls it from each constructor not delegating to `this(...)`.
      (irBody as IrBlockBody).statements.removeIf { it is IrInstanceInitializerCall }
      return
    }

    // The secondary constructors delegate to the primary constructor, which is the only one running
    // the initializers.
    if (!container.isPrimary) {
      return
    }

    val initializers =
      IrBlockImpl(
        irClass.startOffset,
        irClass.endOffset,
        context.irBuiltIns.unitType,
        null,
        extractInitializers(irClass) {
          (it is IrField && !it.isStatic) || (it is IrAnonymousInitializer && !it.isStatic)
        },
      )
    initializers.patchDeclarationParents(container)
    irBody.transformChildrenVoid(
      object : IrElementTransformerVoid() {
        override fun visitInstanceInitializerCall(
          expression: IrInstanceInitializerCall
        ): IrExpression = initializers
      }
    )
  }
}
