package com.google.j2cl.transpiler.integration.genericnativetype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public class BoundedJsArray<V> {
  /**
   * Creates a new native JS array.
   */
  @JsOverlay
  public static <V> BoundedJsArray<V> create(int bounds) {
    return null;
  }

  /**
   * Creates a new native JS array.
   */
  @JsOverlay
  public static <V> BoundedJsArray<V> create(int bounds, int size) {
    return null;
  }
}
