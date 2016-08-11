package com.google.j2cl.transpiler.integration.allsimplebridges;

public class Tester261 {
  abstract static class C1<T> {
    public abstract String get(T value);
  }

  @SuppressWarnings("unchecked")
  static class C2 extends C1<String> {
    @SuppressWarnings("MissingOverride")
    public String get(String value) {
      return "C2.get";
    }
  }

  @SuppressWarnings("unchecked")
  public static void test() {
    C2 s = new C2();
    assert s.get("").equals("C2.get");
    assert ((C1) s).get("").equals("C2.get");
  }
}
