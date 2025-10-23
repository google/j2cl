/*
 * Copyright 2022 Google Inc.
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

package com.google.j2cl.transpiler.frontend.kotlin.ir

import com.google.j2cl.transpiler.ast.JsEnumInfo
import com.google.j2cl.transpiler.ast.JsInfo
import com.google.j2cl.transpiler.ast.JsMemberType
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_ASYNC_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_CONSTRUCTOR_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_ENUM_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_FUNCTION_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_IGNORE_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_METHOD_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OPTIONAL_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OVERLAY_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PROPERTY_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_TYPE_ANNOTATION_NAME
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isFromJava
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private fun IrClass.getJsTypeAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_TYPE_ANNOTATION_FQ_NAME)

private fun IrClass.getJsEnumAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_ENUM_ANNOTATION_FQ_NAME)

private fun IrClass.getJsFunctionAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_FUNCTION_ANNOTATION_FQ_NAME)

private fun IrClass.getJsTypeOrJsEnumAnnotation(): IrConstructorCall? =
  getJsTypeAnnotation() ?: getJsEnumAnnotation()

private fun IrConstructor.getJsConstructorAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_CONSTRUCTOR_ANNOTATION_FQ_NAME)

private fun IrFunction.getJsMethodAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_METHOD_ANNOTATION_FQ_NAME)

private fun IrFunction.getJsPropertyAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME)

private fun IrProperty.getJsPropertyAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME)

private fun IrField.getJsPropertyAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME)

private fun IrEnumEntry.getJsPropertyAnnotation(): IrConstructorCall? =
  getJsInteropAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME)

private fun IrClass.getJsInteropAnnotation(name: FqName): IrConstructorCall? = getAnnotation(name)

private fun IrDeclaration.getJsInteropAnnotation(name: FqName): IrConstructorCall? {
  val annotation = getAnnotation(name)
  if (annotation != null) return annotation
  return when {
    // look on the property if this is a property getter or setter
    this is IrSimpleFunction -> correspondingPropertySymbol?.owner?.getAnnotation(name)
    this is IrField && canBeJsProperty -> correspondingPropertySymbol?.owner?.getAnnotation(name)
    else -> null
  }
}

/**
 * Whether this field can have `@JsProperty` applied to it.
 *
 * `@JsProperty` is attached to Kotlin properties which in turn consist of a backing field and
 * accessor functions. We can only apply `@JsProperty` to one or the other, but not both. Generally
 * backing fields in Kotlin are never referenced by user code (except in the accessor functions
 * themselves), so `@JsProperty` should be applied to the accessors which are actually referenced.
 *
 * However, there are situations where the backing field should honor the `@JsProperty` annotation:
 * 1. Properties annotated with `@JvmField` as Java-usages will be using the field member.
 * 2. Fields that we see originating from Java
 * 3. The backing field of the companion object const properties
 * 4. Private properties with no explicit accessors. Kotlin will not generate code for these
 *    accessors and instead use the backing field directly.
 */
private val IrField.canBeJsProperty: Boolean
  get() =
    isJvmField ||
      isFromJava() ||
      isCompanionConstantBackingField ||
      correspondingPropertySymbol?.owner?.hasAccessors == false

fun IrClass.getJsEnumInfo(): JsEnumInfo? {
  val annotation = getJsEnumAnnotation() ?: return null
  return JsEnumInfo.newBuilder().run {
    val hasCustomValue =
      annotation.getValueArgumentAsConst<Boolean>(HAS_CUSTOM_VALUE_ANNOTATION_ATTRIBUTE) ?: false
    val isNative =
      annotation.getValueArgumentAsConst<Boolean>(IS_NATIVE_ANNOTATION_ATTRIBUTE) ?: false

    setHasCustomValue(hasCustomValue)
    setSupportsComparable(!hasCustomValue || isNative)
    setSupportsOrdinal(!hasCustomValue && !isNative)
    build()
  }
}

val IrClass.jsName: String?
  get() = getJsTypeOrJsEnumAnnotation()?.getValueArgumentAsConst<String>(NAME_ANNOTATION_ATTRIBUTE)

val IrClass.jsNamespace: String?
  get() =
    getJsTypeOrJsEnumAnnotation()?.getValueArgumentAsConst<String>(NAMESPACE_ANNOTATION_ATTRIBUTE)

val IrClass.isNative: Boolean
  get() =
    getJsTypeOrJsEnumAnnotation()?.getValueArgumentAsConst<Boolean>(IS_NATIVE_ANNOTATION_ATTRIBUTE)
      ?: false

