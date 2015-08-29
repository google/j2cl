package com.google.j2cl.transpiler.integration.lambdasnestedscope;

/**
 * Test nested lambdas.
 */
public class LambdaNestingInLambdas {
  public void test() {
    int a = 10;
    MyInterface i =
        m -> {
          int b = 20;
          MyInterface ii = n -> a + b + m + n;
          return ii.fun(100);
        };
    assert (i.fun(200) == 330);
  }
}
