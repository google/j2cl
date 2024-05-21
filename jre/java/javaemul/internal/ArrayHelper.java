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
package javaemul.internal;

import static javaemul.internal.InternalPreconditions.checkCriticalArrayBounds;
import static javaemul.internal.InternalPreconditions.checkCriticalArrayCopyIndices;

import java.util.Comparator;
import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Provides utilities to perform operations on Arrays. */
public final class ArrayHelper {

  public static final int ARRAY_PROCESS_BATCH_SIZE = 10000;

  public static <T> T clone(T array) {
    Object[] result = asNativeArray(array).slice();
    return (T) ArrayStamper.stampJavaTypeInfo(result, array);
  }

  public static <T> T clone(T array, int fromIndex, int toIndex) {
    Object[] result = unsafeClone(array, fromIndex, toIndex);
    // array.slice doesn't expand if toIndex > array.length
    int length = result.length;
    int requestedLength = toIndex - fromIndex;
    if (requestedLength > length) {
      Object initialValue = getElementInitialValue(array);
      if (initialValue == null) {
        setLength(result, requestedLength);
      } else {
        for (int i = length; i < requestedLength; ++i) {
          result[i] = initialValue;
        }
      }
    }
    return (T) ArrayStamper.stampJavaTypeInfo(result, array);
  }

  /**
   * Unlike clone, this method returns a copy of the array that is not type marked, nor size
   * guaranteed. This is only safe for temp arrays as returned array will not do any type checks.
   */
  public static Object[] unsafeClone(Object array, int fromIndex, int toIndex) {
    return asNativeArray(array).slice(fromIndex, toIndex);
  }

  public static <T> T[] createFrom(T[] array, int length) {
    return ArrayStamper.stampJavaTypeInfo(new NativeArray(length), array);
  }

  @JsMethod(name = "Array.isArray", namespace = JsPackage.GLOBAL)
  public static native boolean isArray(Object o);

  public static int getLength(Object array) {
    return asNativeArray(array).length;
  }

  /** Sets the length of an array to particular size. */
  public static <T> T setLength(T array, int length) {
    asNativeArray(array).length = length;
    return array;
  }

  /**
   * Resize the array to accommodate requested length. For JavaScript this is same as setting the
   * length.
   */
  public static <T> T grow(T array, int length) {
    return setLength(array, length);
  }

  public static void push(Object[] array, Object o) {
    asNativeArray(array).push(o);
  }

  public static void fill(Object array, @DoNotAutobox Object value, int fromIndex, int toIndex) {
    checkCriticalArrayBounds(fromIndex, toIndex, getLength(array));
    asNativeArray(array).fill(value, fromIndex, toIndex);
  }

  public static void fill(Object array, @DoNotAutobox Object value) {
    asNativeArray(array).fill(value);
  }

  public static void removeFrom(Object[] array, int index, int deleteCount) {
    asNativeArray(array).splice(index, deleteCount);
  }

  public static void insertTo(Object[] array, int index, Object value) {
    asNativeArray(array).splice(index, 0, value);
  }

  public static void copy(Object array, int srcOfs, Object dest, int destOfs, int len) {
    copy(
        JsUtils.<Object[]>uncheckedCast(array),
        srcOfs,
        JsUtils.<Object[]>uncheckedCast(dest),
        destOfs,
        len);
  }

  private static void copy(Object[] src, int srcOfs, Object[] dest, int destOfs, int len) {
    // Critical check since we may cause infinite loop below otherwise.
    checkCriticalArrayCopyIndices(src, srcOfs, dest, destOfs, len);

    if (len == 0) {
      return;
    }

    if (src == dest && srcOfs < destOfs) {
      // Reverse copy to handle overlap that would destroy values otherwise.
      srcOfs += len;
      for (int destEnd = destOfs + len; destEnd > destOfs; ) {
        dest[--destEnd] = src[--srcOfs];
      }
    } else {
      for (int destEnd = destOfs + len; destOfs < destEnd; ) {
        dest[destOfs++] = src[srcOfs++];
      }
    }
  }

  public static <T> T concat(T a, T b) {
    Object result = clone(a);
    setLength(result, getLength(a) + getLength(b));
    copy(b, 0, result, getLength(a), getLength(b));
    return (T) result;
  }