val IrClass.isJsFunction: Boolean
  get() = getJsFunctionAnnotation() != null

val IrClass.isJsType: Boolean
  get() = getJsTypeAnnotation() != null

val IrClass.isJsEnum: Boolean
  get() = getJsEnumAnnotation() != null

val IrDeclaration.isJsIgnore: Boolean
  get() =
    getJsInteropAnnotation(JS_IGNORE_ANNOTATION_FQ_NAME) != null ||
      // Default param function stubs should implicitly be considered as JsIgnore'd as they will
      // otherwise conflict the "real" JS member.
      origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
      // Instance field of the Companion class should be marked as JsIgnore
      isCompanionInstanceField

private val IrDeclaration.isCompanionInstanceField: Boolean
  get() =
    this is IrField &&
      type.getClass()?.isCompanion == true &&
      origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE

val IrProperty.isJsProperty: Boolean
  get() = getJsPropertyAnnotation() != null

val IrFunction.isJsProperty: Boolean
  get() = getJsPropertyAnnotation() != null

val IrValueParameter.isJsOptional: Boolean
  get() =
    getJsInteropAnnotation(JS_OPTIONAL_ANNOTATION_FQ_NAME) != null &&
      (parent as? IrDeclaration)?.isCompanionMember == false

private val IrFunction.isJsAsync: Boolean
  get() = getJsInteropAnnotation(JS_ASYNC_ANNOTATION_FQ_NAME) != null

private val IrDeclaration.isJsOverlay: Boolean
  get() =
    when {
      // TODO(b/301155797): clean that up when a more general solution is implemented.
      // Companion instance field on native JsType and JsFunction need to be marked as JsOverlay as
      // they are not part of the native contract.
      // Backing static field are moved to the enclosing type. For JsFunction, they need to be
      // marked as JsOverlay.
      this is IrField &&
        (isCompanionFieldOfNativeJsTypeOrJsFunction || isStaticBackingFieldOfJsFunction) -> true
      isCompanionMember -> false
      else -> getJsInteropAnnotation(JS_OVERLAY_ANNOTATION_FQ_NAME) != null
    }

private fun IrDeclaration.getJsMemberAnnotation(): IrConstructorCall? =
  when (this) {
    is IrConstructor -> getJsConstructorAnnotation()
    is IrFunction -> getJsMethodAnnotation() ?: getJsPropertyAnnotation()
    is IrProperty -> getJsPropertyAnnotation()
    is IrField -> getJsPropertyAnnotation()
    is IrEnumEntry -> getJsPropertyAnnotation()
    else -> null
  }

fun IrDeclaration.getJsInfo(): JsInfo =
  JsInfo.newBuilder()
    .setJsMemberType(getJsMemberType())
    .setJsOverlay(isJsOverlay)
    .setJsAsync(this is IrFunction && isJsAsync)
    .setHasJsMemberAnnotation(false)
    .apply {
      val jsMemberAnnotation = getJsMemberAnnotation()
      setHasJsMemberAnnotation(jsMemberAnnotation != null)
      if (!isCompanionMember && jsMemberAnnotation != null) {
        setJsName(jsMemberAnnotation.getValueArgumentAsConst(NAME_ANNOTATION_ATTRIBUTE))
        setJsNamespace(jsMemberAnnotation.getValueArgumentAsConst(NAMESPACE_ANNOTATION_ATTRIBUTE))
      }
    }
    .build()

fun IrDeclaration.isJsMember(): Boolean =
  when {
    this is IrVariable -> false
    isJsIgnore -> false
    isCompanionMember -> false
    getJsMemberAnnotation() != null -> true
    isJsEnumEntry() -> true
    isPublicMemberOfJsType() -> !isJsOverlay
    isMemberOfNativeJsType() -> !isMemberOfJsEnum && !isJsOverlay
    else -> false
  }

private fun IrDeclaration.getJsMemberType(): JsMemberType =
  when (this) {
    is IrFunction -> getJsMemberType()
    is IrField,
    is IrEnumEntry -> if (isJsMember()) JsMemberType.PROPERTY else JsMemberType.NONE
    else -> JsMemberType.NONE
  }

