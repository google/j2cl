package com.google.j2cl.transpiler.integration.arrayliteral;

public class Main {
  public static void main(String... args) {
    testEmptyArrayLiteral();
    testOneD();
    testTwoD();
    testPartial2D();
    testUnbalanced2D();
    testTerseLiteral();
  }

  private static void testEmptyArrayLiteral() {
    Object[] empty = new Object[] {};
    assert empty.length == 0;
    try {
      empty[0] = null;
      assert false : "Should have already thrown IndexOutOfBounds.";
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }
  }

  private static void testOneD() {
    int[] oneD = new int[] {0, 1, 2};
    assert oneD.length == 3;

    // Values read fine.
    assert oneD[0] == 0;
    assert oneD[1] == 1;
    assert oneD[2] == 2;

    // Insertion to legal indexes is fine.
    oneD[0] = 5;
    oneD[1] = 5;
    oneD[2] = 5;

    // You can NOT insert to an out of bounds index.
    try {
      oneD[3] = 5;
      assert false : "An expected failure did not occur.";
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }
  }

  private static void testPartial2D() {
    Object[][] partial2D = new Object[][] {null};
    assert partial2D.length == 1;

    // Values read fine.
    assert partial2D[0] == null;

    // When trying to insert into the uninitialized section you'll get an NPE.
    try {
      partial2D[0][0] = new Object();
      assert false : "An expected failure did not occur.";
    } catch (NullPointerException e) {
      // expected
    }

    // You can replace it with a fully initialized array with the right dimensions.
    partial2D = new Object[2][2];
    assert partial2D.length == 2;
    assert partial2D[0].length == 2;
  }

  private static void testTwoD() {
    Main main = new Main();
    Object[][] twoD = new Main[][] {{main, main}, {main, main}};
    assert twoD.length == 2;
    assert twoD[0].length == 2;

    // Values read fine.
    assert twoD[0][0] == main;
    assert twoD[0][1] == main;
    assert twoD[1][0] == main;
    assert twoD[1][1] == main;

    // Insertion to legal indexes is fine.
    twoD[0][0] = main;
    twoD[0][1] = main;
    twoD[1][0] = main;
    twoD[1][1] = main;

    // You can NOT insert to an out of bounds index.
    try {
      twoD[0][2] = main;
      assert false : "An expected failure did not occur.";
    } catch (ArrayIndexOutOfBoundsException e) {
      // expected
    }

    // When inserting a leaf value the type must conform.
    try {
      twoD[0][0] = new Object();
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }

    // The object-literal partial arrays still know their depth and so for example will reject an
    // attempt to stick an object into a 1-dimensional array slot.
    try {
      ((Object[]) twoD)[1] = new Object();
      assert false : "An expected failure did not occur.";
    } catch (ArrayStoreException e) {
      // expected
    }
  }

  private static void testUnbalanced2D() {
    Object[][] unbalanced2D = new Object[][] {{null, null}, null};
    assert unbalanced2D.length == 2;

    // The first branch is actually fully initialized.
    assert unbalanced2D[0][0] == null;
    assert unbalanced2D[0].length == 2;
    // The second branch less so.
    assert unbalanced2D[1] == null;
  }

  @SuppressWarnings("cast")
  private static void testTerseLiteral() {
    int[] terseLiteral = {0, 1, 2};
    assert terseLiteral.length == 3;
    assert terseLiteral instanceof int[];
  }
}
