package com.google.j2cl.transpiler.integration.morebridgemethods;

import jsinterop.annotations.JsType;

public class Tester5 {
  @JsType
  static interface I1 {
    @SuppressWarnings("unused")
    default String get(String value) {
      return "I1.get";
    }
  }

  abstract static class C1<T> {
    public abstract String get(T value);
  }

  @SuppressWarnings("unchecked")
  static class C2 extends C1<String> implements I1 {
    @Override
    public String get(String value) {
      return "C2.get";
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C2 s = new C2();
    assert s.get("").equals("C2.get");
    assert ((C1) s).get("").equals("C2.get");
    assert ((I1) s).get("").equals("C2.get");
  }
}
