package com.google.j2cl.transpiler.readable.jstypecastsinstanceof;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class CastToNativeType {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsType {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  public static class NativeObject<K, V> {}

  @SuppressWarnings({"unused", "rawtypes", "unchecked"})
  public void test() {
    Object a = new NativeJsType();
    NativeJsType b = (NativeJsType) a;
    boolean c = a instanceof NativeJsType;
    NativeJsType d[] = (NativeJsType[]) a;
    c = a instanceof NativeJsType[];

    NativeObject e = (NativeObject) a;
    NativeObject<String, Object> f = (NativeObject<String, Object>) a;
    c = a instanceof NativeObject;
    NativeObject[] g = (NativeObject[]) a;
    NativeObject<String, Object>[] h = (NativeObject<String, Object>[]) a;
    c = a instanceof NativeObject[];
  }
}
