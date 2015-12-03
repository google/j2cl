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
  @JsType(isNative = true, namespace = GLOBAL, name = "")
  private interface NativeFunction<T> {
    T apply(Object thisContext, Object argsArray);
  }

  @JsProperty(name = "fromCharCode", namespace = "String")
  private static native NativeFunction<String> getFromCharCodeFunction();

  public static String fromCharCode(Object array) {
    return getFromCharCodeFunction().apply(null, array);
  }
}
