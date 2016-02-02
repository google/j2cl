package com.google.j2cl.transpiler.readable.nativetype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public class MyTypedArray<T> {
  private int length;

  @JsOverlay
  public final int size() {
    return length;
  }

  @JsOverlay
  public final T get(int index) {
    return null;
  }
}
