package com.google.j2cl.transpiler.integration.multiplecatch;

/**
 * Test multiple catch.
 */
public class Main {
  public static void main(String... args) {
    try {
      throwRuntimeException(); // So that the following assert isn't considered dead code.
      assert false; // should have skipped past this with an exception.
    } catch (NullPointerException | ClassCastException e) {
      assert false; // The wrong exception type was caught.
    } catch (RuntimeException r) {
      // expected
    }
  }

  public static void throwRuntimeException() throws RuntimeException {
    throw new RuntimeException();
  }
}
