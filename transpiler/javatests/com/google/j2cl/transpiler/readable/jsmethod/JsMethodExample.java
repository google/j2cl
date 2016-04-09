package com.google.j2cl.transpiler.readable.jsmethod;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "console", namespace = JsPackage.GLOBAL)
public class JsMethodExample {
  public static native void log(String message);

  @JsOverlay
  public static void main(String... args) {
    log("test");
  }
}
