package com.google.j2cl.transpiler.readable.jstypecastsinstanceof;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
public class CastToTypeVariable<T extends CastToTypeVariable<T>> {
  @SuppressWarnings({"unused", "unchecked"})
  @JsOverlay
  public final T setField(int index, boolean value) {
    Object o = new Object();
    T[] a = (T[]) o;
    return (T) this;
  }
}