  public static boolean equals(double[] array1, double[] array2) {
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
      // Make sure we follow Double equality semantics (per spec of the method).
      if (!((Double) array1[i]).equals(array2[i])) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(float[] array1, float[] array2) {
    return equals(JsUtils.<double[]>uncheckedCast(array1), JsUtils.<double[]>uncheckedCast(array2));
  }

  public static int binarySearch(
      final double[] sortedArray, int fromIndex, int toIndex, final double key) {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final double midVal = sortedArray[mid];

      int cmp = Double.compare(midVal, key);
      if (cmp < 0) {
        low = mid + 1;
      } else if (cmp > 0) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  public static int binarySearch(
      final float[] sortedArray, int fromIndex, int toIndex, final float key) {
    return binarySearch(JsUtils.<double[]>uncheckedCast(sortedArray), fromIndex, toIndex, key);
  }

  public static void sortPrimitive(float[] array) {
    sortPrimitive(array, getDoubleComparator());
  }

  public static void sortPrimitive(double[] array) {
    sortPrimitive(array, getDoubleComparator());
  }

  public static void sortPrimitive(long[] array) {
    sortPrimitive(array, getLongComparator());
  }

  public static void sortPrimitive(Object array) {
    sortPrimitive(array, getIntComparator());
  }

  public static void sortPrimitive(float[] array, int fromIndex, int toIndex) {
    sortPrimitive(array, fromIndex, toIndex, getDoubleComparator());
  }

  public static void sortPrimitive(double[] array, int fromIndex, int toIndex) {
    sortPrimitive(array, fromIndex, toIndex, getDoubleComparator());
  }

  public static void sortPrimitive(long[] array, int fromIndex, int toIndex) {
    sortPrimitive(array, fromIndex, toIndex, getLongComparator());
  }

  public static void sortPrimitive(Object array, int fromIndex, int toIndex) {
    sortPrimitive(array, fromIndex, toIndex, getIntComparator());
  }

  /** Compare function for sort. */
  @JsFunction
  private interface CompareFunction {
    double compare(Object d1, Object d2);
  }

  private static void sortPrimitive(Object array, CompareFunction fn) {
    asNativeArray(array).sort(fn);
  }

  private static void sortPrimitive(Object array, int fromIndex, int toIndex, CompareFunction fn) {
    checkCriticalArrayBounds(fromIndex, toIndex, getLength(array));
    Object temp = ArrayHelper.unsafeClone(array, fromIndex, toIndex);
    sortPrimitive(temp, fn);
    copy(temp, 0, array, fromIndex, toIndex - fromIndex);
  }

  public static <T> void sort(T[] array, Comparator<? super T> c) {
    MergeSorter.sort(array, 0, array.length, c);
  }

  public static <T> void sort(T[] array, int fromIndex, int toIndex, Comparator<? super T> c) {
    checkCriticalArrayBounds(fromIndex, toIndex, array.length);
    MergeSorter.sort(array, fromIndex, toIndex, c);
  }

  @JsFunction
  private interface CompareDoubleFunction {
    double compare(double d1, double d2);
  }

  private static CompareFunction getIntComparator() {
    return JsUtils.uncheckedCast((CompareDoubleFunction) (a, b) -> a - b);
  }

  private static CompareFunction getDoubleComparator() {
    return JsUtils.uncheckedCast((CompareDoubleFunction) Double::compare);
  }

  @JsFunction
  private interface CompareLongFunction {
    @SuppressWarnings("unusable-by-js")
    int compare(long d1, long d2);
  }

  private static CompareFunction getLongComparator() {
    return JsUtils.uncheckedCast((CompareLongFunction) Long::compare);
  }

  private static NativeArray asNativeArray(Object array) {
    return JsUtils.uncheckedCast(array);
  }

  @JsMethod(namespace = "vmbootstrap.Arrays", name = "$getElementInitialValue")
  private static native Object getElementInitialValue(Object array);

  @JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
  private static class NativeArray {
    int length;

    NativeArray(int length) {}

    native void push(Object item);

    native Object[] slice();

    native Object[] slice(int fromIndex, int toIndex);

    native void fill(Object value);

    native void fill(Object value, int fromIndex, int toIndex);

    native void splice(int index, int deleteCount, Object... value);

    native <T> void sort(CompareFunction compareFunction);
  }

  private ArrayHelper() {}
}
