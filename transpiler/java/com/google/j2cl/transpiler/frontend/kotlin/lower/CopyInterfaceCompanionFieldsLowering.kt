/*
 * Copyright 2024 Google Inc.
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

import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsIgnore
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsProperty
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsType
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJvmField
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.isInterface

/**
 * Copies static fields from companion objects onto the enclosing interface if a const property is
 * present.
 *
 * This is a workaround for a Kotlin limitation where @JvmField cannot be used on companion
 * properties if any of the properties is const or var.
 *
 * Note: var fields will _not_ be copied to the enclosing interface as mutations will not be
 * reflected. This shouldn't be an issue for code coming from J2KT as you cannot have a non-final
 * static interface field.
 *
 * TODO(b/319293520): Remove this pass when Kotlin lifts this restriction.
 *
 * See: https://youtrack.jetbrains.com/issue/KT-64878
 */
internal class CopyInterfaceCompanionFieldsLowering(private val context: JvmBackendContext) :
  ClassLoweringPass {

  override fun lower(irClass: IrClass) {
    if (!irClass.isInterface) return

    val companion =
      irClass.declarations.singleOrNull { it is IrClass && it.isCompanion } as? IrClass ?: return
    val companionValFields =
      companion.fields.filter { it.correspondingPropertySymbol != null && it.isNonConstVal }

    // We'll only apply this lowering if there are no @JvmField annotated fields.
    if (companionValFields.any(IrField::isJvmField)) return

    val isJsTypeInterface = irClass.isJsType

    companionValFields
      // Only convert fields that would be JsMembers in the interface. This means their property is
      // either (a) @JsProperty or (b) public and the interface is a @JsType.
      .filter {
        with(it.correspondingPropertySymbol!!.owner) {
          isJsProperty ||
            (isJsTypeInterface && visibility == DescriptorVisibilities.PUBLIC && !isJsIgnore)
        }
      }
      .forEach { copyFieldOnto(it, irClass) }
  }

  private fun copyFieldOnto(originalField: IrField, newParent: IrClass) {
    newParent
      .addField {
        updateFrom(originalField)
        name = originalField.name
        isStatic = true
      }
      .apply {
        parent = newParent
        origin = INTERFACE_COMPANION_COPIED_FIELD
        correspondingPropertySymbol = null
        initializer =
          context.createJvmIrBuilder(symbol).run {
            irExprBody(irGetField(null, originalField, originalField.type))
          }
        annotations = originalField.annotations
        // Assume the visibility of the original corresponding property.
        visibility = originalField.correspondingPropertySymbol!!.owner.visibility
      }
  }
}

private object INTERFACE_COMPANION_COPIED_FIELD :
  IrDeclarationOriginImpl("INTERFACE_COMPANION_COPIED_FIELD")

private val IrField.isNonConstVal: Boolean
  get() = with(correspondingPropertySymbol!!.owner) { !isVar && !isConst }
