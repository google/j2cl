package com.google.j2cl.transpiler.integration.lambdasnestedscope;

/**
 * Test lambda nested in local class.
 */
public class LambdaNestingInLocalClass {
  public int f = 1;

  class InnerClass {
    public int f = 10;

    public int run(int a) {
      MyInterface intf = i -> LambdaNestingInLocalClass.this.f + f + i;
      return intf.fun(a);
    }
  }

  public void test() {
    assert new InnerClass().run(100) == 111;
  }
}
