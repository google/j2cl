package com.google.j2cl.transpiler.readable.jstypecastsinstanceof;

import jsinterop.annotations.JsType;

public class CastToNativeType {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsType {}

  public void test() {
    Object a = new NativeJsType();
    NativeJsType b = (NativeJsType) a;
    boolean c = a instanceof NativeJsType;
    NativeJsType d[] = (NativeJsType[]) a;
    c = a instanceof NativeJsType[];
  }
}
