package com.google.j2cl.transpiler.integration.arrayreadwrite;

public class Main {

  public static void main(String... args) {
    testObjects();
    testInts();
    testLongs();
    testMains();
    testCompoundArrayOperations();
  }

  private static void testLongs() {
    long[] longs = new long[100];
    int i = 0;

    // Initial value.
    assert longs[0] == 0;

    // Assignment.
    assert (longs[0] = 10) == 10;
    assert longs[0] == 10;

    // Compound assignment.
    assert (longs[0] += 100) == 110;
    assert longs[0] == 110;

    // Preservation of side effects for compound assignment expressions
    // (Any expressions with side effects should be evaluated exactly once).
    i = 1;
    assert (longs[i++] += 1) == 1;
    assert i == 2;
    assert longs[0] == 110;
    assert longs[1] == 1; // 0 + 1 = 1
    assert longs[2] == 0;

    assert (longs[i++] -= 1) == -1;
    assert i == 3;
    assert longs[1] == 1;
    assert longs[2] == -1; // 0 - 1 = -1
    assert longs[3] == 0;

    longs[i] = 2;
    assert (longs[i++] *= 2) == 4;
    assert i == 4;
    assert longs[2] == -1;
    assert longs[3] == 4; // 2 * 2 = 4
    assert longs[4] == 0;

    longs[i] = 10;
    assert (longs[i++] /= 2) == 5;
    assert i == 5;
    assert longs[3] == 4;
    assert longs[4] == 5; // 10 / 2 = 5
    assert longs[5] == 0;

    longs[i] = 10;
    assert (longs[i++] %= 4) == 2;
    assert i == 6;
    assert longs[4] == 5;
    assert longs[5] == 2; // 10 % 4 = 2
    assert longs[6] == 0;

    longs[i] = 7;
    assert (longs[i++] &= 14) == 6;
    assert i == 7;
    assert longs[5] == 2;
    assert longs[6] == 6; // 7 & 14 = 6
    assert longs[7] == 0;

    longs[i] = 8;
    assert (longs[i++] |= 2) == 10;
    assert i == 8;
    assert longs[6] == 6;
    assert longs[7] == 10; // 8 | 2  = 10
    assert longs[8] == 0;

    longs[i] = 7;
    assert (longs[i++] ^= 14) == 9;
    assert i == 9;
    assert longs[7] == 10;
    assert longs[8] == 9; // 7 ^ 14 = 9
    assert longs[9] == 0;

    longs[i] = 2;
    assert (longs[i++] <<= 1) == 4;
    assert i == 10;
    assert longs[8] == 9;
    assert longs[9] == 4; // 2 << 1 = 4
    assert longs[10] == 0;

    longs[i] = -10;
    assert (longs[i++] >>= 2) == -3;
    assert i == 11;
    assert longs[9] == 4;
    assert longs[10] == -3; // -10 >> 2 = -3
    assert longs[11] == 0;

    longs[i] = -10;
    assert (longs[i++] >>>= 2) == 4611686018427387901L;
    assert i == 12;
    assert longs[10] == -3;
    assert longs[11] == 4611686018427387901L; // -10 >>> 2 = 4611686018427387901
    assert longs[12] == 0;

    // Prefix and postfix expressions.
    i = 15;
    assert (++longs[i]) == 1;
    assert longs[15] == 1;

    assert (longs[i]++) == 1;
    assert longs[15] == 2;

    //Preservation of side effects for prefix and postfix expressions.
    assert (++longs[++i]) == 1;
    assert i == 16;
    assert longs[15] == 2;
    assert longs[16] == 1;
    assert longs[17] == 0;

    assert (longs[++i]++) == 0;
    assert i == 17;
    assert longs[16] == 1;
    assert longs[17] == 1;
    assert longs[18] == 0;
  }

