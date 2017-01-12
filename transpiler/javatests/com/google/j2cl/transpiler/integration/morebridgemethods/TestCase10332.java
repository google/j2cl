package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase10332 {
  static class B {
    @SuppressWarnings("unused")
    public String get(String value) {
      return "B get String";
    }
  }

  @JsType
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
  }
}
