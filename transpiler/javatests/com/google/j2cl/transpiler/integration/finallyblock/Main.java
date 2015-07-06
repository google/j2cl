package com.google.j2cl.transpiler.integration.finallyblock;

/**
 * Test finally block.
 */
public class Main {
  public static int value = 0;

  public static int fun() {
    try {
      return 10;
    } finally {
      value = 10;
    }
  }

  public static void main(String[] args) {
    assert fun() == Main.value;
  }
}
