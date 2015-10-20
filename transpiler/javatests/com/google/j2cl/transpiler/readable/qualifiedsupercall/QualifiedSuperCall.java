package com.google.j2cl.transpiler.readable.qualifiedsupercall;

class Parent {
  public void fun() {}
}

public class QualifiedSuperCall extends Parent {
  public class InnerClass {
    public void test() {
      QualifiedSuperCall.super.fun();
    }
  }
}
