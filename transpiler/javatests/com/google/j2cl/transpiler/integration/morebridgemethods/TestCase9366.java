package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase9366 {
  @SuppressWarnings("rawtypes")
  static class B<B1 extends Comparable> {
    @SuppressWarnings("unused")
    public String get(String value) {
      return "B get String";
    }
  }

  @JsType
  static class C<C1 extends String> extends B<C1> {
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
  }
}
