package com.google.j2cl.transpiler.integration.multiplecatch;

/**
 * Test multiple catch.
 */
public class Main {
  public static void main(String... args) {
    try {
      throwRuntimeException(); // So assert isn't considered dead code by javac.
      assert false; // should have skipped past this with an exception.
    } catch (NullPointerException | ClassCastException e) {
      assert false; // The wrong exception type was caught.
    } catch (RuntimeException r) {
      // expected
    }

    try {
      throwIllegalArgumentException(); // So assert isn't considered dead code by javac.
      assert false; // should have skipped past this with an exception.
    } catch (NullPointerException | IllegalArgumentException e) {
      // expected
    }
  }

  public static void throwRuntimeException() {
    throw new RuntimeException();
  }

  public static void throwIllegalArgumentException() {
    throw new IllegalArgumentException();
  }
}
