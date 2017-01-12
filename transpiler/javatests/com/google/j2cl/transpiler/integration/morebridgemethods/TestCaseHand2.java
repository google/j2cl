package com.google.j2cl.transpiler.integration.morebridgemethods;

import java.util.function.Consumer;

public class TestCaseHand2 {

  static interface I<I1> {
    String get(Consumer<? super I1> consumer);
  }

  abstract static class B<B1, B2> implements I<B1> {
    @SuppressWarnings("unused")
    public String get(B2 consumer) {
      return "B get B2";
    }
  }

  static class C<C1> extends B<C1, Consumer<? super C1>> implements I<C1> {}

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void test() {
    C c = new C();
    assert ((B) c).get("").equals("B get B2");
    assert c.get("").equals("B get B2");
    assert ((I) c).get(null).equals("B get B2");
  }
}
