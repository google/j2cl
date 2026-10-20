/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.selectSAMOverriddenFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Rewrites the SAM conversions of property references into references to their getters.
 *
 * `UpgradeCallableReferences` replaces the SAM conversions of lambdas and function references with
 * rich function references implementing the `fun interface`, but leaves the SAM conversions of
 * property references as `IrTypeOperatorCall(SAM_CONVERSION)`, which J2CL does not lower.
 *
 * TODO(b/575822395): The resulting wrapper wraps the getter instead of the property, so it can't be
 *   equal to the wrapper of the same property reference held in a variable.
 */
internal class SamConversionOfPropertyReferenceLowering : FileLoweringPass {
  override fun lower(irFile: IrFile) {
    irFile.transformChildrenVoid(
      object : IrElementTransformerVoid() {
        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
          expression.transformChildrenVoid(this)

          val propertyReference = expression.argument
          if (
            expression.operator != IrTypeOperator.SAM_CONVERSION ||
              propertyReference !is IrRichPropertyReference
          ) {
            return expression
          }

          return IrRichFunctionReferenceImpl(
              startOffset = expression.startOffset,
              endOffset = expression.endOffset,
              type = expression.typeOperand,
              reflectionTargetSymbol =
                (propertyReference.reflectionTargetSymbol as? IrPropertySymbol)
                  ?.owner
                  ?.getter
                  ?.symbol,
              overriddenFunctionSymbol =
                expression.typeOperand.classOrFail.owner.selectSAMOverriddenFunction().symbol,
              invokeFunction = propertyReference.getterFunction,
              origin = propertyReference.origin,
            )
            .apply { boundValues += propertyReference.boundValues }
        }
      }
    )
  }
}
