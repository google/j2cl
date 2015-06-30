package com.google.j2cl.transpiler.integration.interfacemethoddispatch;

/**
 * Test interface use.
 */
public class Main {
  public static void main(String[] args) {
    SomeClass s = new SomeClass();
    assert run(s) == 1;
  }

  public static int run(SomeInterface someInterface) {
    return someInterface.run();
  }
}
