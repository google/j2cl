package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase4787 {
  static interface CI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "CI1 get String";
    }
  }

  @JsType
  @SuppressWarnings("unchecked")
  static class C<C1> implements CI1 {
    @SuppressWarnings("unused")
    public String get(C1 value) {
      return "C get C1";
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assert c.get("").equals("CI1 get String");
    assert ((CI1) c).get("").equals("CI1 get String");
  }
}
