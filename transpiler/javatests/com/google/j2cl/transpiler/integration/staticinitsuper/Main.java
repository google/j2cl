package com.google.j2cl.transpiler.integration.staticinitsuper;

import java.util.ArrayList;
import java.util.List;

/** Test static initializers order via super. */
public class Main {

  static class A implements B, C {}

  interface B {
    static final Object rerunClinitOfA = new A();
    static final Object triggerD1 = new D1();

    default void dummyB() {}
  }

  interface C {
    static final Object triggerD2 = new D2();

    default void dummyC() {}
  }

  static class D1 {
    static {
      loadOrder.add("D1");
    }
  }

  static class D2 {
    static {
      loadOrder.add("D2");
    }
  }

  private static List<String> loadOrder = new ArrayList<>();

  public static void main(String... args) {
    new A(); // start clinit chain

    assert 2 == loadOrder.size() : "classes are not loaded";

    // TODO(b/28909166): Uncomment below
    // assert loadOrder.get(0).equals("D1") : "expected D1";
    // assert loadOrder.get(1).equals("D2") : "expected D2";
  }
}
