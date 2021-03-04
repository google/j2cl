/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.integration.foreachstatement;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Test for for loops.
 */
public class Main {

  static class MyIterable implements Iterable<Integer>, Serializable {
    class MyIterator implements Iterator<Integer> {
      private int i = 5;

      public boolean hasNext() {
        return i >= 1;
      }

      public Integer next() {
        return new Integer(i--);
      }

      public void remove() {}
    }

    public MyIterator iterator() {
      return new MyIterator();
    }
  }

  private static void testForEachArray() {
    int[] array = new int[] {5, 4, 3, 2, 1};
    int j = 5;
    int lastSeenInt = -1;
    for (int i : array) {
      assertTrue("Seen:<" + i + "> Expected:<" + j + ">", i == j);
      j--;
      lastSeenInt = i;
    }

    assertTrue("LastSeen:<" + lastSeenInt + "> should be zero", lastSeenInt == 1);
  }

  private static void testForEachArray_nested() {
    int[][] matrix = new int[5][5];
    // load the matrix
    for (int i = 0; i < 5; i++) {
      for (int k = 0; k < 5; k++) {
        matrix[i][k] = value(i, k);
      }
    }

    int i0 = 0;
    for (int[] row : matrix) {
      int k0 = 0;
      for (int k : row) {
        assertTrue("Seen:<" + k + "> Expected:<" + value(i0, k0) + ">", k == value(i0, k0));
        k0++;
      }
      i0++;
    }
  }

  private static int value(int i, int j) {
    return i * 10 + j;
  }

  private static void testForEachIterable() {
    Integer lastSeenInteger = new Integer(-1);
    int j = 5;
    for (Integer s : new MyIterable()) {
      assertTrue("Seen:<" + s + "> Expected:<" + j + ">", s.intValue() == j);
      j--;
      lastSeenInteger = s;
    }
    assertTrue(
        "LastSeen:<" + lastSeenInteger + "> should be zero", lastSeenInteger.intValue() == 1);
  }

  private static <T extends MyIterable & Serializable, S extends T>
      void testForEachIterable_typeVariablesAndIntersectionTypes() {
    S iterable = (S) new MyIterable();
    Integer lastSeenInteger = new Integer(-1);
    int j = 5;
    for (Integer s : iterable) {
      assertTrue("Seen:<" + s + "> Expected:<" + j + ">", s.intValue() == j);
      j--;
      lastSeenInteger = s;
    }
    assertTrue(
        "LastSeen:<" + lastSeenInteger + "> should be zero", lastSeenInteger.intValue() == 1);
  }

  public static void main(String... args) {
    testForEachArray();
    testForEachArray_nested();
    testForEachIterable();
    testForEachIterable_typeVariablesAndIntersectionTypes();
  }
}
