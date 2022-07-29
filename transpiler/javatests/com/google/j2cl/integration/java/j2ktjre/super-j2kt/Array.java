/*
 * Copyright 2022 Google Inc.
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
package j2ktjre;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Array {
  static class ExampleObject implements Comparable<ExampleObject> {
    public int val;

    ExampleObject(int val) {
      this.val = val;
    }

    public int compareTo(ExampleObject other) {
      return this.val - other.val;
    }
  }

  static class ExampleObjectComparator implements Comparator<ExampleObject> {
    public int compare(ExampleObject o1, ExampleObject o2) {
      return o1.val - o2.val;
    }
  }

  static void testArrays() {
    byte b1 = 1, b2 = 2, b3 = 3, b4 = 4;
    short s1 = 1, s2 = 2, s3 = 3, s4 = 4;
    ExampleObject obj1 = new ExampleObject(1);
    ExampleObject obj2 = new ExampleObject(2);
    ExampleObject obj3 = new ExampleObject(3);
    ExampleObject obj4 = new ExampleObject(4);

    byte[] byteArray1 = {b1, b2, b3, b4};
    char[] charArray1 = {'a', 'b', 'c', 'd'};
    double[] doubleArray1 = {1, 2, 3, 4};
    float[] floatArray1 = {1F, 2F, 3F, 4F};
    int[] intArray1 = {1, 2, 3, 4};
    long[] longArray1 = {1L, 2L, 3L, 4L};
    short[] shortArray1 = {s1, s2, s3, s4};
    ExampleObject[] objArray1 = {obj1, obj2, obj3, obj4};

    Integer[] integerArray = {1, 2, 3, 4};
    List<Integer> intList1 = Arrays.asList(integerArray);
    assertEquals((Integer) 1, intList1.get(0));
    assertEquals((Integer) 4, intList1.get(3));

    List<Integer> intList2 = Arrays.asList(new Integer[] {1, 2, 3, 4});
    assertEquals((Integer) 1, intList2.get(0));
    assertEquals((Integer) 4, intList2.get(3));

    List<Integer> intList3 = Arrays.asList(1, 2, 3, 4);
    assertEquals((Integer) 1, intList3.get(0));
    assertEquals((Integer) 4, intList3.get(3));

    // TODO(b/239034072): Add tests for set on lists after varargs are fixed

    assertEquals(2, Arrays.binarySearch(byteArray1, b3));
    assertEquals(-3, Arrays.binarySearch(byteArray1, 0, 2, b3));

    assertEquals(2, Arrays.binarySearch(charArray1, 'c'));
    assertEquals(-3, Arrays.binarySearch(charArray1, 0, 2, 'c'));

    assertEquals(2, Arrays.binarySearch(doubleArray1, 3));
    assertEquals(-3, Arrays.binarySearch(doubleArray1, 0, 2, 3));

    assertEquals(2, Arrays.binarySearch(floatArray1, 3F));
    assertEquals(-3, Arrays.binarySearch(floatArray1, 0, 2, 3F));

    assertEquals(2, Arrays.binarySearch(intArray1, 3));
    assertEquals(-3, Arrays.binarySearch(intArray1, 0, 2, 3));

    assertEquals(2, Arrays.binarySearch(longArray1, 3L));
    assertEquals(-3, Arrays.binarySearch(longArray1, 0, 2, 3L));

    assertEquals(2, Arrays.binarySearch(shortArray1, s3));
    assertEquals(-3, Arrays.binarySearch(shortArray1, 0, 2, s3));

    assertEquals(2, Arrays.binarySearch(objArray1, obj3));
    assertEquals(-3, Arrays.binarySearch(objArray1, 0, 2, obj3));

    ExampleObject[] objArrayAlias = objArray1;

    assertEquals(2, Arrays.binarySearch(objArrayAlias, obj3, new ExampleObjectComparator()));
    assertEquals(-3, Arrays.binarySearch(objArrayAlias, 0, 2, obj3, new ExampleObjectComparator()));

    assertEquals(2, Arrays.binarySearch(objArrayAlias, obj3, null));

    boolean[] boolArray2 = {true, false, true, false};
    byte[] byteArray2 = {b4, b3, b1, b2};
    char[] charArray2 = {'d', 'c', 'a', 'b'};
    double[] doubleArray2 = {4, 3, 1, 2};
    float[] floatArray2 = {4F, 3F, 1F, 2F};
    int[] intArray2 = {4, 3, 1, 2};
    long[] longArray2 = {4L, 3L, 1L, 2L};
    short[] shortArray2 = {s4, s3, s1, s2};
    ExampleObject[] objArray2 = {obj4, obj3, obj1, obj2};

    boolean[] boolCopy = Arrays.copyOf(boolArray2, 3);
    assertEquals(3, boolCopy.length);
    assertEquals(true, boolCopy[0]);
    assertEquals(true, boolCopy[2]);

    byte[] byteCopy = Arrays.copyOf(byteArray2, 3);
    assertEquals(3, byteCopy.length);
    assertEquals(b4, byteCopy[0]);
    assertEquals(b1, byteCopy[2]);

    char[] charCopy = Arrays.copyOf(charArray2, 3);
    assertEquals(3, charCopy.length);
    assertEquals('d', charCopy[0]);
    assertEquals('a', charCopy[2]);

    double[] doubleCopy = Arrays.copyOf(doubleArray2, 3);
    assertEquals(3, doubleCopy.length);
    assertEquals(4.0, doubleCopy[0]);
    assertEquals(1.0, doubleCopy[2]);

    float[] floatCopy = Arrays.copyOf(floatArray2, 3);
    assertEquals(3, floatCopy.length);
    assertEquals(4F, floatCopy[0]);
    assertEquals(1F, floatCopy[2]);

    int[] intCopy = Arrays.copyOf(intArray2, 3);
    assertEquals(3, intCopy.length);
    assertEquals(4, intCopy[0]);
    assertEquals(1, intCopy[2]);

    long[] longCopy = Arrays.copyOf(longArray2, 3);
    assertEquals(3, longCopy.length);
    assertEquals(4L, longCopy[0]);
    assertEquals(1L, longCopy[2]);

    short[] shortCopy = Arrays.copyOf(shortArray2, 3);
    assertEquals(3, shortCopy.length);
    assertEquals(s4, shortCopy[0]);
    assertEquals(s1, shortCopy[2]);

    ExampleObject[] objCopy = Arrays.copyOf(objArray2, 3);
    assertEquals(3, objCopy.length);
    assertEquals(objCopy[0], obj4);
    assertEquals(objCopy[2], obj1);

    boolCopy = Arrays.copyOfRange(boolArray2, 0, 3);
    assertEquals(3, boolCopy.length);
    assertEquals(true, boolCopy[0]);
    assertEquals(true, boolCopy[2]);

    byteCopy = Arrays.copyOfRange(byteArray2, 0, 3);
    assertEquals(3, byteCopy.length);
    assertEquals(b4, byteCopy[0]);
    assertEquals(b1, byteCopy[2]);

    charCopy = Arrays.copyOfRange(charArray2, 0, 3);
    assertEquals(3, charCopy.length);
    assertEquals('d', charCopy[0]);
    assertEquals('a', charCopy[2]);

    doubleCopy = Arrays.copyOfRange(doubleArray2, 0, 3);
    assertEquals(3, doubleCopy.length);
    assertEquals(4.0, doubleCopy[0]);
    assertEquals(1.0, doubleCopy[2]);

    floatCopy = Arrays.copyOfRange(floatArray2, 0, 3);
    assertEquals(3, floatCopy.length);
    assertEquals(4F, floatCopy[0]);
    assertEquals(1F, floatCopy[2]);

    intCopy = Arrays.copyOfRange(intArray2, 0, 3);
    assertEquals(3, intCopy.length);
    assertEquals(4, intCopy[0]);
    assertEquals(1, intCopy[2]);

    longCopy = Arrays.copyOfRange(longArray2, 0, 3);
    assertEquals(3, longCopy.length);
    assertEquals(4L, longCopy[0]);
    assertEquals(1L, longCopy[2]);

    shortCopy = Arrays.copyOfRange(shortArray2, 0, 3);
    assertEquals(3, shortCopy.length);
    assertEquals(s4, shortCopy[0]);
    assertEquals(s1, shortCopy[2]);

    objCopy = Arrays.copyOfRange(objArray2, 0, 3);
    assertEquals(3, objCopy.length);
    assertEquals(obj4, objCopy[0]);
    assertEquals(obj1, objCopy[2]);

    boolean[] boolArray3 = {true, false, true, false};
    byte[] byteArray3 = {b4, b3, b1, b2};
    char[] charArray3 = {'d', 'c', 'a', 'b'};
    double[] doubleArray3 = {4, 3, 1, 2};
    float[] floatArray3 = {4F, 3F, 1F, 2F};
    int[] intArray3 = {4, 3, 1, 2};
    long[] longArray3 = {4L, 3L, 1L, 2L};
    short[] shortArray3 = {s4, s3, s1, s2};
    ExampleObject[] objArray3 = {obj4, obj3, obj1, obj2};

    assertTrue(Arrays.equals(boolArray2, boolArray3));
    assertTrue(Arrays.equals(byteArray2, byteArray3));
    assertTrue(Arrays.equals(charArray2, charArray3));
    assertTrue(Arrays.equals(doubleArray2, doubleArray3));
    assertTrue(Arrays.equals(floatArray2, floatArray3));
    assertTrue(Arrays.equals(intArray2, intArray3));
    assertTrue(Arrays.equals(longArray2, longArray3));
    assertTrue(Arrays.equals(shortArray2, shortArray3));
    assertTrue(Arrays.equals(objArray2, objArray3));

    assertTrue(Arrays.deepEquals(objArray2, objArray3));
    assertFalse(Arrays.deepEquals(objArray1, objArray2));

    assertEquals(Arrays.deepToString(objArray2), Arrays.deepToString(objArray3));

    assertEquals(Arrays.deepHashCode(objArray2), Arrays.deepHashCode(objArray3));

    assertEquals(Arrays.hashCode(boolArray2), Arrays.hashCode(boolArray3));
    assertEquals(Arrays.hashCode(byteArray2), Arrays.hashCode(byteArray3));
    assertEquals(Arrays.hashCode(charArray2), Arrays.hashCode(charArray3));
    assertEquals(Arrays.hashCode(doubleArray2), Arrays.hashCode(doubleArray3));
    assertEquals(Arrays.hashCode(floatArray2), Arrays.hashCode(floatArray3));
    assertEquals(Arrays.hashCode(intArray2), Arrays.hashCode(intArray3));
    assertEquals(Arrays.hashCode(longArray2), Arrays.hashCode(longArray3));
    assertEquals(Arrays.hashCode(shortArray2), Arrays.hashCode(shortArray3));
    assertEquals(Arrays.hashCode(objArray2), Arrays.hashCode(objArray3));

    boolean[] boolFillArr1 = {true, false, true, false};
    Arrays.fill(boolFillArr1, true);
    assertEquals(true, boolFillArr1[0]);
    assertEquals(true, boolFillArr1[3]);

    boolean[] boolFillArr2 = {true, false, true, false};
    Arrays.fill(boolFillArr2, 0, 2, false);
    assertEquals(false, boolFillArr2[0]);
    assertEquals(true, boolFillArr2[2]);

    byte[] byteFillArr1 = {b4, b3, b1, b2};
    Arrays.fill(byteFillArr1, b1);
    assertEquals(b1, byteFillArr1[0]);
    assertEquals(b1, byteFillArr1[3]);

    byte[] byteFillArr2 = {b4, b3, b1, b2};
    Arrays.fill(byteFillArr2, 0, 2, b1);
    assertEquals(b1, byteFillArr2[0]);
    assertEquals(b2, byteFillArr2[3]);

    char[] charFillArr1 = {'d', 'c', 'a', 'b'};
    Arrays.fill(charFillArr1, 'a');
    assertEquals('a', charFillArr1[0]);
    assertEquals('a', charFillArr1[3]);

    char[] charFillArr2 = {'d', 'c', 'a', 'b'};
    Arrays.fill(charFillArr2, 0, 2, 'c');
    assertEquals('c', charFillArr2[0]);
    assertEquals('b', charFillArr2[3]);

    double[] doubleFillArr1 = {4, 3, 1, 2};
    Arrays.fill(doubleFillArr1, 1);
    assertEquals(1.0, doubleFillArr1[0]);
    assertEquals(1.0, doubleFillArr1[3]);

    double[] doubleFillArr2 = {4, 3, 1, 2};
    Arrays.fill(doubleFillArr2, 0, 2, 3);
    assertEquals(3.0, doubleFillArr2[0]);
    assertEquals(2.0, doubleFillArr2[3]);

    float[] floatFillArr1 = {4F, 3F, 1F, 2F};
    Arrays.fill(floatFillArr1, 1F);
    assertEquals(1F, floatFillArr1[0]);
    assertEquals(1F, floatFillArr1[3]);

    float[] floatFillArr2 = {4F, 3F, 1F, 2F};
    Arrays.fill(floatFillArr2, 0, 2, 3F);
    assertEquals(3F, floatFillArr2[0]);
    assertEquals(2F, floatFillArr2[3]);

    int[] intFillArr1 = {4, 3, 1, 2};
    Arrays.fill(intFillArr1, 1);
    assertEquals(1, intFillArr1[0]);
    assertEquals(1, intFillArr1[3]);

    int[] intFillArr2 = {4, 3, 1, 2};
    Arrays.fill(intFillArr2, 0, 2, 3);
    assertEquals(3, intFillArr2[0]);
    assertEquals(2, intFillArr2[3]);

    long[] longFillArr1 = {4L, 3L, 1L, 2L};
    Arrays.fill(longFillArr1, 1L);
    assertEquals(1L, longFillArr1[0]);
    assertEquals(1L, longFillArr1[3]);

    long[] longFillArr2 = {4L, 3L, 1L, 2L};
    Arrays.fill(longFillArr2, 0, 2, 3L);
    assertEquals(3L, longFillArr2[0]);
    assertEquals(2L, longFillArr2[3]);

    short[] shortFillArr1 = {s4, s3, s1, s2};
    Arrays.fill(shortFillArr1, s1);
    assertEquals(s1, shortFillArr1[0]);
    assertEquals(s1, shortFillArr1[3]);

    short[] shortFillArr2 = {s4, s3, s1, s2};
    Arrays.fill(shortFillArr2, 0, 2, s1);
    assertEquals(s1, shortFillArr2[0]);
    assertEquals(s2, shortFillArr2[3]);

    ExampleObject[] objFillArr1 = {obj4, obj3, obj1, obj2};
    Arrays.fill(objFillArr1, obj1);
    assertEquals(obj1, objFillArr1[0]);
    assertEquals(obj1, objFillArr1[3]);

    ExampleObject[] objFillArr2 = {obj4, obj3, obj1, obj2};
    Arrays.fill(objFillArr2, 0, 2, obj1);
    assertEquals(obj1, objFillArr2[0]);
    assertEquals(obj2, objFillArr2[3]);

    assertEquals("[true, false, true, false]", Arrays.toString(boolArray2));
    assertEquals("[4, 3, 1, 2]", Arrays.toString(byteArray2));
    assertEquals("[d, c, a, b]", Arrays.toString(charArray2));
    assertTrue(
        Arrays.toString(doubleArray2).equals("[4.0, 3.0, 1.0, 2.0]")
            || Arrays.toString(doubleArray2).equals("[4, 3, 1, 2]"));
    assertTrue(
        Arrays.toString(floatArray2).equals("[4.0, 3.0, 1.0, 2.0]")
            || Arrays.toString(floatArray2).equals("[4, 3, 1, 2]"));
    assertEquals("[4, 3, 1, 2]", Arrays.toString(intArray2));
    assertEquals("[4, 3, 1, 2]", Arrays.toString(longArray2));
    assertEquals("[4, 3, 1, 2]", Arrays.toString(shortArray2));
    assertEquals(Arrays.toString(objArray2), Arrays.toString(objArray3));

    double[] doublePrefixArr1 = {1, 2, 3, 4};
    double[] doublePrefixArr2 = {1, 2, 3, 4};
    Arrays.parallelPrefix(
        doublePrefixArr1,
        (x, y) -> {
          return x + y;
        });
    assertEquals(1.0, doublePrefixArr1[0]);
    assertEquals(10.0, doublePrefixArr1[3]);
    Arrays.parallelPrefix(
        doublePrefixArr2,
        1,
        4,
        (x, y) -> {
          return x + y;
        });
    assertEquals(1.0, doublePrefixArr2[0]);
    assertEquals(9.0, doublePrefixArr2[3]);

    int[] intPrefixArr1 = {1, 2, 3, 4};
    int[] intPrefixArr2 = {1, 2, 3, 4};
    Arrays.parallelPrefix(
        intPrefixArr1,
        (x, y) -> {
          return x + y;
        });
    assertEquals(1, intPrefixArr1[0]);
    assertEquals(10, intPrefixArr1[3]);
    Arrays.parallelPrefix(
        intPrefixArr2,
        1,
        4,
        (x, y) -> {
          return x + y;
        });
    assertEquals(1, intPrefixArr2[0]);
    assertEquals(9, intPrefixArr2[3]);

    long[] longPrefixArr1 = {1L, 2L, 3L, 4L};
    long[] longPrefixArr2 = {1L, 2L, 3L, 4L};
    Arrays.parallelPrefix(
        longPrefixArr1,
        (x, y) -> {
          return x + y;
        });
    assertEquals(1L, longPrefixArr1[0]);
    assertEquals(10L, longPrefixArr1[3]);
    Arrays.parallelPrefix(
        longPrefixArr2,
        1,
        4,
        (x, y) -> {
          return x + y;
        });
    assertEquals(1L, longPrefixArr2[0]);
    assertEquals(9L, longPrefixArr2[3]);

    String[] strPrefixArr1 = {"a", "b", "c", "d"};
    String[] strPrefixArr2 = {"a", "b", "c", "d"};
    Arrays.parallelPrefix(
        strPrefixArr1,
        (x, y) -> {
          return x + y;
        });
    assertEquals("a", strPrefixArr1[0]);
    assertEquals("abcd", strPrefixArr1[3]);
    Arrays.parallelPrefix(
        strPrefixArr2,
        1,
        4,
        (x, y) -> {
          return x + y;
        });
    assertEquals("a", strPrefixArr2[0]);
    assertEquals("bcd", strPrefixArr2[3]);

    int[] intArray4 = {1, 2, 3, 4};
    Arrays.setAll(
        intArray4,
        i -> {
          return (i > 2) ? i : 0;
        });
    assertEquals(0, intArray4[0]);
    assertEquals(3, intArray4[3]);

    double[] doubleArray4 = {1, 2, 3, 4};
    Arrays.setAll(
        doubleArray4,
        d -> {
          return (d > 2) ? d : 0;
        });
    assertEquals(0.0, doubleArray4[0]);
    assertEquals(3.0, doubleArray4[3]);

    long[] longArray4 = {1L, 2L, 3L, 4L};
    Arrays.setAll(
        longArray4,
        l -> {
          return (l > 2) ? l : 0;
        });
    assertEquals(0L, longArray4[0]);
    assertEquals(3L, longArray4[3]);

    String[] strArray4 = {"1", "1", "10", "10"};
    Arrays.setAll(
        strArray4,
        s -> {
          return (s > 1) ? Integer.toString(s) : "5";
        });
    assertEquals("5", strArray4[0]);
    assertEquals("3", strArray4[3]);

    int[] intArray5 = {1, 2, 3, 4};
    Arrays.parallelSetAll(
        intArray5,
        i -> {
          return (i > 2) ? i : 0;
        });
    assertEquals(0, intArray5[0]);
    assertEquals(3, intArray5[3]);

    double[] doubleArray5 = {1, 2, 3, 4};
    Arrays.parallelSetAll(
        doubleArray5,
        d -> {
          return (d > 2) ? d : 0;
        });
    assertEquals(0.0, doubleArray5[0]);
    assertEquals(3.0, doubleArray5[3]);

    long[] longArray5 = {1L, 2L, 3L, 4L};
    Arrays.parallelSetAll(
        longArray5,
        l -> {
          return (l > 2) ? l : 0;
        });
    assertEquals(0L, longArray5[0]);
    assertEquals(3L, longArray5[3]);

    String[] strArray5 = {"1", "1", "10", "10"};
    Arrays.parallelSetAll(
        strArray5,
        s -> {
          return (s > 1) ? Integer.toString(s) : "5";
        });
    assertEquals("5", strArray5[0]);
    assertEquals("3", strArray5[3]);

    byte[] byteUnsortedArr1 = {b4, b3, b1, b2};
    Arrays.sort(byteUnsortedArr1, 1, 3);
    assertEquals(b4, byteUnsortedArr1[0]);
    assertEquals(b1, byteUnsortedArr1[1]);
    Arrays.sort(byteUnsortedArr1);
    assertEquals(b1, byteUnsortedArr1[0]);
    assertEquals(b4, byteUnsortedArr1[3]);

    char[] charUnsortedArr1 = {'d', 'c', 'a', 'b'};
    Arrays.sort(charUnsortedArr1, 1, 3);
    assertEquals('d', charUnsortedArr1[0]);
    assertEquals('a', charUnsortedArr1[1]);
    Arrays.sort(charUnsortedArr1);
    assertEquals('a', charUnsortedArr1[0]);
    assertEquals('d', charUnsortedArr1[3]);

    double[] doubleUnsortedArr1 = {4, 3, 1, 2};
    Arrays.sort(doubleUnsortedArr1, 1, 3);
    assertEquals(4.0, doubleUnsortedArr1[0]);
    assertEquals(1.0, doubleUnsortedArr1[1]);
    Arrays.sort(doubleUnsortedArr1);
    assertEquals(1.0, doubleUnsortedArr1[0]);
    assertEquals(4.0, doubleUnsortedArr1[3]);

    float[] floatUnsortedArr1 = {4F, 3F, 1F, 2F};
    Arrays.sort(floatUnsortedArr1, 1, 3);
    assertEquals(4F, floatUnsortedArr1[0]);
    assertEquals(1F, floatUnsortedArr1[1]);
    Arrays.sort(floatUnsortedArr1);
    assertEquals(1F, floatUnsortedArr1[0]);
    assertEquals(4F, floatUnsortedArr1[3]);

    int[] intUnsortedArr1 = {4, 3, 1, 2};
    Arrays.sort(intUnsortedArr1, 1, 3);
    assertEquals(4, intUnsortedArr1[0]);
    assertEquals(1, intUnsortedArr1[1]);
    Arrays.sort(intUnsortedArr1);
    assertEquals(1, intUnsortedArr1[0]);
    assertEquals(4, intUnsortedArr1[3]);

    long[] longUnsortedArr1 = {4L, 3L, 1L, 2L};
    Arrays.sort(longUnsortedArr1, 1, 3);
    assertEquals(4L, longUnsortedArr1[0]);
    assertEquals(1L, longUnsortedArr1[1]);
    Arrays.sort(longUnsortedArr1);
    assertEquals(1L, longUnsortedArr1[0]);
    assertEquals(4L, longUnsortedArr1[3]);

    short[] shortUnsortedArr1 = {s4, s3, s1, s2};
    Arrays.sort(shortUnsortedArr1, 1, 3);
    assertEquals(s4, shortUnsortedArr1[0]);
    assertEquals(s1, shortUnsortedArr1[1]);
    Arrays.sort(shortUnsortedArr1);
    assertEquals(s1, shortUnsortedArr1[0]);
    assertEquals(s4, shortUnsortedArr1[3]);

    ExampleObject[] objUnsortedArr1 = {obj4, obj3, obj1, obj2};
    Arrays.parallelSort(objUnsortedArr1, 1, 3, new ExampleObjectComparator());
    assertEquals(obj4, objUnsortedArr1[0]);
    assertEquals(obj1, objUnsortedArr1[1]);
    Arrays.parallelSort(objUnsortedArr1, new ExampleObjectComparator());
    assertEquals(obj1, objUnsortedArr1[0]);
    assertEquals(obj4, objUnsortedArr1[3]);

    byte[] byteUnsortedArr2 = {b4, b3, b1, b2};
    Arrays.parallelSort(byteUnsortedArr2, 1, 3);
    assertEquals(b4, byteUnsortedArr2[0]);
    assertEquals(b1, byteUnsortedArr2[1]);
    Arrays.parallelSort(byteUnsortedArr2);
    assertEquals(b1, byteUnsortedArr2[0]);
    assertEquals(b4, byteUnsortedArr2[3]);

    char[] charUnsortedArr2 = {'d', 'c', 'a', 'b'};
    Arrays.parallelSort(charUnsortedArr2, 1, 3);
    assertEquals('d', charUnsortedArr2[0]);
    assertEquals('a', charUnsortedArr2[1]);
    Arrays.parallelSort(charUnsortedArr2);
    assertEquals('a', charUnsortedArr2[0]);
    assertEquals('d', charUnsortedArr2[3]);

    double[] doubleUnsortedArr2 = {4, 3, 1, 2};
    Arrays.parallelSort(doubleUnsortedArr2, 1, 3);
    assertEquals(4.0, doubleUnsortedArr2[0]);
    assertEquals(1.0, doubleUnsortedArr2[1]);
    Arrays.parallelSort(doubleUnsortedArr2);
    assertEquals(1.0, doubleUnsortedArr2[0]);
    assertEquals(4.0, doubleUnsortedArr2[3]);

    float[] floatUnsortedArr2 = {4F, 3F, 1F, 2F};
    Arrays.parallelSort(floatUnsortedArr2, 1, 3);
    assertEquals(4F, floatUnsortedArr2[0]);
    assertEquals(1F, floatUnsortedArr2[1]);
    Arrays.parallelSort(floatUnsortedArr2);
    assertEquals(1F, floatUnsortedArr2[0]);
    assertEquals(4F, floatUnsortedArr2[3]);

    int[] intUnsortedArr2 = {4, 3, 1, 2};
    Arrays.parallelSort(intUnsortedArr2, 1, 3);
    assertEquals(4, intUnsortedArr2[0]);
    assertEquals(1, intUnsortedArr2[1]);
    Arrays.parallelSort(intUnsortedArr2);
    assertEquals(1, intUnsortedArr2[0]);
    assertEquals(4, intUnsortedArr2[3]);

    long[] longUnsortedArr2 = {4L, 3L, 1L, 2L};
    Arrays.parallelSort(longUnsortedArr2, 1, 3);
    assertEquals(4L, longUnsortedArr2[0]);
    assertEquals(1L, longUnsortedArr2[1]);
    Arrays.parallelSort(longUnsortedArr2);
    assertEquals(1L, longUnsortedArr2[0]);
    assertEquals(4L, longUnsortedArr2[3]);

    short[] shortUnsortedArr2 = {s4, s3, s1, s2};
    Arrays.parallelSort(shortUnsortedArr2, 1, 3);
    assertEquals(s4, shortUnsortedArr2[0]);
    assertEquals(s1, shortUnsortedArr2[1]);
    Arrays.parallelSort(shortUnsortedArr2);
    assertEquals(s1, shortUnsortedArr2[0]);
    assertEquals(s4, shortUnsortedArr2[3]);

    ExampleObject[] objUnsortedArr2 = {obj4, obj3, obj1, obj2};
    Arrays.parallelSort(objUnsortedArr2, 1, 3, new ExampleObjectComparator());
    assertEquals(obj4, objUnsortedArr2[0]);
    assertEquals(obj1, objUnsortedArr2[1]);
    Arrays.parallelSort(objUnsortedArr2, new ExampleObjectComparator());
    assertEquals(obj1, objUnsortedArr2[0]);
    assertEquals(obj4, objUnsortedArr2[3]);
  }
}
