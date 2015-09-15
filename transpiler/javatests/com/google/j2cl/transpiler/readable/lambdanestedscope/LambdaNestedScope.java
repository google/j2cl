package com.google.j2cl.transpiler.readable.lambdanestedscope;

public class LambdaNestedScope {
  interface Runner {
    Object run();
  }

  @SuppressWarnings("unused")
  public void test() {
    Runner runner = () -> new Object() {};
  }
}
