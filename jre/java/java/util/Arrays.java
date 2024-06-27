/*
 * Copyright 2008 Google Inc.
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

package java.util;

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkArraySize;
import static javaemul.internal.InternalPreconditions.checkCriticalArrayBounds;
import static javaemul.internal.InternalPreconditions.checkElementIndex;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.isApiChecked;

import java.io.Serializable;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javaemul.internal.ArrayHelper;
import javaemul.internal.ArrayIterator;

/**
 * Utility methods related to native arrays. See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html">the official Java API
 * doc</a> for details.
 */
public class Arrays {

  private static final class ArrayList<E> extends AbstractList<E>
      implements RandomAccess, Serializable {

    /**
     * The only reason this is non-final is so that E[] (and E) will be exposed for serialization.
     */
    private E[] array;

    ArrayList(E[] array) {
      checkNotNull(array);
      this.array = array;
    }

    @Override
    public boolean contains(Object o) {
      return (indexOf(o) != -1);
    }

    @Override
    public void forEach(Consumer<? super E> consumer) {
      checkNotNull(consumer);
      for (E e : array) {
        consumer.accept(e);
      }
    }

    @Override
    public E get(int index) {
      checkElementIndex(index, size());
      return array[index];
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
      checkNotNull(operator);
      for (int i = 0; i < array.length; i++) {
        array[i] = operator.apply(array[i]);
      }
    }

    @Override
    public E set(int index, E value) {
      E was = get(index);
      array[index] = value;
      return was;
    }

    @Override
    public int size() {
      return array.length;
    }

    @Override
    public void sort(Comparator<? super E> c) {
      Arrays.sort(array, 0, array.length, c);
    }

    @Override
    public Object[] toArray() {
      return toArray(new Object[array.length]);
    }

    @Override
    public Iterator<E> iterator() {
      return new ArrayIterator<E>(array);
    }

    /*
     * Faster than the iterator-based implementation in AbstractCollection.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] out) {
      int size = array.length;
      if (out.length < size) {
        out = ArrayHelper.createFrom(out, size);
      }
      for (int i = 0; i < size; ++i) {
        out[i] = (T) array[i];
      }
      if (out.length > size) {
        out[size] = null;
      }
      return out;
    }
  }

  public static <T> List<T> asList(T... array) {
    return new ArrayList<T>(array);
  }

  /**
   * Perform a binary search on a sorted byte array.
   *
   * @param sortedArray byte array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(byte[] sortedArray, int fromIndex, int toIndex, byte key) {
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.length);
    return binarySearch0(sortedArray, fromIndex, toIndex, key);
  }

  public static int binarySearch(byte[] sortedArray, byte key) {
    return binarySearch0(sortedArray, 0, sortedArray.length, key);
  }

  private static int binarySearch0(
      final byte[] sortedArray, int fromIndex, int toIndex, final byte key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final byte midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted char array.
   *
   * @param sortedArray char array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(char[] sortedArray, int fromIndex, int toIndex, char key) {
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.length);
    return binarySearch0(sortedArray, fromIndex, toIndex, key);
  }

  public static int binarySearch(char[] sortedArray, char key) {
    return binarySearch0(sortedArray, 0, sortedArray.length, key);
  }

  private static int binarySearch0(
      final char[] sortedArray, int fromIndex, int toIndex, final char key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final char midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted double array.
   *
   * @param sortedArray double array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(double[] sortedArray, int fromIndex, int toIndex, double key) {
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.length);
    return ArrayHelper.binarySearch(sortedArray, fromIndex, toIndex, key);
  }

  public static int binarySearch(double[] sortedArray, double key) {
    return ArrayHelper.binarySearch(sortedArray, 0, sortedArray.length, key);
  }

  /**
   * Perform a binary search on a sorted float array.
   *
   * <p>Note that some underlying JavaScript interpreters do not actually implement floats (using
   * double instead), so you may get slightly different behavior regarding values that are very
   * close (or equal) since conversion errors to/from double may change the values slightly.
   *
   * @param sortedArray float array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(float[] sortedArray, int fromIndex, int toIndex, float key) {
    return ArrayHelper.binarySearch(sortedArray, fromIndex, toIndex, key);
  }

  public static int binarySearch(float[] sortedArray, float key) {
    return ArrayHelper.binarySearch(sortedArray, 0, sortedArray.length, key);
  }

  /**
   * Perform a binary search on a sorted int array.
   *
   * @param sortedArray int array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(int[] sortedArray, int fromIndex, int toIndex, int key) {
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.length);
    return binarySearch0(sortedArray, fromIndex, toIndex, key);
  }

  public static int binarySearch(int[] sortedArray, int key) {
    return binarySearch0(sortedArray, 0, sortedArray.length, key);
  }

  private static int binarySearch0(
      final int[] sortedArray, int fromIndex, int toIndex, final int key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final int midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted long array.
   *
   * <p>Note that most underlying JavaScript interpreters do not actually implement longs, so the
   * values must be stored in doubles instead. This means that certain legal values cannot be
   * represented, and comparison of two unequal long values may result in unexpected results if they
   * are not also representable as doubles.
   *
   * @param sortedArray long array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(long[] sortedArray, int fromIndex, int toIndex, long key) {
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.length);
    return binarySearch0(sortedArray, fromIndex, toIndex, key);
  }

  public static int binarySearch(long[] sortedArray, long key) {
    return binarySearch0(sortedArray, 0, sortedArray.length, key);
  }

  private static int binarySearch0(
      final long[] sortedArray, int fromIndex, int toIndex, final long key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final long midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted object array, using natural ordering.
   *
   * @param sortedArray object array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> is not comparable to <code>sortedArray</code>'s
   *     elements.
   */
  public static int binarySearch(Object[] sortedArray, int fromIndex, int toIndex, Object key) {
    return binarySearch(sortedArray, fromIndex, toIndex, key, null);
  }

  public static int binarySearch(Object[] sortedArray, Object key) {
    return binarySearch(sortedArray, key, null);
  }

  /**
   * Perform a binary search on a sorted short array.
   *
   * @param sortedArray short array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(short[] sortedArray, int fromIndex, int toIndex, short key) {
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.length);
    return binarySearch0(sortedArray, fromIndex, toIndex, key);
  }

  public static int binarySearch(short[] sortedArray, short key) {
    return binarySearch0(sortedArray, 0, sortedArray.length, key);
  }

  private static int binarySearch0(
      final short[] sortedArray, int fromIndex, int toIndex, final short key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final short midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted object array, using a user-specified comparison function.
   *
   * @param sortedArray object array to search
   * @param fromIndex index of the first element to search
   * @param toIndex index (exclusive) of the last element to search
   * @param key value to search for
   * @param comparator comparision function, <code>null</code> indicates <i>natural ordering</i>
   *     should be used.
   * @return the index of an element with a matching value, or a negative number which is the index
   *     of the next larger value (or just past the end of the array if the searched value is larger
   *     than all elements in the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> and <code>sortedArray</code>'s elements cannot
   *     be compared by <code>comparator</code>.
   */
  public static <T> int binarySearch(
      T[] sortedArray, int fromIndex, int toIndex, T key, Comparator<? super T> comparator) {
    checkCriticalArrayBounds(fromIndex, toIndex, sortedArray.length);
    return binarySearch0(sortedArray, fromIndex, toIndex, key, comparator);
  }

  public static <T> int binarySearch(T[] sortedArray, T key, Comparator<? super T> c) {
    return binarySearch0(sortedArray, 0, sortedArray.length, key, c);
  }

  private static <T> int binarySearch0(
      final T[] sortedArray,
      int fromIndex,
      int toIndex,
      final T key,
      Comparator<? super T> comparator) {
    comparator = Comparators.nullToNaturalOrder(comparator);
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final T midVal = sortedArray[mid];
      final int compareResult = comparator.compare(midVal, key);

      if (compareResult < 0) {
        low = mid + 1;
      } else if (compareResult > 0) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  public static boolean[] copyOf(boolean[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static byte[] copyOf(byte[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static char[] copyOf(char[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static double[] copyOf(double[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static float[] copyOf(float[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static int[] copyOf(int[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static long[] copyOf(long[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static short[] copyOf(short[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static <T> T[] copyOf(T[] original, int newLength) {
    return copyOfImpl(original, newLength);
  }

  public static boolean[] copyOfRange(boolean[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static byte[] copyOfRange(byte[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static char[] copyOfRange(char[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static double[] copyOfRange(double[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static float[] copyOfRange(float[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static int[] copyOfRange(int[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static long[] copyOfRange(long[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static short[] copyOfRange(short[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  public static <T> T[] copyOfRange(T[] original, int from, int to) {
    return copyOfRangeImpl(original, from, to);
  }

  private static <T> T copyOfImpl(T original, int newLength) {
    checkArraySize(newLength);
    return ArrayHelper.clone(original, 0, newLength);
  }

  private static <T> T copyOfRangeImpl(T original, int from, int to) {
    checkCopyOfRange(original, from, to);
    return ArrayHelper.clone(original, from, to);
  }

  private static void checkCopyOfRange(Object original, int from, int to) {
    // TODO(b/259858988): Workaround since string concat is currently side effectful in j2wasm.
    if (isApiChecked()) {
      checkArgument(from <= to, from + " > " + to);
    }
    int len = ArrayHelper.getLength(original);
    checkCriticalArrayBounds(from, from, len);
  }

  public static boolean deepEquals(Object[] a1, Object[] a2) {
    if (a1 == a2) {
      return true;
    }

    if (a1 == null || a2 == null) {
      return false;
    }

    if (a1.length != a2.length) {
      return false;
    }

    for (int i = 0, n = a1.length; i < n; ++i) {
      if (!Objects.deepEquals(a1[i], a2[i])) {
        return false;
      }
    }

    return true;
  }

  public static int deepHashCode(Object[] a) {
    if (a == null) {
      return 0;
    }

    int hashCode = 1;

    for (Object obj : a) {
      int hash;

      if (obj instanceof Object[]) {
        hash = deepHashCode((Object[]) obj);
      } else if (obj instanceof boolean[]) {
        hash = hashCode((boolean[]) obj);
      } else if (obj instanceof byte[]) {
        hash = hashCode((byte[]) obj);
      } else if (obj instanceof char[]) {
        hash = hashCode((char[]) obj);
      } else if (obj instanceof short[]) {
        hash = hashCode((short[]) obj);
      } else if (obj instanceof int[]) {
        hash = hashCode((int[]) obj);
      } else if (obj instanceof long[]) {
        hash = hashCode((long[]) obj);
      } else if (obj instanceof float[]) {
        hash = hashCode((float[]) obj);
      } else if (obj instanceof double[]) {
        hash = hashCode((double[]) obj);
      } else {
        hash = Objects.hashCode(obj);
      }

      hashCode = 31 * hashCode + hash;
    }

    return hashCode;
  }

  public static String deepToString(Object[] a) {
    return deepToString(a, new HashSet<Object[]>());
  }

  public static boolean equals(boolean[] array1, boolean[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(byte[] array1, byte[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(char[] array1, char[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(double[] array1, double[] array2) {
    return ArrayHelper.equals(array1, array2);
  }

  public static boolean equals(float[] array1, float[] array2) {
    return ArrayHelper.equals(array1, array2);
  }

  public static boolean equals(int[] array1, int[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(long[] array1, long[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(Object[] array1, Object[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      Object val1 = array1[i];
      Object val2 = array2[i];
      if (!Objects.equals(val1, val2)) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(short[] array1, short[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static void fill(boolean[] a, boolean val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(byte[] a, byte val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(char[] a, char val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(char[] a, int fromIndex, int toIndex, char val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(double[] a, double val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(double[] a, int fromIndex, int toIndex, double val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(float[] a, float val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(float[] a, int fromIndex, int toIndex, float val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(int[] a, int val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(int[] a, int fromIndex, int toIndex, int val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(long[] a, int fromIndex, int toIndex, long val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(long[] a, long val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(Object[] a, int fromIndex, int toIndex, Object val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(Object[] a, Object val) {
    ArrayHelper.fill(a, val);
  }

  public static void fill(short[] a, int fromIndex, int toIndex, short val) {
    ArrayHelper.fill(a, val, fromIndex, toIndex);
  }

  public static void fill(short[] a, short val) {
    ArrayHelper.fill(a, val);
  }

  public static int hashCode(boolean[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (boolean e : a) {
      hashCode = 31 * hashCode + Boolean.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(byte[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (byte e : a) {
      hashCode = 31 * hashCode + Byte.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(char[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (char e : a) {
      hashCode = 31 * hashCode + Character.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(double[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (double e : a) {
      hashCode = 31 * hashCode + Double.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(float[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (float e : a) {
      hashCode = 31 * hashCode + Float.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(int[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int e : a) {
      hashCode = 31 * hashCode + Integer.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(long[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (long e : a) {
      hashCode = 31 * hashCode + Long.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(Object[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (Object e : a) {
      hashCode = 31 * hashCode + Objects.hashCode(e);
    }
    return hashCode;
  }

  public static int hashCode(short[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (short e : a) {
      hashCode = 31 * hashCode + Short.hashCode(e);
    }
    return hashCode;
  }

  public static void parallelPrefix(double[] array, DoubleBinaryOperator op) {
    parallelPrefix0(array, 0, array.length, op);
  }

  public static void parallelPrefix(
      double[] array, int fromIndex, int toIndex, DoubleBinaryOperator op) {
    checkCriticalArrayBounds(fromIndex, toIndex, array.length);
    parallelPrefix0(array, fromIndex, toIndex, op);
  }

  private static void parallelPrefix0(
      double[] array, int fromIndex, int toIndex, DoubleBinaryOperator op) {
    checkNotNull(op);
    double acc = array[fromIndex];
    for (int i = fromIndex + 1; i < toIndex; i++) {
      array[i] = acc = op.applyAsDouble(acc, array[i]);
    }
  }

  public static void parallelPrefix(int[] array, IntBinaryOperator op) {
    parallelPrefix0(array, 0, array.length, op);
  }

  public static void parallelPrefix(int[] array, int fromIndex, int toIndex, IntBinaryOperator op) {
    checkCriticalArrayBounds(fromIndex, toIndex, array.length);
    parallelPrefix0(array, fromIndex, toIndex, op);
  }

  private static void parallelPrefix0(
      int[] array, int fromIndex, int toIndex, IntBinaryOperator op) {
    checkNotNull(op);
    int acc = array[fromIndex];
    for (int i = fromIndex + 1; i < toIndex; i++) {
      array[i] = acc = op.applyAsInt(acc, array[i]);
    }
  }

  public static void parallelPrefix(long[] array, LongBinaryOperator op) {
    parallelPrefix0(array, 0, array.length, op);
  }

  public static void parallelPrefix(
      long[] array, int fromIndex, int toIndex, LongBinaryOperator op) {
    checkCriticalArrayBounds(fromIndex, toIndex, array.length);
    parallelPrefix0(array, fromIndex, toIndex, op);
  }

  private static void parallelPrefix0(
      long[] array, int fromIndex, int toIndex, LongBinaryOperator op) {
    checkNotNull(op);
    long acc = array[fromIndex];
    for (int i = fromIndex + 1; i < toIndex; i++) {
      array[i] = acc = op.applyAsLong(acc, array[i]);
    }
  }

  public static <T> void parallelPrefix(T[] array, BinaryOperator<T> op) {
    parallelPrefix0(array, 0, array.length, op);
  }

  public static <T> void parallelPrefix(
      T[] array, int fromIndex, int toIndex, BinaryOperator<T> op) {
    checkCriticalArrayBounds(fromIndex, toIndex, array.length);
    parallelPrefix0(array, fromIndex, toIndex, op);
  }

  private static <T> void parallelPrefix0(
      T[] array, int fromIndex, int toIndex, BinaryOperator<T> op) {
    checkNotNull(op);
    T acc = array[fromIndex];
    for (int i = fromIndex + 1; i < toIndex; i++) {
      array[i] = acc = op.apply(acc, array[i]);
    }
  }

  public static <T> void setAll(T[] array, IntFunction<? extends T> generator) {
    checkNotNull(generator);
    for (int i = 0; i < array.length; i++) {
      array[i] = generator.apply(i);
    }
  }

  public static void setAll(double[] array, IntToDoubleFunction generator) {
    checkNotNull(generator);
    for (int i = 0; i < array.length; i++) {
      array[i] = generator.applyAsDouble(i);
    }
  }

  public static void setAll(int[] array, IntUnaryOperator generator) {
    checkNotNull(generator);
    for (int i = 0; i < array.length; i++) {
      array[i] = generator.applyAsInt(i);
    }
  }

  public static void setAll(long[] array, IntToLongFunction generator) {
    checkNotNull(generator);
    for (int i = 0; i < array.length; i++) {
      array[i] = generator.applyAsLong(i);
    }
  }

  public static <T> void parallelSetAll(T[] array, IntFunction<? extends T> generator) {
    setAll(array, generator);
  }

  public static void parallelSetAll(double[] array, IntToDoubleFunction generator) {
    setAll(array, generator);
  }

  public static void parallelSetAll(int[] array, IntUnaryOperator generator) {
    setAll(array, generator);
  }

  public static void parallelSetAll(long[] array, IntToLongFunction generator) {
    setAll(array, generator);
  }

  public static void sort(byte[] array) {
    ArrayHelper.sortPrimitive(array);
  }

  public static void sort(byte[] array, int fromIndex, int toIndex) {
    ArrayHelper.sortPrimitive(array, fromIndex, toIndex);
  }

  public static void sort(char[] array) {
    ArrayHelper.sortPrimitive(array);
  }

  public static void sort(char[] array, int fromIndex, int toIndex) {
    ArrayHelper.sortPrimitive(array, fromIndex, toIndex);
  }

  public static void sort(double[] array) {
    ArrayHelper.sortPrimitive(array);
  }

  public static void sort(double[] array, int fromIndex, int toIndex) {
    ArrayHelper.sortPrimitive(array, fromIndex, toIndex);
  }

  public static void sort(float[] array) {
    ArrayHelper.sortPrimitive(array);
  }

  public static void sort(float[] array, int fromIndex, int toIndex) {
    ArrayHelper.sortPrimitive(array, fromIndex, toIndex);
  }

  public static void sort(short[] array) {
    ArrayHelper.sortPrimitive(array);
  }

  public static void sort(short[] array, int fromIndex, int toIndex) {
    ArrayHelper.sortPrimitive(array, fromIndex, toIndex);
  }

  public static void sort(int[] array) {
    ArrayHelper.sortPrimitive(array);
  }

  public static void sort(int[] array, int fromIndex, int toIndex) {
    ArrayHelper.sortPrimitive(array, fromIndex, toIndex);
  }

  public static void sort(long[] array) {
    ArrayHelper.sortPrimitive(array);
  }

  public static void sort(long[] array, int fromIndex, int toIndex) {
    ArrayHelper.sortPrimitive(array, fromIndex, toIndex);
  }

  public static void sort(Object[] array) {
    sort(array, null);
  }

  public static void sort(Object[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex, null);
  }

  public static <T> void sort(T[] x, Comparator<? super T> c) {
    ArrayHelper.sort(x, Comparators.nullToNaturalOrder(c));
  }

  public static <T> void sort(T[] x, int fromIndex, int toIndex, Comparator<? super T> c) {
    ArrayHelper.sort(x, fromIndex, toIndex, Comparators.nullToNaturalOrder(c));
  }

  public static void parallelSort(byte[] array) {
    sort(array);
  }

  public static void parallelSort(byte[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static void parallelSort(char[] array) {
    sort(array);
  }

  public static void parallelSort(char[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static void parallelSort(double[] array) {
    sort(array);
  }

  public static void parallelSort(double[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static void parallelSort(float[] array) {
    sort(array);
  }

  public static void parallelSort(float[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static void parallelSort(int[] array) {
    sort(array);
  }

  public static void parallelSort(int[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static void parallelSort(long[] array) {
    sort(array);
  }

  public static void parallelSort(long[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static void parallelSort(short[] array) {
    sort(array);
  }

  public static void parallelSort(short[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static <T extends Comparable<? super T>> void parallelSort(T[] array) {
    sort(array);
  }

  public static <T> void parallelSort(T[] array, Comparator<? super T> c) {
    sort(array, c);
  }

  public static <T extends Comparable<? super T>> void parallelSort(
      T[] array, int fromIndex, int toIndex) {
    sort(array, fromIndex, toIndex);
  }

  public static <T> void parallelSort(
      T[] array, int fromIndex, int toIndex, Comparator<? super T> c) {
    sort(array, fromIndex, toIndex, c);
  }

  public static Spliterator.OfDouble spliterator(double[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfDouble spliterator(
      double[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(
        array, startInclusive, endExclusive, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfInt spliterator(int[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfInt spliterator(int[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(
        array, startInclusive, endExclusive, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfLong spliterator(long[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfLong spliterator(long[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(
        array, startInclusive, endExclusive, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static <T> Spliterator<T> spliterator(T[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static <T> Spliterator<T> spliterator(T[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(
        array, startInclusive, endExclusive, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static DoubleStream stream(double[] array) {
    return stream(array, 0, array.length);
  }

  public static DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
    return StreamSupport.doubleStream(spliterator(array, startInclusive, endExclusive), false);
  }

  public static IntStream stream(int[] array) {
    return stream(array, 0, array.length);
  }

  public static IntStream stream(int[] array, int startInclusive, int endExclusive) {
    return StreamSupport.intStream(spliterator(array, startInclusive, endExclusive), false);
  }

  public static LongStream stream(long[] array) {
    return stream(array, 0, array.length);
  }

  public static LongStream stream(long[] array, int startInclusive, int endExclusive) {
    return StreamSupport.longStream(spliterator(array, startInclusive, endExclusive), false);
  }

  public static <T> Stream<T> stream(T[] array) {
    return stream(array, 0, array.length);
  }

  public static <T> Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
    return StreamSupport.stream(spliterator(array, startInclusive, endExclusive), false);
  }

  public static String toString(boolean[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (boolean element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(byte[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (byte element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(char[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (char element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(double[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (double element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(float[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (float element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(int[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (int element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(long[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (long element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(Object[] x) {
    if (x == null) {
      return "null";
    }

    return Arrays.asList(x).toString();
  }

  public static String toString(short[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (short element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  /** Recursive helper function for {@link Arrays#deepToString(Object[])}. */
  private static String deepToString(Object[] a, Set<Object[]> arraysIveSeen) {
    if (a == null) {
      return "null";
    }

    if (!arraysIveSeen.add(a)) {
      return "[...]";
    }

    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (Object obj : a) {
      if (obj != null && obj.getClass().isArray()) {
        if (obj instanceof Object[]) {
          if (arraysIveSeen.contains(obj)) {
            joiner.add("[...]");
          } else {
            Object[] objArray = (Object[]) obj;
            HashSet<Object[]> tempSet = new HashSet<Object[]>(arraysIveSeen);
            joiner.add(deepToString(objArray, tempSet));
          }
        } else if (obj instanceof boolean[]) {
          joiner.add(toString((boolean[]) obj));
        } else if (obj instanceof byte[]) {
          joiner.add(toString((byte[]) obj));
        } else if (obj instanceof char[]) {
          joiner.add(toString((char[]) obj));
        } else if (obj instanceof short[]) {
          joiner.add(toString((short[]) obj));
        } else if (obj instanceof int[]) {
          joiner.add(toString((int[]) obj));
        } else if (obj instanceof long[]) {
          joiner.add(toString((long[]) obj));
        } else if (obj instanceof float[]) {
          joiner.add(toString((float[]) obj));
        } else if (obj instanceof double[]) {
          joiner.add(toString((double[]) obj));
        } else {
          assert false : "Unexpected array type: " + obj.getClass().getName();
        }
      } else {
        joiner.add(String.valueOf(obj));
      }
    }
    return joiner.toString();
  }

  private Arrays() {}
}
