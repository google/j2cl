package com.google.j2cl.transpiler.readable.subnativejstype;

import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "test.foo")
public class MyNativeJsType {
  public MyNativeJsType(int x) {}
}
