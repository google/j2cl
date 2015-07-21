package com.google.j2cl.transpiler.integration.externalunqualifiedstaticfield;

/**
 * Test unqualified external static field reference.
 */
public class Main {
  public static void main(String[] args) {
    assert getEnumValue(Numbers.ONE) == 1;
    assert getEnumValue(Numbers.TWO) == 2;
    assert getEnumValue(Numbers.THREE) == 3;
  }

  private static int getEnumValue(Numbers numberValue) {
    switch (numberValue) {
      case ONE: // These unqualified static field references have a hidden implicit Numbers. prefix.
        return 1;
      case TWO: // These unqualified static field references have a hidden implicit Numbers. prefix.
        return 2;
      default:
        return 3;
    }
  }
}
