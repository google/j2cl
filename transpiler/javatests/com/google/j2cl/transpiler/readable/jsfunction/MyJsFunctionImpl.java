package com.google.j2cl.transpiler.readable.jsfunction;

public class MyJsFunctionImpl implements MyJsFunctionInterface {
  public int field;

  public int bar() {
    return 0;
  }

  public int fun() {
    return bar() + foo(1);
  }

  @Override
  public int foo(int a) {
    return a + this.bar() + this.field;
  }
}
