package com.google.j2cl.transpiler.integration.morebridgemethods;

public class TestCase5814 {
  static interface BI1 {
    String get(String value);
  }

  abstract static class B<B1> implements BI1 {
    public abstract String get(B1 value);
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
    assert ((BI1) c).get("").equals("C get String");
  }
}
