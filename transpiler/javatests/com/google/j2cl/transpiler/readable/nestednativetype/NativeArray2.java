package com.google.j2cl.transpiler.readable.nestednativetype;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public final class NativeArray {
  @JsOverlay
  public final Object a() {
    return new Object() {};
  }
}
