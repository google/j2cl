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
package com.google.j2cl.transpiler.integration.foreachstatement;

import java.util.Iterator;

/**
 * Test for for loops.
 */
public class Main {

  static class MyIterable implements Iterable<String> {
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        private int i = 5;

        public boolean hasNext() {
          return i >= 0;
        }

        public String next() {
          return "" + i--;
        }

        public void remove() {}
      };
    }
  }

  public static void main(String... args) {

    int[] array = new int[] {5, 4, 3, 2, 1, 0};
    int j = 5;
    int lastSeenInt = -1;
    for (int i : array) {
      assert i == j : "Seen:<" + i + "> Expected:<" + j + ">";
      j--;
      lastSeenInt = i;
    }

    assert lastSeenInt == 0 : "LastSeen:<" + lastSeenInt + "> should be zero";

    String lastSeenString = "";
    j = 5;
    for (String s : new MyIterable()) {

      assert s == "" + j : "Seen:<" + s + "> Expected:<" + j + ">";
      j--;
      lastSeenString = s;
    }
    assert lastSeenString == "0" : "LastSeen:<" + lastSeenString + "> should be zero";

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
        assert k == value(i0, k0) : "Seen:<" + k + "> Expected:<" + value(i0, k0) + ">";
        k0++;
      }
      i0++;
    }
  }

  private static int value(int i, int j) {
    return i * 10 + j;
  }
}
