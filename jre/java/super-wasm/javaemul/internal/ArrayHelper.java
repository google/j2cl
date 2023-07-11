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

import java.util.Comparator;

/** Provides utilities to perform operations on Arrays. */
public final class ArrayHelper {

  public static <T> T clone(T array) {
    return (T) cloneImpl(array, 0, getLength(array));
  }

  public static <T> T clone(T array, int fromIndex, int toIndex) {
    return (T) cloneImpl(array, fromIndex, toIndex);
  }

  public static <T> T unsafeClone(Object array, int fromIndex, int toIndex) {
    return (T) cloneImpl(array, fromIndex, toIndex);
  }

  private static Object cloneImpl(Object array, int fromIndex, int toIndex) {
    int newLength = toIndex - fromIndex;
    Object targetArray = asWasmArray(array).newArray(newLength);
    int endIndex = Math.min(getLength(array), toIndex);
    copy(array, fromIndex, targetArray, 0, endIndex - fromIndex);
    return targetArray;
  }

  public static <T> T[] createFrom(T[] array, int length) {
    return (T[]) asWasmArray(array).newArray(length);
  }

  public static boolean isArray(Object o) {
    return o instanceof WasmArray;
  }

  public static int getLength(Object array) {
    return asWasmArray(array).getLength();
  }

  public static void setLength(Object array, int length) {
    asWasmArray(array).setLength(length);
  }

  public static void push(Object[] array, Object o) {
    ((WasmArray.OfObject) asWasmArray(array)).push(o);
  }

  public static void push(byte[] array, byte o) {
    ((WasmArray.OfByte) asWasmArray(array)).push(o);
  }

  public static void push(int[] array, int o) {
    ((WasmArray.OfInt) asWasmArray(array)).push(o);
  }

  public static void push(long[] array, long o) {
    ((WasmArray.OfLong) asWasmArray(array)).push(o);
  }

  public static void push(double[] array, double o) {
    ((WasmArray.OfDouble) asWasmArray(array)).push(o);
  }

  public static void fill(int[] array, int value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(int[] array, int value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static void fill(double[] array, double value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(double[] array, double value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static void fill(float[] array, float value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(float[] array, float value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static void fill(short[] array, short value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(short[] array, short value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static void fill(long[] array, long value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(long[] array, long value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static void fill(byte[] array, byte value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(byte[] array, byte value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static void fill(char[] array, char value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(char[] array, char value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static void fill(boolean[] array, boolean value) {
    fill(array, value, 0, getLength(array));
  }

  public static void fill(boolean[] array, boolean value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static <T> void fill(T[] array, T value) {
    fill(array, value, 0, getLength(array));
  }

  public static <T> void fill(T[] array, T value, int fromIndex, int toIndex) {
    for (int i = fromIndex; i < toIndex; i++) {
      array[i] = value;
    }
  }

  public static <T> T setAt(T[] array, int index, T value) {
    WasmArray wasmArray = asWasmArray(array);
    T originalValue;
    if (wasmArray.getLength() < index + 1) {
      wasmArray.setLength(index + 1);
      originalValue = null;
    } else {
      originalValue = array[index];
    }
    array[index] = value;
    return originalValue;
  }

  public static void removeFrom(Object[] array, int index, int deleteCount) {
    // Copy the items after deletion end, overwriting deleted items.
    int copyFrom = index + deleteCount;
    copy(array, copyFrom, array, index, array.length - copyFrom);
    // Trim the end array.
    setLength(array, array.length - deleteCount);
  }

  public static void insertTo(Object[] array, int index, Object value) {
    insertTo(array, index, new Object[] {value});
  }

  public static void insertTo(Object[] array, int index, Object[] values) {
    ((WasmArray.OfObject) asWasmArray(array))
        .insertFrom(index, (WasmArray.OfObject) asWasmArray(values));
  }

  public static void copy(Object array, int srcOfs, Object dest, int destOfs, int len) {
    asWasmArray(dest).copyFrom(destOfs, asWasmArray(array), srcOfs, len);
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
      if (Double.doubleToLongBits(array1[i]) != Double.doubleToLongBits(array2[i])) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(float[] array1, float[] array2) {
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
      // Make sure we follow Float equality semantics (per spec of the method).
      if (Float.floatToIntBits(array1[i]) != Float.floatToIntBits(array2[i])) {
        return false;
      }
    }

    return true;
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
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
      final float midVal = sortedArray[mid];

      int cmp = Float.compare(midVal, key);
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

  public static void sortPrimitive(Object array) {
    WasmArray wasmArray = asWasmArray(array);
    wasmArray.sort(0, wasmArray.getLength());
  }

  public static void sortPrimitive(Object array, int fromIndex, int toIndex) {
    asWasmArray(array).sort(fromIndex, toIndex);
  }

  public static <T> void sort(T[] array, Comparator<? super T> c) {
    MergeSorter.sort(array, 0, array.length, c);
  }

  public static <T> void sort(T[] array, int fromIndex, int toIndex, Comparator<? super T> c) {
    checkCriticalArrayBounds(fromIndex, toIndex, array.length);
    MergeSorter.sort(array, fromIndex, toIndex, c);
  }

  private static WasmArray asWasmArray(Object obj) {
    return (WasmArray) obj;
  }

  private ArrayHelper() {}
}
