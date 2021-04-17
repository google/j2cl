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


/** Provides utilities to perform operations on Arrays. */
public final class ArrayHelper {

  public static <T> T[] clone(T[] array, int fromIndex, int toIndex) {
    return (T[]) cloneImpl(array, fromIndex, toIndex);
  }

  public static <T> T unsafeClone(Object array, int fromIndex, int toIndex) {
    return (T) cloneImpl(array, fromIndex, toIndex);
  }

  private static Object cloneImpl(Object array, int fromIndex, int toIndex) {
    int newLength = toIndex - fromIndex;
    Object targetArray = asWasmArray(array).newArray(newLength);
    copy(array, 0, targetArray, 0, newLength);
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

  public static void removeFrom(Object[] array, int index, int deleteCount) {
    // Copy the items after deletion end, overwriting deleted items.
    int copyFrom = index + deleteCount;
    copy(array, index, array, copyFrom, array.length - copyFrom);
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

  private static WasmArray asWasmArray(Object obj) {
    return (WasmArray) obj;
  }

  /** Compare function for sort. */
  public interface CompareFunction {
    double compare(Object d1, Object d2);
  }

  public static void sort(Object array, CompareFunction fn) {
    throw new UnsupportedOperationException();
  }

  private ArrayHelper() {}
}

