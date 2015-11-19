package com.google.j2cl.transpiler.integration.synchronizedblock;

public class Main {
  private static boolean expressionCalled;
  private static boolean blockExecuted;

  public static void main(String... args) {
    synchronized (getX()) {
      blockExecuted = true;
    }
    assert expressionCalled;
    assert blockExecuted;
  }

  private static X getX() {
    expressionCalled = true;
    return new X();
  }

  static class X {}
}
