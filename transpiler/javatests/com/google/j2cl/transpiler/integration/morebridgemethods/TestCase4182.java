package com.google.j2cl.transpiler.integration.morebridgemethods;

public class TestCase4182 {
  static class B<B1> {
    @SuppressWarnings("unused")
    public String get(String value) {
      return "B get String";
    }
  }

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
  }
}
