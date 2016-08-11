package com.google.j2cl.transpiler.integration.allsimplebridges;

import jsinterop.annotations.JsType;

public class Tester115 {
  @JsType
  static interface I1 {
    String get(String value);
  }

  @JsType
  static class C1<T> {
    public String get(T value) {
      return "C1.get";
    }
  }

  @SuppressWarnings("unchecked")
  static class C2 extends C1<String> implements I1 {}

  @SuppressWarnings("unchecked")
  public static void test() {
    C2 s = new C2();
    assert ((C1) s).get("").equals("C1.get");
    assert ((I1) s).get("").equals("C1.get");
  }
}
