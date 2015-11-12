package com.google.j2cl.transpiler.integration.casttoarray;

/**
 * Test cast to array type.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String... args) {
    testDimensionCasts();
    testTypeCasts();
  }

  private static void testTypeCasts() {
    Object o = null;

    Object[] objects = new Object[0];
    String[] strings = new String[0];
    CharSequence[] charSequences = new CharSequence[0];

    o = (Object[]) objects;
    o = (Object[]) strings;
    o = (String[]) strings;
    o = (CharSequence[]) strings;
    o = (Object[]) charSequences;
    o = (CharSequence[]) charSequences;

    try {
      o = (String[]) objects;
      assert false : "the expected cast exception did not occur";
    } catch (ClassCastException e) {
      // expected
    }
    try {
      o = (CharSequence[]) objects;
      assert false : "the expected cast exception did not occur";
    } catch (ClassCastException e) {
      // expected
    }
    try {
      o = (String[]) charSequences;
      assert false : "the expected cast exception did not occur";
    } catch (ClassCastException e) {
      // expected
    }
  }

  private static void testDimensionCasts() {
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
