package com.google.j2cl.transpiler.readable.newinnerclasswithsameouter;

public class NewInnerClassWithSameOuter {
  public class A {
    public void test() {
      // New instance of other inner classes with the same outer class.
      new B();
      new C();
    }
  }

  public class B {}
  // private inner class
  private class C {}
}
