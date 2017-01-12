package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class TestCase3371 {
  abstract static class B {
    public abstract String get(Object value);
  }

  @JsType
  static class C extends B {
    @Override
    public String get(Object value) {
      return "C get Object";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("C get Object");
    assert c.get("").equals("C get Object");
  }
}
