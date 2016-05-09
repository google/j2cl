package com.google.j2cl.transpiler.readable.packageinfosubpackage;

import jsinterop.annotations.JsType;

public class Foo {

  @JsType(
    isNative = true,
    namespace = "com.google.j2cl.transpiler.readable.packageinfosubpackage.subpackage",
    name = "Bar"
  )
  private static class NativeBar {}

  private NativeBar nativeBar = new NativeBar();
}
