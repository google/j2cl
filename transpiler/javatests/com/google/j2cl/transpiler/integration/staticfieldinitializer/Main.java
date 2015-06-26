package com.google.j2cl.transpiler.integration.staticfieldinitializer;

/**
 * Test static field initializer.
 */
public class Main {
  public static int simpleValue = 5;

  public static int calculatedValue = simpleValue * 5;

  public static int someStaticMethod() {
    return simpleValue * calculatedValue;
  }

  public static void main(String... args) {
    assert Main.simpleValue == 5;
    assert Main.calculatedValue == 25;
    assert Main.someStaticMethod() == 125;
  }
}
