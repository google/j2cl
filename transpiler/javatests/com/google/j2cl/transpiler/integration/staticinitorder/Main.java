package com.google.j2cl.transpiler.integration.staticinitorder;

/**
 * Test static initializers order.
 */
public class Main {
  public static int counter = 1;

  public static void main(String... args) {
    assert Main.counter == 5;
    assert Main.field1 == 2;
    assert Main.field2 == 4;
  }

  public static int field1 = initializeField1();

  static {
    assert counter++ == 2; // #2
  }

  public static int field2 = initializeField2();

  static {
    assert counter++ == 4; // #4
  }

  public static int initializeField1() {
    assert counter++ == 1; // #1
    return counter;
  }

  public static int initializeField2() {
    assert counter++ == 3; // #3
    return counter;
  }
}
