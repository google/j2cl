package com.google.j2cl.transpiler.readable.varargs;

public class Varargs {
  public Varargs(int... args) {}

  public Varargs() {
    this(1);
  }

  public void test(int a, Object... args) {}

  public void main() {
    Varargs v = new Varargs();
    v.test(1);
    v.test(1, new Object());
    v.test(1, new Object[] {new Object()});
    v.test(1, new Object[] {});
  }
}

class Child extends Varargs {
  public Child() {
    super(1);
  }
}
