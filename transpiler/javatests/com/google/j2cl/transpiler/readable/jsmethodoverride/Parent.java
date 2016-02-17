package com.google.j2cl.transpiler.readable.jsmethodoverride;

import jsinterop.annotations.JsMethod;

public class Parent {
  @JsMethod(name = "bar")
  public void foo() {
  }
}
