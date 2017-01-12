package com.google.j2cl.transpiler.integration.morebridgemethods;

public class TestCase5828 {
  static class C {
    @SuppressWarnings("unused")
    public String get(Object value) {
      return "C get Object";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert c.get("").equals("C get Object");
  }
}
