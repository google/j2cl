package com.google.j2cl.transpiler.integration.casttonativetypevariable;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
  public static class Foo<T extends Foo<T>> {
    @SuppressWarnings({"unchecked", "unused"})
    @JsOverlay
    public final T getSelf() {
      Object o = new Object[] {new Object()};
      T[] ts = (T[]) o; // cast to T[].
      return (T) this;
    }
  }

  public static class SubFoo extends Foo<SubFoo> {}

  public static void main(String... args) {
    SubFoo sf = new SubFoo();
    assert sf.getSelf() == sf;

    testGenericType();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  public static class NativeObject<K, V> {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void testGenericType() {
    Object a = new NativeObject<String, Object>();

    NativeObject e = (NativeObject) a;
    assert e == a;
    NativeObject<String, Object> f = (NativeObject<String, Object>) a;
    assert f == a;
    assert a instanceof NativeObject;

    Object os = new NativeObject[] {e};
    NativeObject[] g = (NativeObject[]) os;
    assert g[0] == e;
    NativeObject<String, Object>[] h = (NativeObject<String, Object>[]) os;
    assert h[0] == e;
    assert os instanceof NativeObject[];
  }
}
