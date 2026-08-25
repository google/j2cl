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
// TODO(b/550323040): update this file to use the new IrAnnotation node.
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
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.ir.util.isFromJava
import org.jetbrains.kotlin.ir.util.isPropertyAccessor
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME

private fun IrClass.getJsTypeAnnotation(): IrConstructorCall? =
  getAnnotation(JS_TYPE_ANNOTATION_FQ_NAME)

private fun IrClass.getJsEnumAnnotation(): IrConstructorCall? =
  getAnnotation(JS_ENUM_ANNOTATION_FQ_NAME)

private fun IrClass.getJsFunctionAnnotation(): IrConstructorCall? =
  getAnnotation(JS_FUNCTION_ANNOTATION_FQ_NAME)

private fun IrClass.getJsTypeOrJsEnumAnnotation(): IrConstructorCall? =
  getJsTypeAnnotation() ?: getJsEnumAnnotation()

private fun IrConstructor.getJsConstructorAnnotation(): IrConstructorCall? =
  getAnnotation(JS_CONSTRUCTOR_ANNOTATION_FQ_NAME)

private fun IrFunction.getJsMethodAnnotation(): IrConstructorCall? =
  getAnnotation(JS_METHOD_ANNOTATION_FQ_NAME)

private fun IrFunction.getJsPropertyAnnotation(): IrConstructorCall? =
  getAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME)

private fun IrField.getJsPropertyAnnotation(): IrConstructorCall? =
  getAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME)

private fun IrEnumEntry.getJsPropertyAnnotation(): IrConstructorCall? =
  getAnnotation(JS_PROPERTY_ANNOTATION_FQ_NAME)

fun IrClass.getJsEnumInfo(): JsEnumInfo? {
  val annotation = getJsEnumAnnotation() ?: return null
  return JsEnumInfo.builder().run {
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
  get() =
    getJsTypeOrJsEnumAnnotation()?.let {
      // If a name attribute is present on the JsInterop annotation, use that. Otherwise use the
      // unsanitized class name.
      it.getValueArgumentAsConst<String>(NAME_ANNOTATION_ATTRIBUTE) ?: name.asString()
    }

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
  get() = getAnnotation(JS_IGNORE_ANNOTATION_FQ_NAME) != null

val IrFunction.isJsProperty: Boolean
  get() = getJsPropertyAnnotation() != null

val IrValueParameter.isJsOptional: Boolean
  get() = getAnnotation(JS_OPTIONAL_ANNOTATION_FQ_NAME) != null

private val IrFunction.isJsAsync: Boolean
  get() = getAnnotation(JS_ASYNC_ANNOTATION_FQ_NAME) != null

private val IrDeclaration.isJsOverlay: Boolean
  get() = getAnnotation(JS_OVERLAY_ANNOTATION_FQ_NAME) != null

private fun IrDeclaration.getJsMemberAnnotation(): IrConstructorCall? =
  when (this) {
    is IrConstructor -> getJsConstructorAnnotation()
    is IrFunction -> getJsMethodAnnotation() ?: getJsPropertyAnnotation()
    is IrField -> getJsPropertyAnnotation()
    is IrEnumEntry -> getJsPropertyAnnotation()
    else -> null
  }

fun IrDeclaration.getJsInfo(): JsInfo {
  val jsOverlay = isJsOverlay
  val jsAsync = this is IrFunction && isJsAsync
  val jsMemberAnnotation = getJsMemberAnnotation()

  if (!isJsIgnore) {
    val implicitJsMember = isImplicitJsMember()
    val isJsEnumConstant = isJsEnumEntry()
    val memberOfNativeType = isMemberOfNativeJsType() && !isMemberOfJsEnum
    if (
      jsMemberAnnotation != null ||
        ((implicitJsMember || isJsEnumConstant || memberOfNativeType) && !jsOverlay)
    ) {
      return JsInfo.builder()
        .setJsMemberType(getJsMemberType(jsMemberAnnotation))
        .setJsName(jsMemberAnnotation?.getValueArgumentAsConst(NAME_ANNOTATION_ATTRIBUTE))
        .setJsNamespace(jsMemberAnnotation?.getValueArgumentAsConst(NAMESPACE_ANNOTATION_ATTRIBUTE))
        .setJsOverlay(jsOverlay)
        .setJsAsync(jsAsync)
        .setHasJsMemberAnnotation(jsMemberAnnotation != null)
        .build()
    }
  }
  return JsInfo.builder()
    .setJsMemberType(JsMemberType.NONE)
    .setJsOverlay(jsOverlay)
    .setJsAsync(jsAsync)
    .build()
}

private fun IrDeclaration.getJsMemberType(jsMemberAnnotation: IrConstructorCall?): JsMemberType =
  when (this) {
    is IrFunction -> getJsMemberType(jsMemberAnnotation)
    is IrField,
    is IrEnumEntry -> JsMemberType.PROPERTY
    else -> JsMemberType.NONE
  }

private fun IrFunction.getJsMemberType(jsMemberAnnotation: IrConstructorCall?): JsMemberType =
  when {
    this is IrConstructor -> JsMemberType.CONSTRUCTOR
    getJsPropertyAnnotation() != null || (jsMemberAnnotation == null && isPropertyAccessor) -> {
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

private fun IrDeclaration.isMemberOfNativeJsType(): Boolean = parentClassOrNull?.isNative ?: false

private fun IrDeclaration.isJsEnumEntry(): Boolean {
  return this is IrEnumEntry && parentClassOrNull?.isJsEnum == true
}

private fun IrDeclaration.isImplicitJsMember(): Boolean {
  if (!isMemberOfJsType) {
    return false
  }
  return when (this) {
    is IrDeclarationWithVisibility -> canBeImplicitJsMember()
    is IrEnumEntry -> true // Enum entries are always public
    else -> false
  }
}

private fun IrDeclarationWithVisibility.canBeImplicitJsMember(): Boolean {
  if (isSynthetic) {
    return false
  }
  // Public members are implicitly JsMembers.
  if (visibility == DescriptorVisibilities.PUBLIC) {
    // Java component accessors will inherit JsInfo from the components so should not be considered
    // implicit JsMembers even though they are public.
    if (isJavaRecordComponentAccessor()) {
      return false
    }
    return true
  }
  // Java record components fields, although private, are implicit js members. Later in the
  // process, it will be used by public accessors to inherit JsInfo from.
  if (isJavaRecordComponentField()) {
    return true
  }
  return false
}

private fun IrDeclaration.isJavaRecordComponentAccessor(): Boolean =
  this is IrFunction &&
    !isStatic &&
    nonDispatchParameters.isEmpty() &&
    isMemberOfJavaRecord() &&
    // Has matching backing field.
    parentAsClass.declarations.filterIsInstance<IrProperty>().any {
      it.backingField?.isStatic == false && it.name == this.name
    }

private fun IrDeclaration.isJavaRecordComponentField(): Boolean =
  this is IrField && !isStatic && isMemberOfJavaRecord()

private fun IrDeclaration.isMemberOfJavaRecord(): Boolean =
  isFromJava() && parentAsClass.superClass?.hasEqualFqName(JAVA_LANG_RECORD_FQ_NAME) == true

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
