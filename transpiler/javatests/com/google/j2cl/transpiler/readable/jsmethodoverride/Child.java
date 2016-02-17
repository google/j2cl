package com.google.j2cl.transpiler.readable.jsmethodoverride;

import jsinterop.annotations.JsType;

@JsType
public class Child extends Parent {
  @Override
  public void foo() {}
}