private fun IrFunction.getJsMemberType(): JsMemberType =
  when {
    !isJsMember() -> JsMemberType.NONE
    this is IrConstructor -> JsMemberType.CONSTRUCTOR
    isGetter -> JsMemberType.GETTER
    isSetter -> JsMemberType.SETTER
    isJsProperty -> {
      val valueParameters = parameters.filter { it.kind == IrParameterKind.Regular }
      when {
        valueParameters.size == 1 && returnType.isUnit() -> JsMemberType.SETTER
        valueParameters.isEmpty() && !returnType.isUnit() -> JsMemberType.GETTER
        else -> JsMemberType.UNDEFINED_ACCESSOR
      }
    }
    else -> JsMemberType.METHOD
  }

private val IrDeclaration.isMemberOfJsType: Boolean
  get() = parentClassOrNull?.isJsType ?: false

private val IrDeclaration.isMemberOfJsEnum: Boolean
  get() = parentClassOrNull?.isJsEnum ?: false

private val IrDeclaration.isMemberOfJsFunction: Boolean
  get() = parentClassOrNull?.isJsFunction ?: false

private val IrField.isStaticBackingFieldOfJsFunction: Boolean
  get() = isCompanionPropertyBackingField && isMemberOfJsFunction

private val IrField.isCompanionConstantBackingField: Boolean
  get() = isCompanionPropertyBackingField && correspondingPropertySymbol?.owner?.isConst == true

private val IrDeclaration.isCompanionPropertyBackingField: Boolean
  get() = origin == JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD

private val IrField.isCompanionFieldOfNativeJsTypeOrJsFunction: Boolean
  get() =
    origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE &&
      (isMemberOfNativeJsType() || isMemberOfJsFunction)

private fun IrDeclaration.isMemberOfNativeJsType(): Boolean = parentClassOrNull?.isNative ?: false

val IrField.isNativeJsField: Boolean
  get() = isMemberOfNativeJsType() && !isJsOverlay

private fun IrDeclaration.isJsEnumEntry(): Boolean {
  return this is IrEnumEntry && parentClassOrNull?.isJsEnum == true
}

private fun IrDeclaration.isPublicMemberOfJsType(): Boolean {
  if (!isMemberOfJsType) {
    return false
  }
  return when (this) {
    is IrDeclarationWithVisibility -> visibility == DescriptorVisibilities.PUBLIC
    is IrEnumEntry -> true // Enum entries are always public
    else -> false
  }
}

// Computing whether an instance method is a JsMethod requires looking at overridden methods, and
// such computation is done in the J2CL type model. Therefore this method only checks whether a
// static member is a JsMethod.
fun IrFunction.isStaticJsMember() = isStatic && getJsMemberType() != JsMemberType.NONE

private val JS_ASYNC_ANNOTATION_FQ_NAME: FqName = FqName(JS_ASYNC_ANNOTATION_NAME)
private val JS_CONSTRUCTOR_ANNOTATION_FQ_NAME: FqName = FqName(JS_CONSTRUCTOR_ANNOTATION_NAME)
private val JS_ENUM_ANNOTATION_FQ_NAME: FqName = FqName(JS_ENUM_ANNOTATION_NAME)
private val JS_FUNCTION_ANNOTATION_FQ_NAME: FqName = FqName(JS_FUNCTION_ANNOTATION_NAME)
private val JS_TYPE_ANNOTATION_FQ_NAME: FqName = FqName(JS_TYPE_ANNOTATION_NAME)
private val JS_IGNORE_ANNOTATION_FQ_NAME: FqName = FqName(JS_IGNORE_ANNOTATION_NAME)
private val JS_METHOD_ANNOTATION_FQ_NAME: FqName = FqName(JS_METHOD_ANNOTATION_NAME)
private val JS_PROPERTY_ANNOTATION_FQ_NAME: FqName = FqName(JS_PROPERTY_ANNOTATION_NAME)
private val JS_OPTIONAL_ANNOTATION_FQ_NAME: FqName = FqName(JS_OPTIONAL_ANNOTATION_NAME)
private val JS_OVERLAY_ANNOTATION_FQ_NAME: FqName = FqName(JS_OVERLAY_ANNOTATION_NAME)

private val NAME_ANNOTATION_ATTRIBUTE: Name = Name.identifier("name")
private val NAMESPACE_ANNOTATION_ATTRIBUTE: Name = Name.identifier("namespace")
private val IS_NATIVE_ANNOTATION_ATTRIBUTE: Name = Name.identifier("isNative")
private val HAS_CUSTOM_VALUE_ANNOTATION_ATTRIBUTE: Name = Name.identifier("hasCustomValue")
