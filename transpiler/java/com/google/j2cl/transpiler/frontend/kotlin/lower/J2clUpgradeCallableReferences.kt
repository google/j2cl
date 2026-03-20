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

package com.google.j2cl.transpiler.frontend.kotlin.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/** Upgrades property and function reference-like nodes to the new IrRichCallableReference node. */
class J2clUpgradeCallableReferences(context: LoweringContext) :
  UpgradeCallableReferences(
    context,
    upgradeFunctionReferencesAndLambdas = true,
    upgradeSamConversions = true,
    upgradePropertyReferences = true,
    upgradeLocalDelegatedPropertyReferences = true,
  ) {

  override fun lower(irFile: IrFile) {
    super.lower(irFile)

    // TODO(b/489400997): Backport this change to UpgradeCallableReferences under a flag. This can
    // be useful for Kotlin/JS.
    // Rewrite SAM conversions with IrRichPropertyReference to an IrRichFunctionReference to the
    // property getter.
    irFile.transformChildrenVoid(
      object : IrElementTransformerVoid() {
        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
          expression.transformChildrenVoid(this)

          if (
            expression.operator != IrTypeOperator.SAM_CONVERSION ||
              expression.argument !is IrRichPropertyReference
          ) {
            return expression
          }
          val propertyReference = expression.argument as IrRichPropertyReference

          return IrRichFunctionReferenceImpl(
              startOffset = propertyReference.startOffset,
              endOffset = propertyReference.endOffset,
              invokeFunction = propertyReference.getterFunction,
              reflectionTargetSymbol =
                (propertyReference.reflectionTargetSymbol as? IrPropertySymbol)
                  ?.owner
                  ?.getter
                  ?.symbol,
              type = expression.type,
              overriddenFunctionSymbol = selectSAMOverriddenFunction(expression.type),
              origin = propertyReference.origin,
            )
            .apply { boundValues += propertyReference.boundValues }
        }
      }
    )
  }
}
