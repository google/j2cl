package com.google.j2cl.transpiler.integration.allsimplebridges;

public class Tester509 {
  static class C1 {
    public String get(Object value) {
      return "C1.get";
    }
  }

  @SuppressWarnings("unchecked")
  static class C2 extends C1 {}

  @SuppressWarnings("unchecked")
  public static void test() {
    C2 s = new C2();
    assert ((C1) s).get("").equals("C1.get");
  }
}
