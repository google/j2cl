package com.google.j2cl.transpiler.readable.jsvarargs;

public class QualifiedSuperMethodCall extends Main {
  public QualifiedSuperMethodCall() {
    super(0);
  }

  public class InnerClass {
    public void test() {
      // multiple arguments.
      QualifiedSuperMethodCall.super.f3(1, 1, 2);
      // no argument for varargs.
      QualifiedSuperMethodCall.super.f3(1);
      // array literal for varargs.
      QualifiedSuperMethodCall.super.f3(1, new int[] {1, 2});
      // empty array literal for varargs.
      QualifiedSuperMethodCall.super.f3(1, new int[] {});
      // array object for varargs.
      int[] ints = new int[] {1, 2};
      QualifiedSuperMethodCall.super.f3(1, ints);
    }
  }
}
