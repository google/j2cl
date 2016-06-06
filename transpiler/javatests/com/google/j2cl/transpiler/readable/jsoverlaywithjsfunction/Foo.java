package com.google.j2cl.transpiler.readable.jsoverlaywithjsfunction;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = GLOBAL, name = "Object")
public class Foo {
  @JsOverlay
  public final void bar() {
    new Intf() {
      @Override
      public void run() {}
    }.run();
  }
  
  @JsFunction
  private interface Intf {
    void run();
  }
}
