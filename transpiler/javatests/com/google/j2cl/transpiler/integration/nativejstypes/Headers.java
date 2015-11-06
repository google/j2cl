package com.google.j2cl.transpiler.integration.nativejstypes;

import jsinterop.annotations.JsType;

/**
 * Native js types with global namespace.
 */
@JsType(namespace = "", isNative = true)
public class Headers {
  public native void append(String name, String value);

  public native String get(String name);
}
