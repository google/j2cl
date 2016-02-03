package com.google.j2cl.transpiler.integration.abstractinnerclass;

public class Main {
  interface A {
    int foo();
  }

  abstract static class B implements A {
    int bar() {
      return foo();
    }
  }

  abstract class C implements A {
    int bar() {
      return foo();
    }
  }

  static class BB extends B {
    @Override
    public int foo() {
      return 10;
    }
  }

  class CC extends C {
    @Override
    public int foo() {
      return 20;
    }
  }

  public static void main(String... args) {
    assert (new BB().bar() == 10);
    assert (new Main().new CC().bar() == 20);
  }
}
