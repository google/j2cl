/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package arrayreadwrite;

import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

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
    assertTrue(longs[0] == 0);

    // Assignment.
    assertTrue((longs[0] = 10) == 10);
    assertTrue(longs[0] == 10);

    // Compound assignment.
    assertTrue((longs[0] += 100) == 110);
    assertTrue(longs[0] == 110);

    // Preservation of side effects for compound assignment expressions
    // (Any expressions with side effects should be evaluated exactly once).
    i = 1;
    assertTrue((longs[i++] += 1) == 1);
    assertTrue(i == 2);
    assertTrue(longs[0] == 110);
    assertTrue(longs[1] == 1); // 0 + 1 = 1
    assertTrue(longs[2] == 0);

    assertTrue((longs[i++] -= 1) == -1);
    assertTrue(i == 3);
    assertTrue(longs[1] == 1);
    assertTrue(longs[2] == -1); // 0 - 1 = -1
    assertTrue(longs[3] == 0);

    longs[i] = 2;
    assertTrue((longs[i++] *= 2) == 4);
    assertTrue(i == 4);
    assertTrue(longs[2] == -1);
    assertTrue(longs[3] == 4); // 2 * 2 = 4
    assertTrue(longs[4] == 0);

    longs[i] = 10;
    assertTrue((longs[i++] /= 2) == 5);
    assertTrue(i == 5);
    assertTrue(longs[3] == 4);
    assertTrue(longs[4] == 5); // 10 / 2 = 5
    assertTrue(longs[5] == 0);

    longs[i] = 10;
    assertTrue((longs[i++] %= 4) == 2);
    assertTrue(i == 6);
    assertTrue(longs[4] == 5);
    assertTrue(longs[5] == 2); // 10 % 4 = 2
    assertTrue(longs[6] == 0);

    longs[i] = 7;
    assertTrue((longs[i++] &= 14) == 6);
    assertTrue(i == 7);
    assertTrue(longs[5] == 2);
    assertTrue(longs[6] == 6); // 7 & 14 = 6
    assertTrue(longs[7] == 0);

    longs[i] = 8;
    assertTrue((longs[i++] |= 2) == 10);
    assertTrue(i == 8);
    assertTrue(longs[6] == 6);
    assertTrue(longs[7] == 10); // 8 | 2  = 10
    assertTrue(longs[8] == 0);

    longs[i] = 7;
    assertTrue((longs[i++] ^= 14) == 9);
    assertTrue(i == 9);
    assertTrue(longs[7] == 10);
    assertTrue(longs[8] == 9); // 7 ^ 14 = 9
    assertTrue(longs[9] == 0);

    longs[i] = 2;
    assertTrue((longs[i++] <<= 1) == 4);
    assertTrue(i == 10);
    assertTrue(longs[8] == 9);
    assertTrue(longs[9] == 4); // 2 << 1 = 4
    assertTrue(longs[10] == 0);

    longs[i] = -10;
    assertTrue((longs[i++] >>= 2) == -3);
    assertTrue(i == 11);
    assertTrue(longs[9] == 4);
    assertTrue(longs[10] == -3); // -10 >> 2 = -3
    assertTrue(longs[11] == 0);

    longs[i] = -10;
    assertTrue((longs[i++] >>>= 2) == 4611686018427387901L);
    assertTrue(i == 12);
    assertTrue(longs[10] == -3);
    assertTrue(longs[11] == 4611686018427387901L); // -10 >>> 2 = 4611686018427387901
    assertTrue(longs[12] == 0);

    // Prefix and postfix expressions.
    i = 15;
    assertTrue((++longs[i]) == 1);
    assertTrue(longs[15] == 1);

    assertTrue((longs[i]++) == 1);
    assertTrue(longs[15] == 2);

    // Preservation of side effects for prefix and postfix expressions.
    assertTrue((++longs[++i]) == 1);
    assertTrue(i == 16);
    assertTrue(longs[15] == 2);
    assertTrue(longs[16] == 1);
    assertTrue(longs[17] == 0);

    assertTrue((longs[++i]++) == 0);
    assertTrue(i == 17);
    assertTrue(longs[16] == 1);
    assertTrue(longs[17] == 1);
    assertTrue(longs[18] == 0);
  }

  private static void testInts() {
    int[] ints = new int[100];
    int i = 0;

    // Initial value.
    assertTrue(ints[0] == 0);

    // Assignment.
    assertTrue((ints[0] = 10) == 10);
    assertTrue(ints[0] == 10);

    // Compound assignment.
    assertTrue((ints[0] += 100) == 110);
    assertTrue(ints[0] == 110);

    // Preservation of side effects in compound assignment
    // (Any expressions with side effects should be evaluated exactly once).
    i = 1;
    assertTrue((ints[i++] += 1) == 1);
    assertTrue(i == 2);
    assertTrue(ints[0] == 110);
    assertTrue(ints[1] == 1);
    assertTrue(ints[2] == 0);

    assertTrue((ints[i++] -= 1) == -1);
    assertTrue(i == 3);
    assertTrue(ints[1] == 1);
    assertTrue(ints[2] == -1); // 0 - 1 = -1
    assertTrue(ints[3] == 0);

    ints[i] = 2;
    assertTrue((ints[i++] *= 2) == 4);
    assertTrue(i == 4);
    assertTrue(ints[2] == -1);
    assertTrue(ints[3] == 4); // 2 * 2 = 4
    assertTrue(ints[4] == 0);

    ints[i] = 10;
    assertTrue((ints[i++] /= 2) == 5);
    assertTrue(i == 5);
    assertTrue(ints[3] == 4);
    assertTrue(ints[4] == 5); // 10 / 2 = 5
    assertTrue(ints[5] == 0);

    ints[i] = 10;
    assertTrue((ints[i++] %= 4) == 2);
    assertTrue(i == 6);
    assertTrue(ints[4] == 5);
    assertTrue(ints[5] == 2); // 10 % 4 = 2
    assertTrue(ints[6] == 0);

    ints[i] = 7;
    assertTrue((ints[i++] &= 14) == 6);
    assertTrue(i == 7);
    assertTrue(ints[5] == 2);
    assertTrue(ints[6] == 6); // 7 & 14 = 6
    assertTrue(ints[7] == 0);

    ints[i] = 8;
    assertTrue((ints[i++] |= 2) == 10);
    assertTrue(i == 8);
    assertTrue(ints[6] == 6);
    assertTrue(ints[7] == 10); // 8 | 2  = 10
    assertTrue(ints[8] == 0);

    ints[i] = 7;
    assertTrue((ints[i++] ^= 14) == 9);
    assertTrue(i == 9);
    assertTrue(ints[7] == 10);
    assertTrue(ints[8] == 9); // 7 ^ 14 = 9
    assertTrue(ints[9] == 0);

    ints[i] = 2;
    assertTrue((ints[i++] <<= 1) == 4);
    assertTrue(i == 10);
    assertTrue(ints[8] == 9);
    assertTrue(ints[9] == 4); // 2 << 1 = 4
    assertTrue(ints[10] == 0);

    ints[i] = -10;
    assertTrue((ints[i++] >>= 2) == -3);
    assertTrue(i == 11);
    assertTrue(ints[9] == 4);
    assertTrue(ints[10] == -3); // -10 >> 2 = -3
    assertTrue(ints[11] == 0);

    ints[i] = -10;
    assertTrue((ints[i++] >>>= 2) == 1073741821);
    assertTrue(i == 12);
    assertTrue(ints[10] == -3);
    assertTrue(ints[11] == 1073741821); // -10 >>> 2 = 1073741821
    assertTrue(ints[12] == 0);

    // Prefix and postfix expressions.
    i = 15;
    assertTrue((++ints[i]) == 1);
    assertTrue(ints[15] == 1);

    assertTrue((ints[i]++) == 1);
    assertTrue(ints[15] == 2);

    // Preservation of side effects for prefix and postfix expressions.
    assertTrue((++ints[++i]) == 1);
    assertTrue(i == 16);
    assertTrue(ints[15] == 2);
    assertTrue(ints[16] == 1);
    assertTrue(ints[17] == 0);

    assertTrue((ints[++i]++) == 0);
    assertTrue(i == 17);
    assertTrue(ints[16] == 1);
    assertTrue(ints[17] == 1);
    assertTrue(ints[18] == 0);
  }

  private static void testObjects() {
    Object expectedObject = new Object();
    Object[] objects = new Object[100];

    // Initial value.
    assertTrue(objects[0] == null);
    assertTrue(objects[0] != expectedObject);

    // Assignment.
    objects[0] = expectedObject;
    assertTrue(objects[0] == expectedObject);
  }

  private static void testCompoundArrayOperations() {
    int[] intArray = new int[1];
    intArray[0] += 3;
    assertTrue(intArray[0] == 3);

    intArray[0] /= 2;
    assertTrue(intArray[0] == 1);

    boolean[] booleanArray = new boolean[1];
    assertFalse(booleanArray[0]);
    booleanArray[0] |= true;
    assertTrue(booleanArray[0]);

    long[] longArray = new long[1];
    longArray[0] += 1;
    assertTrue(longArray[0] == 1);

    String[] stringArray = new String[1];
    stringArray[0] += null;
    assertTrue(stringArray[0].equals("nullnull"));
  }

  private static void testMains() {
    Main m = new Main();
    Main[] mains = new Main[100];

    mains[0] = null;
    assertTrue(mains[0] == null);

    mains[0] = m;
    assertTrue(mains[0] == m);
  }
}
