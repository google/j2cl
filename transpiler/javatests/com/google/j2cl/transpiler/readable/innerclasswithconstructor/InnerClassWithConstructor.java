package com.google.j2cl.transpiler.readable.innerclasswithconstructor;

public class InnerClassWithConstructor {
  public int a;

  class InnerClass {
    public int b;

    public InnerClass(int b) {
      this.b = a + b;
    }

    public InnerClass() {
      this(a);
    }
  }
}
