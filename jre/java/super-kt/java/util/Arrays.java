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

import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import javaemul.internal.annotations.KtNative;

/**
 * Utility methods related to native arrays. See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html">the official Java API
 * doc</a> for details.
 */
@KtNative("java.util.Arrays") // Reimplemented in Kotlin, not transpiled.
public class Arrays {

  public static native <T> List<T> asList(T... array);

  public static native int binarySearch(byte[] sortedArray, int fromIndex, int toIndex, byte key);

  public static native int binarySearch(byte[] sortedArray, byte key);

  public static native int binarySearch(char[] sortedArray, int fromIndex, int toIndex, char key);

  public static native int binarySearch(char[] sortedArray, char key);

  public static native int binarySearch(
      double[] sortedArray, int fromIndex, int toIndex, double key);

  public static native int binarySearch(double[] sortedArray, double key);

  public static native int binarySearch(float[] sortedArray, int fromIndex, int toIndex, float key);

  public static native int binarySearch(float[] sortedArray, float key);

  public static native int binarySearch(int[] sortedArray, int fromIndex, int toIndex, int key);

  public static native int binarySearch(int[] sortedArray, int key);

  public static native int binarySearch(long[] sortedArray, int fromIndex, int toIndex, long key);

  public static native int binarySearch(long[] sortedArray, long key);

  public static native int binarySearch(
      Object[] sortedArray, int fromIndex, int toIndex, Object key);

  public static native int binarySearch(Object[] sortedArray, Object key);

  public static native int binarySearch(short[] sortedArray, int fromIndex, int toIndex, short key);

  public static native int binarySearch(short[] sortedArray, short key);

  public static native <T> int binarySearch(
      T[] sortedArray, int fromIndex, int toIndex, T key, Comparator<? super T> comparator);

  public static native <T> int binarySearch(T[] sortedArray, T key, Comparator<? super T> c);

  public static native boolean[] copyOf(boolean[] original, int newLength);

  public static native byte[] copyOf(byte[] original, int newLength);

  public static native char[] copyOf(char[] original, int newLength);

  public static native double[] copyOf(double[] original, int newLength);

  public static native float[] copyOf(float[] original, int newLength);

  public static native int[] copyOf(int[] original, int newLength);

  public static native long[] copyOf(long[] original, int newLength);

  public static native short[] copyOf(short[] original, int newLength);

  public static native <T> T[] copyOf(T[] original, int newLength);

  public static native boolean[] copyOfRange(boolean[] original, int from, int to);

  public static native byte[] copyOfRange(byte[] original, int from, int to);

  public static native char[] copyOfRange(char[] original, int from, int to);

  public static native double[] copyOfRange(double[] original, int from, int to);

  public static native float[] copyOfRange(float[] original, int from, int to);

  public static native int[] copyOfRange(int[] original, int from, int to);

  public static native long[] copyOfRange(long[] original, int from, int to);

  public static native short[] copyOfRange(short[] original, int from, int to);

  public static native <T> T[] copyOfRange(T[] original, int from, int to);

  public static native boolean deepEquals(Object[] a1, Object[] a2);

  public static native int deepHashCode(Object[] a);

  public static native String deepToString(Object[] a);

  public static native boolean equals(boolean[] array1, boolean[] array2);

  public static native boolean equals(byte[] array1, byte[] array2);

  public static native boolean equals(char[] array1, char[] array2);

  public static native boolean equals(double[] array1, double[] array2);

  public static native boolean equals(float[] array1, float[] array2);

  public static native boolean equals(int[] array1, int[] array2);

  public static native boolean equals(long[] array1, long[] array2);

  public static native boolean equals(Object[] array1, Object[] array2);

  public static native boolean equals(short[] array1, short[] array2);

  public static native void fill(boolean[] a, boolean val);

  public static native void fill(boolean[] a, int fromIndex, int toIndex, boolean val);

  public static native void fill(byte[] a, byte val);

  public static native void fill(byte[] a, int fromIndex, int toIndex, byte val);

  public static native void fill(char[] a, char val);

  public static native void fill(char[] a, int fromIndex, int toIndex, char val);

  public static native void fill(double[] a, double val);

  public static native void fill(double[] a, int fromIndex, int toIndex, double val);

  public static native void fill(float[] a, float val);

  public static native void fill(float[] a, int fromIndex, int toIndex, float val);

  public static native void fill(int[] a, int val);

  public static native void fill(int[] a, int fromIndex, int toIndex, int val);

  public static native void fill(long[] a, long val);

  public static native void fill(long[] a, int fromIndex, int toIndex, long val);

