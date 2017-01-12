package com.google.j2cl.transpiler.integration.morebridgemethods;

public class TestCase9226 {
  static class B<B1> {
    @SuppressWarnings("unused")
    public String get(String value) {
      return "B get String";
    }
  }

  static class C extends B<String> {}

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("B get String");
    assert c.get("").equals("B get String");
  }
}
