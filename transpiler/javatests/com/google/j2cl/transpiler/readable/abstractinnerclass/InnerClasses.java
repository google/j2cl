package com.google.j2cl.transpiler.readable.abstractinnerclass;

public class InnerClasses {
  interface A {
    void foo();
  }

  abstract static class B implements A {
    void bar() {
      foo();
    }
  }

  abstract class C implements A {
    void bar() {
      foo();
    }
  }
}