  public static native void fill(Object[] a, int fromIndex, int toIndex, Object val);

  public static native void fill(Object[] a, Object val);

  public static native void fill(short[] a, short val);

  public static native void fill(short[] a, int fromIndex, int toIndex, short val);

  public static native int hashCode(boolean[] a);

  public static native int hashCode(byte[] a);

  public static native int hashCode(char[] a);

  public static native int hashCode(double[] a);

  public static native int hashCode(float[] a);

  public static native int hashCode(int[] a);

  public static native int hashCode(long[] a);

  public static native int hashCode(Object[] a);

  public static native int hashCode(short[] a);

  public static native void parallelPrefix(double[] array, DoubleBinaryOperator op);

  public static native void parallelPrefix(
      double[] array, int fromIndex, int toIndex, DoubleBinaryOperator op);

  public static native void parallelPrefix(int[] array, IntBinaryOperator op);

  public static native void parallelPrefix(
      int[] array, int fromIndex, int toIndex, IntBinaryOperator op);

  public static native void parallelPrefix(long[] array, LongBinaryOperator op);

  public static native void parallelPrefix(
      long[] array, int fromIndex, int toIndex, LongBinaryOperator op);

  public static native <T> void parallelPrefix(T[] array, BinaryOperator<T> op);

  public static native <T> void parallelPrefix(
      T[] array, int fromIndex, int toIndex, BinaryOperator<T> op);

  public static native <T> void setAll(T[] array, IntFunction<? extends T> generator);

  public static native void setAll(double[] array, IntToDoubleFunction generator);

  public static native void setAll(int[] array, IntUnaryOperator generator);

  public static native void setAll(long[] array, IntToLongFunction generator);

  public static native <T> void parallelSetAll(T[] array, IntFunction<? extends T> generator);

  public static native void parallelSetAll(double[] array, IntToDoubleFunction generator);

  public static native void parallelSetAll(int[] array, IntUnaryOperator generator);

  public static native void parallelSetAll(long[] array, IntToLongFunction generator);

  public static native void sort(byte[] array);

  public static native void sort(byte[] array, int fromIndex, int toIndex);

  public static native void sort(char[] array);

  public static native void sort(char[] array, int fromIndex, int toIndex);

  public static native void sort(double[] array);

  public static native void sort(double[] array, int fromIndex, int toIndex);

  public static native void sort(float[] array);

  public static native void sort(float[] array, int fromIndex, int toIndex);

  public static native void sort(int[] array);

  public static native void sort(int[] array, int fromIndex, int toIndex);

  public static native void sort(long[] array);

  public static native void sort(long[] array, int fromIndex, int toIndex);

  public static native void sort(Object[] array);

  public static native void sort(Object[] array, int fromIndex, int toIndex);

  public static native void sort(short[] array);

  public static native void sort(short[] array, int fromIndex, int toIndex);

  public static native <T> void sort(T[] x, Comparator<? super T> c);

  public static native <T> void sort(T[] x, int fromIndex, int toIndex, Comparator<? super T> c);

  public static native void parallelSort(byte[] array);

  public static native void parallelSort(byte[] array, int fromIndex, int toIndex);

  public static native void parallelSort(char[] array);

  public static native void parallelSort(char[] array, int fromIndex, int toIndex);

  public static native void parallelSort(double[] array);

  public static native void parallelSort(double[] array, int fromIndex, int toIndex);

  public static native void parallelSort(float[] array);

  public static native void parallelSort(float[] array, int fromIndex, int toIndex);

  public static native void parallelSort(int[] array);

  public static native void parallelSort(int[] array, int fromIndex, int toIndex);

  public static native void parallelSort(long[] array);

  public static native void parallelSort(long[] array, int fromIndex, int toIndex);

  public static native void parallelSort(short[] array);

  public static native void parallelSort(short[] array, int fromIndex, int toIndex);

  public static native <T extends Comparable<? super T>> void parallelSort(T[] array);

  public static native <T> void parallelSort(T[] array, Comparator<? super T> c);

  public static native <T extends Comparable<? super T>> void parallelSort(
      T[] array, int fromIndex, int toIndex);

  public static native <T> void parallelSort(
      T[] array, int fromIndex, int toIndex, Comparator<? super T> c);

  public static native String toString(boolean[] a);

  public static native String toString(byte[] a);

  public static native String toString(char[] a);

  public static native String toString(double[] a);

  public static native String toString(float[] a);

  public static native String toString(int[] a);

  public static native String toString(long[] a);

  public static native String toString(Object[] x);

  public static native String toString(short[] a);

  private Arrays() { }
}
