package com.google.j2cl.transpiler.integration.allsimplebridges;

import jsinterop.annotations.JsType;

public class Tester673 {
  @JsType
  static interface I1 {
    default String get(String value) {
      return "I1.get";
    }
  }

  @SuppressWarnings("unchecked")
  static class C1 implements I1 {
    @SuppressWarnings("MissingOverride")
    public String get(Object value) {
      return "C1.get";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C1 s = new C1();
    assert s.get(new Object()).equals("C1.get");
    assert ((I1) s).get("").equals("I1.get");
  }
}
