package com.google.j2cl.transpiler.integration.casttoclass;

/**
 * Test cast to class type.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object object = new Main();

    // This is fine.
    Main main = (Main) object;

    // But this isn't fine.
    try {
      RuntimeException exception = (RuntimeException) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }
}
