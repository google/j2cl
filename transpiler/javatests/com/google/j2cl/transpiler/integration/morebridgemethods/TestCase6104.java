package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase6104 {
  @JsType
  static class B {
    @SuppressWarnings("unused")
    public String get(String value) {
      return "B get String";
    }
  }

  static interface CI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "CI1 get String";
    }
  }

  static class C extends B implements CI1 {}

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("B get String");
    assert c.get("").equals("B get String");
    assert ((CI1) c).get("").equals("B get String");
  }
}
