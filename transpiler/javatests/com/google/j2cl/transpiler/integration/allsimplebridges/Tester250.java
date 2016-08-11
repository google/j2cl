package com.google.j2cl.transpiler.integration.allsimplebridges;

import jsinterop.annotations.JsType;

public class Tester250 {
  static interface I1 {
    default String get(String value) {
      return "I1.get";
    }
  }

  @JsType
  static class C1 {
    public String get(Object value) {
      return "C1.get";
    }
  }

  @SuppressWarnings("unchecked")
  static class C2 extends C1 implements I1 {}

  @SuppressWarnings("unchecked")
  public static void test() {
    C2 s = new C2();
    assert ((C1) s).get("").equals("C1.get");
    assert ((I1) s).get("").equals("I1.get");
  }
}
