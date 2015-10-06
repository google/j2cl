package com.google.j2cl.transpiler.integration.newinnerclasswithsameouter;

public class Main {
  public int f;

  public Main(int f) {
    this.f = f;
  }

  public class A {
    public void test() {
      // New instance of other inner classes with the same outer class.
      B b = new B();
      C c = new C();
      assert (b.get() == 10);
      assert (c.get() == 20);
    }
  }

  public class B {
    public int get() {
      return f;
    }
  }

  // private inner class
  private class C {
    public int get() {
      return f * 2;
    }
  }

  public static void main(String... args) {
    Main m = new Main(10);
    A a = m.new A();
    a.test();
  }
}
