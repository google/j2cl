package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase6 {
  static interface BI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "BI1 get String";
    }
  }

  static class B<B1> implements BI1 {
    @SuppressWarnings("unused")
    public String get(B1 value) {
      return "B get B1";
    }
  }

  @JsType
  @SuppressWarnings("unchecked")
  static class C extends B<String> {
    @Override
    public String get(String value) {
      return "C get String";
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("C get String");
    assert c.get("").equals("C get String");
    assert ((BI1) c).get("").equals("C get String");
  }
}
