package com.google.j2cl.transpiler.readable.nativejstypes;

import jsinterop.annotations.JsType;

@JsType(namespace = "", isNative = true)
public class Headers {
  public native void append(String name, String value);

  public native String get(String name);
}
