package com.google.j2cl.transpiler.readable.genericnativetype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public class BoundedJsArray<V> {
  private BoundedJsArray(int size) {}

  /**
   * Creates a new native JS array.
   */
  @JsOverlay
  public static <V> BoundedJsArray<V> create() {
    BoundedJsArray<V> array = null;
    return array;
  }

  /**
   * Creates a new native JS array.
   */
  @JsOverlay
  public static <V> BoundedJsArray<V> create(int size) {
    BoundedJsArray<V> array = new BoundedJsArray<>(size);
    return array;
  }
}
