package com.google.j2cl.transpiler.integration.arrayreadwrite;

public class Main {
  public static void main(String... args) {
    testObjects();
    testInts();
    testMains();
  }

  private static void testInts() {
    int[] ints = new int[100];

    // Initial value.
    assert ints[0] == 0;

    // Assignment.
    ints[0] = 10;
    assert ints[0] == 10;

    // Compound assignment.
    ints[0] += 100;
    assert ints[0] == 110;
  }

  private static void testObjects() {
    Object expectedObject = new Object();
    Object[] objects = new Object[100];

    // Initial value.
    assert objects[0] == null;
    assert objects[0] != expectedObject;

    // Assignment.
    objects[0] = expectedObject;
    assert objects[0] == expectedObject;
  }

  private static void testMains() {
    Main m = new Main();
    Main[] mains = new Main[100];

    mains[0] = null;
    assert mains[0] == null;

    mains[0] = m;
    assert mains[0] == m;
  }
}
