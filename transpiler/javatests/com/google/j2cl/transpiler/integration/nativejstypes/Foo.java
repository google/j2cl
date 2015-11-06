package com.google.j2cl.transpiler.integration.nativejstypes;

import jsinterop.annotations.JsType;

/**
 * Native JsType with "namespace" and "name".
 */
@JsType(namespace = "com.acme", name = "MyFoo", isNative = true)
public class Foo {
  public int x;
  public int y;

  public native int sum();
}
