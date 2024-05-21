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

import static javaemul.internal.InternalPreconditions.checkArrayCopyIndices;
import static javaemul.internal.InternalPreconditions.checkCriticalArrayBounds;

import java.util.Comparator;
import javaemul.internal.annotations.Wasm;

/** Provides utilities to perform operations on Arrays. */
public final class ArrayHelper {

  public static <T> T clone(T array, int fromIndex, int toIndex) {
    return (T) cloneImpl(array, fromIndex, toIndex);
  }

  public static <T> T unsafeClone(Object array, int fromIndex, int toIndex) {
    return (T) cloneImpl(array, fromIndex, toIndex);
  }

  private static Object cloneImpl(Object array, int fromIndex, int toIndex) {
    int newLength = toIndex - fromIndex;
    WasmArray wasmArray = asWasmArray(array);
    Object targetArray = wasmArray.newArray(newLength);
    int endIndex = Math.min(wasmArray.getLength(), toIndex);
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

  public static <T> T setLength(T array, int newLength) {
    return getLength(array) == newLength ? array : clone(array, 0, newLength);
  }

  /**
   * Resize the array to accommodate requested length. For Wasm, the size is increased in larger
   * chunks to amortize cost of growing similar to JavaScript.
   */
  public static <T> T grow(T array, int length) {
    return clone(array, 0, getNewCapacity(getLength(array), length));
  }

  public static int getNewCapacity(int originalCapacity, int requestedCapacity) {
    // Grow roughly with 1.5x rate at minimum.
    int minCapacity = originalCapacity + (originalCapacity >> 1) + 1;
    return Math.max(minCapacity, requestedCapacity);
  }

  public static void fill(int[] array, int value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(int[] array, int value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $int.array")
  private static native void nativeFill(int[] array, int offset, int value, int size);

  public static void fill(double[] array, double value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(double[] array, double value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $double.array")
  private static native void nativeFill(double[] array, int offset, double value, int size);

  public static void fill(float[] array, float value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(float[] array, float value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $float.array")
  private static native void nativeFill(float[] array, int offset, float value, int size);

  public static void fill(short[] array, short value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(short[] array, short value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $short.array")
  private static native void nativeFill(short[] array, int offset, short value, int size);

  public static void fill(long[] array, long value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(long[] array, long value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $long.array")
  private static native void nativeFill(long[] array, int offset, long value, int size);

  public static void fill(byte[] array, byte value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(byte[] array, byte value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $byte.array")
  private static native void nativeFill(byte[] array, int offset, byte value, int size);

  public static void fill(char[] array, char value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(char[] array, char value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $char.array")
  private static native void nativeFill(char[] array, int offset, char value, int size);

  public static void fill(boolean[] array, boolean value) {
    nativeFill(array, 0, value, array.length);
  }

  public static void fill(boolean[] array, boolean value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $boolean.array")
  private static native void nativeFill(boolean[] array, int offset, boolean value, int size);

  public static <T> void fill(T[] array, T value) {
    nativeFill(array, 0, value, array.length);
  }

  public static <T> void fill(T[] array, T value, int fromIndex, int toIndex) {
    nativeFill(array, fromIndex, value, toIndex - fromIndex);
  }

  @Wasm("array.fill $java.lang.Object.array")
  private static native void nativeFill(Object[] array, int offset, Object value, int size);

  public static void copy(Object array, int srcOfs, Object dest, int destOfs, int len) {
    checkArrayCopyIndices(array, srcOfs, dest, destOfs, len);
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
