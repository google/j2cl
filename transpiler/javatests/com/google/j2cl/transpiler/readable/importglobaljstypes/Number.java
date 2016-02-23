package com.google.j2cl.transpiler.readable.importglobaljstypes;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Tests explicit import by namespaced JsMethod.
 */
public class Number {
  // import native js "Number" in a java class "Number".
  @JsMethod(name = "isInteger", namespace = "Number")
  public static native boolean fun(double x);

  public static boolean test(double x) {
    return Number.fun(x);
  }

  /**
   * Tests for generic native type.
   */
  @JsType(isNative = true, namespace = GLOBAL, name = "Function")
  private interface NativeFunction<T> {
    T apply(Object thisContext, int[] argsArray);
  }

  @JsProperty(name = "fromCharCode", namespace = "String")
  private static native NativeFunction<String> getFromCharCodeFunction();

  public static String fromCharCode(int[] array) {
    return getFromCharCodeFunction().apply(null, array);
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  public static interface MyLiteralType {}

  public MyLiteralType testJsDocForLiteralType(MyLiteralType a) {
    return a;
  }
}
