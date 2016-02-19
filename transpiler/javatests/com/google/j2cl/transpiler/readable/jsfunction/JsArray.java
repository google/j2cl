package com.google.j2cl.transpiler.readable.jsfunction;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public class JsArray {
  @JsOverlay
  public final void sort() {
    sort(new MyJsFunctionImpl());
  }

  private native void sort(MyJsFunctionInterface func);
}
