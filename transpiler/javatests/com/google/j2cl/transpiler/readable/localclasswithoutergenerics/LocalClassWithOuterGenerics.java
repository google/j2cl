package com.google.j2cl.transpiler.readable.localclasswithoutergenerics;

public class LocalClassWithOuterGenerics<A> {
  @SuppressWarnings("unused")
  public static <T> void foo() {
    class Bar {
      void baz(Bar other) {}
    }
    Bar bar = new Bar();
  }
}
