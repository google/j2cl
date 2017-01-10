package com.google.j2cl.transpiler.buildtest;

import jsinterop.annotations.JsType;

@JsType(namespace = "build")
public class Foo {
  public static Bar createBar() {
    return new Bar();
  }
}
