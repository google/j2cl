/*
 * Copyright 2022 Google Inc.
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
package jsdoctypemappings

import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

internal class JsDocTypeMappings {
  @JsFunction
  fun interface NativeFunction {
    fun f()
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array") class NativeType

  @JsType(name = "ExportedTypeNewName") inner class ExportedType internal constructor()

  fun returnComparatorWildcard(): Comparator<*>? {
    return null
  }

  fun returnComparable(): java.lang.Comparable<*>? {
    return null
  }

  fun returnJavaLangString(): String? {
    return null
  }

  fun returnJavaLangObject(): Any? {
    return null
  }

  fun returnJavaLangByte(): Byte? {
    return null
  }

  fun returnJavaLangShort(): Short? {
    return null
  }

  fun returnJavaLangInteger(): Int? {
    return null
  }

  fun returnJavaLangLong(): Long? {
    return null
  }

  fun returnJavaLangFloat(): Float? {
    return null
  }

  fun returnJavaLangDouble(): Double? {
    return null
  }

  fun returnJavaLangCharacter(): Char? {
    return null
  }

  fun returnJavaLangBoolean(): Boolean? {
    return null
  }

  fun returnByte(): Byte {
    return 0
  }

  fun returnShort(): Short {
    return 0
  }

  fun returnInt(): Int {
    return 0
  }

  fun returnLong(): Long {
    return 0L
  }

  fun returnFloat(): Float {
    return 0f
  }

  fun returnDouble(): Double {
    return 0.0
  }

  fun returnChar(): Char {
    return '0'
  }

  fun returnBoolean(): Boolean {
    return false
  }

  fun returnVoid() {}

  fun returnNativeFunction(): NativeFunction? {
    return null
  }

  fun returnNativeType(): NativeType? {
    return null
  }

  fun returnExportedType(): ExportedType? {
    return null
  }

  fun <MethodTypeParameter> returnMethodTypeParameter(): MethodTypeParameter? {
    return null
  }

  @JsType(isNative = true, name = "number", namespace = JsPackage.GLOBAL) interface NativeNumber

  fun returnNativeNumber(): NativeNumber? {
    return null
  }

  @JsType(isNative = true, name = "boolean", namespace = JsPackage.GLOBAL) interface NativeBoolean

  fun returnNativeBoolean(): NativeBoolean? {
    return null
  }

  @JsType(isNative = true, name = "string", namespace = JsPackage.GLOBAL) interface NativeString

  fun returnNativeString(): NativeString? {
    return null
  }

  @JsType(isNative = true, name = "null", namespace = JsPackage.GLOBAL) interface NativeNull

  fun returnNativeNull(): NativeNull? {
    return null
  }

  @JsType(isNative = true, name = "undefined", namespace = JsPackage.GLOBAL)
  interface NativeUndefined

  fun returnNativeUndefined(): NativeUndefined? {
    return null
  }

  @JsType(isNative = true, name = "void", namespace = JsPackage.GLOBAL) interface NativeVoid

  fun returnNativeVoid(): NativeVoid? {
    return null
  }

  @JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL) interface NativeStar

  fun returnStar(): NativeStar? {
    return null
  }

  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL) interface NativeWildcard

  fun returnNativeWildcard(): NativeWildcard? {
    return null
  }
}
