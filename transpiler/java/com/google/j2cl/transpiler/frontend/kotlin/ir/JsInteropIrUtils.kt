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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/** Superset of attributes that can be extracted from JsInterop annotations */
data class JsAnnotationInfo(
  val name: String? = null,
  val namespace: String? = null,
  val isNative: Boolean,
  val hasCustomValue: Boolean,
)

fun IrDeclaration.getJsMemberAnnotationInfo(): JsAnnotationInfo? {
  val eligibleAnnotations =
    when (this) {
      is IrConstructor -> sequenceOf(JS_CONSTRUCTOR_ANNOTATION_FQ_NAME)
      is IrSimpleFunction ->
        sequenceOf(JS_METHOD_ANNOTATION_FQ_NAME, JS_PROPERTY_ANNOTATION_FQ_NAME)
      is IrField,
      is IrEnumEntry -> sequenceOf(JS_PROPERTY_ANNOTATION_FQ_NAME)
      is IrClass ->
        sequenceOf(
          JS_TYPE_ANNOTATION_FQ_NAME,
          JS_ENUM_ANNOTATION_FQ_NAME,
          JS_FUNCTION_ANNOTATION_FQ_NAME,
        )
      else -> emptySequence()
    }
  return eligibleAnnotations.firstNotNullOfOrNull { findJsMemberAnnotationInfo(it) }
}

private fun IrDeclaration.findJsMemberAnnotationInfo(name: FqName): JsAnnotationInfo? =
  findJsinteropAnnotation(name)?.jsAnnotationInfo

private fun IrDeclaration.findJsinteropAnnotation(name: FqName): IrConstructorCall? {
  val annotation = getAnnotation(name)
  if (annotation != null) return annotation
  return when {
    // look on the property if this is a property getter or setter
    this is IrSimpleFunction -> correspondingPropertySymbol?.owner?.getAnnotation(name)
    this is IrField &&
      (isJvmField ||
        isCompanionConstantBackingField ||
        origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) ->
      // look on the property if this is a backing field, but only if it's a JvmField or constant.
      // No other fields participate in JsInterop.
      correspondingPropertySymbol?.owner?.getAnnotation(name)
    else -> null
  }
}

private val IrConstructorCall.jsAnnotationInfo: JsAnnotationInfo
  get() =
    JsAnnotationInfo(
      getValueArgumentAsConst(Name.identifier("name"), IrConstKind.String),
      getValueArgumentAsConst(Name.identifier("namespace"), IrConstKind.String),
      (getValueArgumentAsConst(Name.identifier("isNative"), IrConstKind.Boolean) ?: false),
      (getValueArgumentAsConst(Name.identifier("hasCustomValue"), IrConstKind.Boolean) ?: false),
    )

fun IrClass.getJsEnumInfo(): JsEnumInfo? {
  val jsEnumAnnotationInfo =
    findJsinteropAnnotation(JS_ENUM_ANNOTATION_FQ_NAME)?.jsAnnotationInfo ?: return null
  return JsEnumInfo.newBuilder().run {
    val hasCustomValue = jsEnumAnnotationInfo.hasCustomValue
    val isNative = jsEnumAnnotationInfo.isNative

    setHasCustomValue(hasCustomValue)
    setSupportsComparable(!hasCustomValue || isNative)
    setSupportsOrdinal(!hasCustomValue && !isNative)
    build()
  }
}

val IrClass.isJsFunction: Boolean
  get() = findJsinteropAnnotation(JS_FUNCTION_ANNOTATION_FQ_NAME) != null

val IrClass.isJsType: Boolean
  get() = findJsinteropAnnotation(JS_TYPE_ANNOTATION_FQ_NAME) != null

private val IrFunction.isJsAsync: Boolean
  get() = findJsinteropAnnotation(JS_ASYNC_ANNOTATION_FQ_NAME) != null

val IrClass.isJsEnum: Boolean
  get() = findJsinteropAnnotation(JS_ENUM_ANNOTATION_FQ_NAME) != null

val IrDeclaration.isJsIgnore: Boolean
  get() =
    findJsinteropAnnotation(JS_IGNORE_ANNOTATION_FQ_NAME) != null ||
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

val IrDeclaration.isJsProperty: Boolean
  get() = findJsinteropAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME) != null

val IrValueParameter.isJsOptional: Boolean
  get() =
    findJsinteropAnnotation(JS_OPTIONAL_ANNOTATION_FQ_NAME) != null &&
      (parent as? IrDeclaration)?.isCompanionMember == false

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
      else -> findJsinteropAnnotation(JS_OVERLAY_ANNOTATION_FQ_NAME) != null
    }

fun IrDeclaration.getJsInfo(): JsInfo =
  // TODO(b/225908831): Handle JsAsync
  JsInfo.newBuilder()
    .setJsMemberType(getJsMemberType())
    .setJsOverlay(isJsOverlay)
    .apply {
      if (isJsMember()) {
        getJsMemberAnnotationInfo()?.let {
          setJsName(it.name)
          setJsNamespace(it.namespace)
        }
      }
    }
    .setHasJsMemberAnnotation(getJsMemberAnnotationInfo() != null)
    .build()

private fun IrDeclaration.isJsMember(): Boolean =
  when {
    isJsIgnore -> false
    isCompanionMember -> false
    getJsMemberAnnotationInfo() != null -> true
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
    isJsProperty ->
      when {
        valueParameters.size == 1 && returnType.isUnit() -> JsMemberType.SETTER
        valueParameters.isEmpty() && !returnType.isUnit() -> JsMemberType.GETTER
        else -> JsMemberType.UNDEFINED_ACCESSOR
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

private fun IrDeclaration.isMemberOfNativeJsType(): Boolean =
  parentClassOrNull?.getJsMemberAnnotationInfo()?.isNative ?: false

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
    else -> throw AssertionError("Unexpected IrDeclaration")
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
