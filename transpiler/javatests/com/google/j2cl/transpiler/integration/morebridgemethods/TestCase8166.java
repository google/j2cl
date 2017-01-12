package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase8166 {
  static interface CI3 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "CI3 get String";
    }
  }

  @JsType
  static interface CI1 extends CI3 {
    @Override
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
