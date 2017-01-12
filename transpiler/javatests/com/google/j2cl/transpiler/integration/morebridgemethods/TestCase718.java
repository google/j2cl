package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase718 {
  @JsType
  static interface CI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "CI1 get String";
    }
  }

  static class C implements CI1 {}

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert c.get("").equals("CI1 get String");
    assert ((CI1) c).get("").equals("CI1 get String");
  }
}
