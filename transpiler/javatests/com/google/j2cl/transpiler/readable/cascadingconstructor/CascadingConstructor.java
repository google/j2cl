package com.google.j2cl.transpiler.readable.cascadingconstructor;

public class CascadingConstructor {
  private int a;
  private int b;

  private CascadingConstructor(int a, int b) {
    this.a = a;
    this.b = b;
  }

  public CascadingConstructor(int a) {
    this(a, a * 2);
  }
}
