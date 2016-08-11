package com.google.j2cl.transpiler.integration.allsimplebridges;

public class Tester610 {
  static interface I1 {
    default String get(String value) {
      return "I1.get";
    }
  }

  @SuppressWarnings("unchecked")
  static class C1 implements I1 {}

  @SuppressWarnings("unchecked")
  public static void test() {
    C1 s = new C1();
    assert ((I1) s).get("").equals("I1.get");
  }
}
