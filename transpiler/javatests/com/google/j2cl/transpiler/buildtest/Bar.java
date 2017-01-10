package com.google.j2cl.transpiler.buildtest;

import jsinterop.annotations.JsType;

@JsType(namespace = "build")
public class Bar {
  public static Foo createFoo() {
    return new Foo();
  }
}
