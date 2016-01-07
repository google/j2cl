package com.google.j2cl.transpiler.readable.nativejstypeobjectmethods;

import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithToString {
    @Override
    public native String toString();
  }

  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithoutToString {}

  public static void test() {
    NativeJsTypeWithToString n1 = new NativeJsTypeWithToString();
    n1.toString();
    Object n2 = new NativeJsTypeWithToString();
    n2.toString();

    NativeJsTypeWithoutToString n3 = new NativeJsTypeWithoutToString();
    n3.toString();
    Object n4 = new NativeJsTypeWithoutToString();
    n4.toString();
  }
}
