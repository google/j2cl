package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase10020 {
  static class B<B1> {
    @SuppressWarnings("unused")
    public String get(String value) {
      return "B get String";
    }
  }

  @JsType
  static interface CI1 {
    String get(String value);
  }

  @SuppressWarnings("rawtypes")
  static class C extends B implements CI1 {}

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("B get String");
    assert c.get("").equals("B get String");
    assert ((CI1) c).get("").equals("B get String");
  }
}
