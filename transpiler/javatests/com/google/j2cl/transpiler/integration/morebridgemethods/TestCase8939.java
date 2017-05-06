package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase8939 {
  static interface BI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "BI1 get String";
    }
  }

  static class B<B1> implements BI1 {
    B() {}

    @SuppressWarnings("unused")
    public String get(B1 value) {
      return "B get B1";
    }
  }

  @JsType
  static interface CI1 {
    String get(String value);
  }

  static class C extends B<String> implements CI1 {
    C() {}
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("B get B1");
    assert c.get("").equals("B get B1");
    assert ((BI1) c).get("").equals("B get B1");
  }
}
