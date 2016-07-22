package com.google.j2cl.transpiler.readable.abstractmethodoverridedefault;

public class AbstractMethodOverrideDefault {
  interface I {
    void foo();

    default void bar() {}
  }

  interface J {
    default void foo() {}

    void bar();
  }

  abstract static class A implements I {
    @Override
    public abstract void foo();
  }

  abstract static class B extends A implements J {
    @SuppressWarnings("unused")
    @Override
    public void bar() {
      if (false) {
        bar();
      }
    }
  }
}
