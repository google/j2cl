package com.google.j2cl.transpiler.readable.simplelocalclass;

public class SimpleLocalClass {
  public void test(final int p) {
    final int localVar = 1;
    class InnerClass {
      public int fun() {
        return localVar + p; // captured local variable and parameter.
      }
    }
    new InnerClass().fun(); // new local class.

    // local class that has no captured variables.
    class InnerClassWithoutCaptures {}
    new InnerClassWithoutCaptures();
  }

  // Local class with same name.
  public void fun() {
    final int localVar = 1;
    class InnerClass {
      int field = localVar;
    }
  }

  // Local class with same name after $.
  public void foo() {
    class Abc$InnerClass {}
    class Klm$InnerClass {}
  }
}
