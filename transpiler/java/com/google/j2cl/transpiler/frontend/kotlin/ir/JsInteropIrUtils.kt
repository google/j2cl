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
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_ENUM_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_FUNCTION_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OPTIONAL_ANNOTATION_NAME
import com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_TYPE_ANNOTATION_NAME
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private fun IrClass.getJsTypeAnnotation(): IrConstructorCall? =
  getAnnotation(JS_TYPE_ANNOTATION_FQ_NAME)

private fun IrClass.getJsEnumAnnotation(): IrConstructorCall? =
  getAnnotation(JS_ENUM_ANNOTATION_FQ_NAME)

private fun IrClass.getJsFunctionAnnotation(): IrConstructorCall? =
  getAnnotation(JS_FUNCTION_ANNOTATION_FQ_NAME)

private fun IrClass.getJsTypeOrJsEnumAnnotation(): IrConstructorCall? =
  getJsTypeAnnotation() ?: getJsEnumAnnotation()

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

val IrValueParameter.isJsOptional: Boolean
  get() = getAnnotation(JS_OPTIONAL_ANNOTATION_FQ_NAME) != null

private val JS_ENUM_ANNOTATION_FQ_NAME: FqName = FqName(JS_ENUM_ANNOTATION_NAME)
private val JS_FUNCTION_ANNOTATION_FQ_NAME: FqName = FqName(JS_FUNCTION_ANNOTATION_NAME)
private val JS_TYPE_ANNOTATION_FQ_NAME: FqName = FqName(JS_TYPE_ANNOTATION_NAME)
private val JS_OPTIONAL_ANNOTATION_FQ_NAME: FqName = FqName(JS_OPTIONAL_ANNOTATION_NAME)

private val NAME_ANNOTATION_ATTRIBUTE: Name = Name.identifier("name")
private val NAMESPACE_ANNOTATION_ATTRIBUTE: Name = Name.identifier("namespace")
private val IS_NATIVE_ANNOTATION_ATTRIBUTE: Name = Name.identifier("isNative")
private val HAS_CUSTOM_VALUE_ANNOTATION_ATTRIBUTE: Name = Name.identifier("hasCustomValue")
