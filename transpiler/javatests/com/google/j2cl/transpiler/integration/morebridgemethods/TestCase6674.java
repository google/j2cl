package com.google.j2cl.transpiler.integration.morebridgemethods;

public class TestCase6674 {
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

  static class C extends B<String> {}

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("B get B1");
    assert c.get("").equals("B get B1");
    assert ((BI1) c).get("").equals("B get B1");
  }
}