  private static void testInts() {
    int[] ints = new int[100];
    int i = 0;

    // Initial value.
    assert ints[0] == 0;

    // Assignment.
    assert (ints[0] = 10) == 10;
    assert ints[0] == 10;

    // Compound assignment.
    assert (ints[0] += 100) == 110;
    assert ints[0] == 110;

    // Preservation of side effects in compound assignment
    // (Any expressions with side effects should be evaluated exactly once).
    i = 1;
    assert (ints[i++] += 1) == 1;
    assert i == 2;
    assert ints[0] == 110;
    assert ints[1] == 1;
    assert ints[2] == 0;

    assert (ints[i++] -= 1) == -1;
    assert i == 3;
    assert ints[1] == 1;
    assert ints[2] == -1; // 0 - 1 = -1
    assert ints[3] == 0;

    ints[i] = 2;
    assert (ints[i++] *= 2) == 4;
    assert i == 4;
    assert ints[2] == -1;
    assert ints[3] == 4; // 2 * 2 = 4
    assert ints[4] == 0;

    ints[i] = 10;
    assert (ints[i++] /= 2) == 5;
    assert i == 5;
    assert ints[3] == 4;
    assert ints[4] == 5; // 10 / 2 = 5
    assert ints[5] == 0;

    ints[i] = 10;
    assert (ints[i++] %= 4) == 2;
    assert i == 6;
    assert ints[4] == 5;
    assert ints[5] == 2; // 10 % 4 = 2
    assert ints[6] == 0;

    ints[i] = 7;
    assert (ints[i++] &= 14) == 6;
    assert i == 7;
    assert ints[5] == 2;
    assert ints[6] == 6; // 7 & 14 = 6
    assert ints[7] == 0;

    ints[i] = 8;
    assert (ints[i++] |= 2) == 10;
    assert i == 8;
    assert ints[6] == 6;
    assert ints[7] == 10; // 8 | 2  = 10
    assert ints[8] == 0;

    ints[i] = 7;
    assert (ints[i++] ^= 14) == 9;
    assert i == 9;
    assert ints[7] == 10;
    assert ints[8] == 9; // 7 ^ 14 = 9
    assert ints[9] == 0;

    ints[i] = 2;
    assert (ints[i++] <<= 1) == 4;
    assert i == 10;
    assert ints[8] == 9;
    assert ints[9] == 4; // 2 << 1 = 4
    assert ints[10] == 0;

    ints[i] = -10;
    assert (ints[i++] >>= 2) == -3;
    assert i == 11;
    assert ints[9] == 4;
    assert ints[10] == -3; // -10 >> 2 = -3
    assert ints[11] == 0;

    ints[i] = -10;
    assert (ints[i++] >>>= 2) == 1073741821;
    assert i == 12;
    assert ints[10] == -3;
    assert ints[11] == 1073741821; // -10 >>> 2 = 1073741821
    assert ints[12] == 0;

    // Prefix and postfix expressions.
    i = 15;
    assert (++ints[i]) == 1;
    assert ints[15] == 1;

    assert (ints[i]++) == 1;
    assert ints[15] == 2;

    //Preservation of side effects for prefix and postfix expressions.
    assert (++ints[++i]) == 1;
    assert i == 16;
    assert ints[15] == 2;
    assert ints[16] == 1;
    assert ints[17] == 0;

    assert (ints[++i]++) == 0;
    assert i == 17;
    assert ints[16] == 1;
    assert ints[17] == 1;
    assert ints[18] == 0;
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

  private static void testCompoundArrayOperations() {
    int[] intArray = new int[1];
    intArray[0] += 3;
    assert intArray[0] == 3;

    intArray[0] /= 2;
    // Uncomment the following code after b/34341877 has been fixed
    // assert intArray[0] == 1;

    // String[] stringArray = new String[1];
    // stringArray[0] += null;
    // assert stringArray[0].equals("nullnull");

    // boolean[] booleanArray = new boolean[1];
    // booleanArray[0] |= true;
    // Object o = booleanArray[0];
    // assert o instanceof Boolean;

    long[] longArray = new long[1];
    longArray[0] += 1;
    assert longArray[0] == 1;
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
