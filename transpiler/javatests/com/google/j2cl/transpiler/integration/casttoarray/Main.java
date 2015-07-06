package com.google.j2cl.transpiler.integration.casttoarray;

/**
 * Test cast to array type.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String... args) {
    Object object = new Object[10][10];

    // These are fine.
    Object[] object1d = (Object[]) object;
    Object[][] object2d = (Object[][]) object;

    // A 2d array cannot be cast to a 3d array.
    try {
      Object[][][] object3d = (Object[][][]) object2d;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }

    // A non-array cannot be cast to an array.
    try {
      object1d = (Object[]) new Object();
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }
}
