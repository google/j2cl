package com.google.j2cl.transpiler.integration.morebridgemethods;

public class TestCase10092 {
  static interface BI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "BI1 get String";
    }
  }

  abstract static class B implements BI1 {}

  static class C extends B {}

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("BI1 get String");
    assert c.get("").equals("BI1 get String");
    assert ((BI1) c).get("").equals("BI1 get String");
  }
}
