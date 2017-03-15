package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsType;

public class TestCase1596 {
  static interface BI1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "BI1 get String";
    }
  }

  @JsType
  @SuppressWarnings("unchecked")
  static class B<B1> implements BI1 {
    @SuppressWarnings("unused")
    public String get(B1 value) {
      return "B get B1";
    }
  }

  static class C extends B<String> {
    @JsConstructor
    public C() {}
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("B get B1");
    assert c.get("").equals("B get B1");
    assert ((BI1) c).get("").equals("B get B1");
  }
}
