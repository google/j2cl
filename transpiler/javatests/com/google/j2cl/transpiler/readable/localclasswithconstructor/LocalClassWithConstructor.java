package com.google.j2cl.transpiler.readable.localclasswithconstructor;

public class LocalClassWithConstructor {
  public void test(final int p) {
    final int localVar = 1;
    // local class with non-default constructor and this() call
    class LocalClass {
      public int field;

      public LocalClass(int a, int b) {
        field = localVar + a + b;
      }

      public LocalClass(int a) {
        this(a, p);
        field = localVar;
      }
    }
    new LocalClass(1);
  }
}
