/*
 * Copyright 2017 Google Inc.
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
package jsdoctypemappings;

import java.util.Comparator;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class JsDocTypeMappings {

  @JsFunction
  static interface NativeFunction {
    void f();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  static class NativeType {}

  @JsType(name = "ExportedTypeNewName")
  class ExportedType {}

  Comparator<?> returnComparatorWildcard() {
    return null;
  }

  @SuppressWarnings("rawtypes")
  Comparable returnComparable() {
    return null;
  }

  java.lang.String returnJavaLangString() {
    return null;
  }

  java.lang.Object returnJavaLangObject() {
    return null;
  }

  java.lang.Byte returnJavaLangByte() {
    return null;
  }

  java.lang.Short returnJavaLangShort() {
    return null;
  }

  java.lang.Integer returnJavaLangInteger() {
    return null;
  }

  java.lang.Long returnJavaLangLong() {
    return null;
  }

  java.lang.Float returnJavaLangFloat() {
    return null;
  }

  java.lang.Double returnJavaLangDouble() {
    return null;
  }

  java.lang.Character returnJavaLangCharacter() {
    return null;
  }

  java.lang.Boolean returnJavaLangBoolean() {
    return null;
  }

  byte returnByte() {
    return 0;
  }

  short returnShort() {
    return 0;
  }

  int returnInt() {
    return 0;
  }

  long returnLong() {
    return 0;
  }

  float returnFloat() {
    return 0;
  }

  double returnDouble() {
    return 0;
  }

  char returnChar() {
    return 0;
  }

  boolean returnBoolean() {
    return false;
  }

  void returnVoid() {}

  NativeFunction returnNativeFunction() {
    return null;
  }

  NativeType returnNativeType() {
    return null;
  }

  ExportedType returnExportedType() {
    return null;
  }

  <MethodTypeParameter> MethodTypeParameter returnMethodTypeParameter() {
    return null;
  }

  @JsType(isNative = true, name = "number", namespace = JsPackage.GLOBAL)
  interface NativeNumber {}

  NativeNumber returnNativeNumber() {
    return null;
  }

  @JsType(isNative = true, name = "boolean", namespace = JsPackage.GLOBAL)
  interface NativeBoolean {}

  NativeBoolean returnNativeBoolean() {
    return null;
  }

  @JsType(isNative = true, name = "string", namespace = JsPackage.GLOBAL)
  interface NativeString {}

  NativeString returnNativeString() {
    return null;
  }

  @JsType(isNative = true, name = "null", namespace = JsPackage.GLOBAL)
  interface NativeNull {}

  NativeNull returnNativeNull() {
    return null;
  }

  @JsType(isNative = true, name = "undefined", namespace = JsPackage.GLOBAL)
  interface NativeUndefined {}

  NativeUndefined returnNativeUndefined() {
    return null;
  }

  @JsType(isNative = true, name = "void", namespace = JsPackage.GLOBAL)
  interface NativeVoid {}

  NativeVoid returnNativeVoid() {
    return null;
  }

  @JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL)
  interface NativeStar {}

  NativeStar returnStar() {
    return null;
  }

  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
  interface NativeWildcard {}

  NativeWildcard returnNativeWildcard() {
    return null;
  }
}
