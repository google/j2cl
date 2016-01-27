package com.google.j2cl.transpiler.integration.subnativejstype;

import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "test.foo")
public class MyNativeType {
  MyNativeType(int x, int y) {}

  public boolean executed;
  public int x;
  public int y;

  public native int foo(int a);
}
