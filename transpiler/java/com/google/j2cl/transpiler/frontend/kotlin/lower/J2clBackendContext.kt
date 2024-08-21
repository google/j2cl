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
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")

package com.google.j2cl.transpiler.frontend.kotlin.lower

import com.google.j2cl.transpiler.frontend.kotlin.ir.IntrinsicMethods
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsIgnore
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsProperty
import com.google.j2cl.transpiler.frontend.kotlin.ir.isJsType
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.jvm.CachedFieldsForObjectInstances
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.filterOutAnnotations
import org.jetbrains.kotlin.ir.util.isNonCompanionObject
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

/** Context used in the lowering passes and wrapping the `JvmBackendContext`. */
class J2clBackendContext(
  val jvmBackendContext: JvmBackendContext,
  val intrinsics: IntrinsicMethods,
) : CommonBackendContext by jvmBackendContext {
  override val preferJavaLikeCounterLoop: Boolean
    get() = false

  val cachedDeclarations = J2ClCachedDeclarations(this)

  fun createJvmIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
  ) = jvmBackendContext.createJvmIrBuilder(symbol, startOffset, endOffset)
}

/** J2CL specific re-implementation of `org.jetbrains.kotlin.backend.jvm.JvmCachedDeclarations`. */
class J2ClCachedDeclarations(private val context: J2clBackendContext) {
  private val jvmCachedDeclarations = context.jvmBackendContext.cachedDeclarations
  private val staticBackingFields = HashMap<IrProperty, IrField>()

  val fieldsForObjectInstances: CachedFieldsForObjectInstances
    get() = jvmCachedDeclarations.fieldsForObjectInstances

  /**
   * This function is copied and modified from
   * org/jetbrains/kotlin/backend/jvm/JvmCachedDeclarations#getStaticBackingField. Due to JVM
   * restrictions on private interface members, the original implementation does not create a static
   * backing field for interfaces properties if they are not all annotated with `JvmField`
   * annotations. In the J2CL context, we do not have that constraint around fields on interface, so
   * we create a static backing field for all interface properties whatever they are annotated or
   * not.
   */
  @Suppress("KotlincFE10G3")
  fun getStaticBackingField(irProperty: IrProperty): IrField? {
    if (irProperty.isFakeOverride) return null
    val oldField = irProperty.backingField ?: return null
    val oldParent = irProperty.parent as? IrClass ?: return null
    if (!oldParent.isObject) return null
    // MODIFIED BY GOOGLE
    // Call the original method to handle non companion object or companion on classes. So we store
    // the field in the `jvmBackendContext` as before.
    // Passes lowering value classes may call this method to get the static field.
    if (oldParent.isNonCompanionObject || !oldParent.parentAsClass.isJvmInterface) {
      return jvmCachedDeclarations.getStaticBackingField(irProperty)
    }
    // END OF MODIFICATION.

    return staticBackingFields.getOrPut(irProperty) {
      context.irFactory
        .buildField {
          updateFrom(oldField)
          name = oldField.name
          isStatic = true
        }
        .apply {
          // MODIFIED BY GOOGLE
          // The only case we process here is moving fields from the companion to the
          // enclosing interface. We simplify the original code accordingly.
          // Original code:
          // // We don't move fields to interfaces unless all fields are annotated with @JvmField.
          // // It is an error to annotate only some of the fields of an interface companion with
          // // @JvmField, so checking the current field only should be enough.
          // val hasJvmField = oldField.hasAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
          // val shouldMoveFields = oldParent.isCompanion &&
          // (!oldParent.parentAsClass.isJvmInterface || hasJvmField)
          // if (shouldMoveFields) {
          //    parent = oldParent.parentAsClass
          //    val isPrivate = DescriptorVisibilities.isPrivate(oldField.visibility)
          //    val parentIsPrivate = DescriptorVisibilities.isPrivate(oldParent.visibility)
          //    annotations = if (parentIsPrivate && !isPrivate) {
          //        jvmBackendContext.createJvmIrBuilder(this.symbol).run {
          //            filterOutAnnotations(
          //                DeprecationResolver.JAVA_DEPRECATED,
          //                oldField.annotations
          //            ) + irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag)
          //        }
          //    } else {
          //        oldField.annotations
          //    }
          // } else {
          //     parent = oldParent
          //     annotations = oldField.annotations
          // }
          parent = oldParent.parentAsClass
          val isPrivate = DescriptorVisibilities.isPrivate(oldField.visibility)
          val parentIsPrivate = DescriptorVisibilities.isPrivate(oldParent.visibility)
          annotations =
            if (parentIsPrivate && !isPrivate) {
              context.createJvmIrBuilder(this.symbol).run {
                filterOutAnnotations(DeprecationResolver.JAVA_DEPRECATED, oldField.annotations) +
                  irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag)
              }
            } else {
              oldField.annotations
            }
          // Set the visibility of the backing field to public if the underlining property is a
          // public JsProperty.
          // This is a workaround for a Kotlin limitation where @JvmField cannot be used on
          // companion properties if any of the properties is const or var.
          // See: https://youtrack.jetbrains.com/issue/KT-64878
          // TODO(b/319293520): Remove this when Kotlin lifts this restriction.
          if (
            irProperty.isJsProperty ||
              (oldParent.parentAsClass.isJsType &&
                irProperty.visibility == DescriptorVisibilities.PUBLIC &&
                !irProperty.isJsIgnore)
          ) {
            visibility = DescriptorVisibilities.PUBLIC
          }
          // END OF MODIFICATIONS.

          initializer = oldField.initializer?.patchDeclarationParents(this)
          oldField.replaceThisByStaticReference(
            fieldsForObjectInstances,
            oldParent,
            oldParent.thisReceiver!!,
          )
          origin =
            if (irProperty.parentAsClass.isCompanion)
              JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD
            else origin
        }
    }
  }
}
