package com.google.j2cl.transpiler.readable.lambdanoreturn;

public class LambdaNoReturn {
  interface Runner {
    void run();
  }

  @SuppressWarnings("unused")
  public void test() {
    Runner runner = () -> new Object();
  }
}
