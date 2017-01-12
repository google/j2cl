package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsMethod;

public class BridgeJsMethodMain {
  public static class A<T> {
    @JsMethod
    public T fun(T t) {
      return t;
    }
  }

  public static class B extends A<String> {
    // It is a JsMethod itself and also overrides a JsMethod.
    @Override
    @JsMethod
    public String fun(String s) {
      return s + "abc";
    }
  }

  public static Object callFunByA(A a, Object o) {
    return a.fun(o);
  }

  public static void test(String... args) {
    try {
      callFunByA(new B(), 1);
      assert false;
    } catch (ClassCastException e) {
      // expected.
    }
  }
}
