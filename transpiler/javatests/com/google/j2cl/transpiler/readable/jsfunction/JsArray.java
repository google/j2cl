package com.google.j2cl.transpiler.readable.jsfunction;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public class JsArray {
  @JsOverlay
  public final void sort() {
    sort(
        new CompareFunction() {
          @Override
          public int apply(Object o1, Object o2) {
            return 0;
          }
        });
  }

  @JsFunction
  interface CompareFunction {
    int apply(Object o1, Object o2);
  }

  private native void sort(CompareFunction func);
}
