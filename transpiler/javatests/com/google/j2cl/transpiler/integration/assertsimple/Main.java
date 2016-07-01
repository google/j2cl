package com.google.j2cl.transpiler.integration.assertsimple;

/**
 * Test method body, assert statement, and binary expression
 * with number literals work fine.
 */
public class Main {
  public static void main(String[] args) {
    assert 1 + 2 == 3;

    try {
      assert 2 == 3;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
    }
  }
}
