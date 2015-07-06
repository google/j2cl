package com.google.j2cl.transpiler.integration.nestedtrycatch;

/**
 * Test nested try catch.
 */
public class Main {
  static int count = 1;

  public static void main(String... args) {
    try {
      throwRuntimeException();
    } catch (RuntimeException ae) {
      count = 2;
      try {
        notThrowException();
      } catch (ClassCastException ie) {
        assert false; // No exception was thrown so shouldn't reach catch block.
      }
    } finally {
      assert count == 2; // first catch block is executed.
    }
  }

  public static void throwRuntimeException() throws RuntimeException {
    throw new RuntimeException();
  }

  public static void notThrowException() throws ClassCastException {}
}
