package com.google.j2cl.transpiler.readable.nativejstypes;

import jsinterop.annotations.JsType;

/**
 * Native JsType without "namespace" or "name".
 */
@JsType(isNative = true)
public class Bar {
  public int x;
  public int y;
  public static int f;

  public Bar(int x, int y) {};

  public native int product();

  @JsType(isNative = true)
  interface Inner {}
}

/** Native JsType with "name". */
@JsType(name = "Zoo", isNative = true)
class Another {
  @JsType(isNative = true)
  interface Inner {}
}
