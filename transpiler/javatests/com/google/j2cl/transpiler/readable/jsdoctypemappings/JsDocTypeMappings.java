package com.google.j2cl.transpiler.readable.jsdoctypemappings;

import java.util.Comparator;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class JsDocTypeMappings {

  @JsFunction
  private static interface NativeFunction {
    void f();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  private static class NativeType {}

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
}
