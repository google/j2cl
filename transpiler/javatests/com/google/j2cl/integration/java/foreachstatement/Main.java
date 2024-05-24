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
package foreachstatement;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Test for for loops.
 */
public class Main {
  public static void main(String... args) {
    testForEachArray();
    testForEachArray_nested();
    testForEachArray_boxed();
    testForEachArray_unboxed();
    testForEachIterable();
    testForEachIterable_typeVariable();
    testForEachIterable_intersection();
    testForEachIterable_union();
    testForEachIterable_widening();
  }

  static class MyIterable implements Iterable<Integer>, Serializable {
    class MyIterator implements Iterator<Integer> {
      private int i = 5;

      @Override
      public boolean hasNext() {
        return i >= 1;
      }

      @Override
      public Integer next() {
        return i--;
      }

      @Override
      public void remove() {}
    }

    @Override
    public Iterator<Integer> iterator() {
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

    assertTrue("LastSeen:<" + lastSeenInt + "> should be one", lastSeenInt == 1);
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

  private static void testForEachArray_boxed() {
    String concatName = "";
    for (Integer i : new int[] {1, 2, 3}) {
      concatName += i.getClass().getSimpleName();
    }
    assertEquals("IntegerIntegerInteger", concatName);
  }

  private static void testForEachArray_unboxed() {
    int sum = 0;
    for (int i : new Integer[] {1, 2, 3}) {
      sum += i;
    }
    assertEquals(6, sum);
  }

  private static int value(int i, int j) {
    return i * 10 + j;
  }

  private static void testForEachIterable() {
    Integer lastSeenInteger = -1;
    int j = 5;
    for (Integer s : new MyIterable()) {
      assertTrue("Seen:<" + s + "> Expected:<" + j + ">", s.intValue() == j);
      j--;
      lastSeenInteger = s;
    }
    assertTrue("LastSeen:<" + lastSeenInteger + "> should be one", lastSeenInteger.intValue() == 1);
  }

  private static <T, C extends Iterable<T>> void testForEachIterable_typeVariable() {
    C iterable =
        (C)
            new Iterable<T>() {
              private Integer[] elements = new Integer[] {1, 2, 3};

              @Override
              public Iterator<T> iterator() {
                return new Iterator<T>() {
                  int index = 0;

                  @Override
                  public boolean hasNext() {
                    return index < elements.length;
                  }

                  @Override
                  public T next() {
                    return (T) elements[index++];
                  }
                };
              }
            };

    String onetwothree = "";
    for (T e : iterable) {
      onetwothree += e.toString();
    }
    assertEquals("123", onetwothree);
  }

  private static <T extends MyIterable & Serializable, S extends T>
      void testForEachIterable_intersection() {
    S iterable = (S) new MyIterable();
    Integer lastSeenInteger = -1;
    int j = 5;
    for (Integer i : iterable) {
      assertTrue("Seen:<" + i + "> Expected:<" + j + ">", i.intValue() == j);
      j--;
      lastSeenInteger = i;
    }
    assertTrue("LastSeen:<" + lastSeenInteger + "> should be one", lastSeenInteger.intValue() == 1);
  }

  static class Exception1 extends Exception implements Iterable<Number> {
    Number[] numbers = {1, 2, 3};

    @Override
    public Iterator<Number> iterator() {
      return Arrays.asList(numbers).iterator();
    }
  }

  static class Exception2 extends Exception implements Iterable<Integer> {
    Integer[] numbers = {4, 5, 6};

    @Override
    public Iterator<Integer> iterator() {
      return Arrays.asList(numbers).iterator();
    }
  }

  static class Exception3 extends Exception implements Iterable<Integer> {
    Integer[] numbers = {7, 8, 9};

    @Override
    public Iterator<Integer> iterator() {
      return Arrays.asList(numbers).iterator();
    }
  }

  private static void testForEachIterable_union() {
    try {
      if (true) {
        throw new Exception1();
      } else {
        throw new Exception2();
      }
    } catch (Exception1 | Exception2 e) {
      int start = 1;
      // No common iterable element, but it can be assumed to be assignable to Number.
      for (Number n : e) {
        assertTrue(n.intValue() == start++);
      }
    }

    try {
      if (true) {
        throw new Exception2();
      } else {
        throw new Exception3();
      }
    } catch (Exception2 | Exception3 e) {
      int start = 4;
      // Common iterable element that is assignable to int.
      for (int n : e) {
        assertTrue(n == start++);
      }
    }
  }

  private static <T extends MyIterable, S extends T> void testForEachIterable_widening() {
    double lastSeenDouble = -1;
    int j = 5;
    for (double d : new MyIterable()) {
      assertTrue("Seen:<" + d + "> Expected:<" + j + ">", d == j);
      j--;
      lastSeenDouble = d;
    }
    assertTrue("LastSeen:<" + lastSeenDouble + "> should be one", lastSeenDouble == 1);

    // Repro for b/342207724.
    S iterable = (S) new MyIterable();
    long lastSeenLong = -1;
    j = 5;
    for (long l : iterable) {
      assertTrue("Seen:<" + l + "> Expected:<" + j + ">", l == j);
      j--;
      lastSeenLong = l;
    }
    assertTrue("LastSeen:<" + lastSeenLong + "> should be one", lastSeenLong == 1);
  }
}
