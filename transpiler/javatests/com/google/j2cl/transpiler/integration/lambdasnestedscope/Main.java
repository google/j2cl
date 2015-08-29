package com.google.j2cl.transpiler.integration.lambdasnestedscope;

public class Main {
  public static void main(String... args) {
    new LambdaNestingInAnonymousClasses().test();
    new LambdaNestingInLambdas().test();
    new LambdaNestingInLocalClass().test();
    new MixedNesting().test();
  }
}
