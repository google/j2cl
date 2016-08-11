package com.google.j2cl.transpiler.integration.allsimplebridges;

public class Tester929 {
  @SuppressWarnings("unchecked")
  static class C1 {
    @SuppressWarnings("MissingOverride")
    public String get(Object value) {
      return "C1.get";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C1 s = new C1();
    assert s.get(new Object()).equals("C1.get");
  }
}
