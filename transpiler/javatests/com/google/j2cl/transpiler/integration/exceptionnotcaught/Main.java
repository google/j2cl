package com.google.j2cl.transpiler.integration.exceptionnotcaught;

/**
 * Test exception is not caught.
 */
public class Main {
  public static int value = 1;

  public static void main(String[] args) {
    try {
      uncaughtException();
      value = 2; // exception should be thrown, this statement should not be executed.
      // assert false here cannot guarantee the transpiler transpiles correctly.
      // Even if it is not transpiled correctly, and the statement after uncaughtException() is
      // executed, it will be caught by the catch block and will not cause an error.
    } catch (ClassCastException e) {
      // expected.
    } finally {
      assert value == 1;
    }
  }

  public static void uncaughtException() throws ClassCastException {
    try {
      throw new ClassCastException();
    } catch (NullPointerException e) {
      assert false; // wrong exception type
    }
  }
}
