package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsMethod;

public class TestCaseHand1 {
  private static class A<A1> {
    @SuppressWarnings("unused")
    public String get(A1 a1) {
      return "A get Object";
    }
  }

  private static class B<B1> extends A<B1> {
    @JsMethod
    @Override
    public String get(B1 b1) {
      return "B get Object";
    }
  }

  private static class C extends B<String> {
    @Override
    public String get(String string) {
      return "C get String";
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assert ((A) c).get("").equals("C get String");
    assert ((B) c).get("").equals("C get String");
    assert c.get("").equals("C get String");
  }
}
