package com.google.j2cl.transpiler.integration.morebridgemethods;

public class TestCase2123 {
  static interface BI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "BI1 get String";
    }
  }

  static class B implements BI1 {}

  static class C extends B {
    @Override
    public String get(String value) {
      return "C get String";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("C get String");
    assert c.get("").equals("C get String");
    assert ((BI1) c).get("").equals("C get String");
  }
}
