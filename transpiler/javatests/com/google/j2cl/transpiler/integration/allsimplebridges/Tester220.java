package com.google.j2cl.transpiler.integration.allsimplebridges;

import jsinterop.annotations.JsType;

public class Tester220 {
  static interface I1 {
    String get(String value);
  }

  @JsType
  static class C1 {
    public String get(Object value) {
      return "C1.get";
    }
  }

  @SuppressWarnings("unchecked")
  static class C2 extends C1 implements I1 {
    @SuppressWarnings("MissingOverride")
    public String get(String value) {
      return "C2.get";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C2 s = new C2();
    assert s.get("").equals("C2.get");
    assert ((C1) s).get("").equals("C1.get");
    assert ((I1) s).get("").equals("C2.get");
  }
}
