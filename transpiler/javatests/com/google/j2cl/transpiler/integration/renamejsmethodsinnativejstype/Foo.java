package com.google.j2cl.transpiler.integration.renamejsmethodsinnativejstype;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * Native JsType with "namespace" and "name".
 */
@JsType(namespace = "com.acme", name = "MyFoo", isNative = true)
public class Foo {
  public int x;
  public int y;

  @JsMethod(name = "mysum")
  public native int sum();
}
