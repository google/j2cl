/*
 * Copyright 2022 Google Inc.
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

import java.util.Iterator;

// TODO(b/237266658): Reove all unsupported functions.
/** Helper for java.lang.reflect.Array */
public final class ArrayHelper {

  private ArrayHelper() {}

  public static final int ARRAY_PROCESS_BATCH_SIZE = 10000;

  public static <T> T[] clone(T[] array) {
    throw new UnsupportedOperationException();
  }

  public static <T> T[] clone(T[] array, int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  public static Object[] unsafeClone(Object array, int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  public static <T> T[] createFrom(T[] array, int length) {
    throw new UnsupportedOperationException();
  }

  public static boolean isArray(Object o) {
    throw new UnsupportedOperationException();
  }

  public static int getLength(Object array) {
    if (array instanceof Object[]) {
      return ((Object[]) array).length;
    }

    if (array instanceof boolean[]) {
      return ((boolean[]) array).length;
    }

    if (array instanceof byte[]) {
      return ((byte[]) array).length;
    }

    if (array instanceof char[]) {
      return ((char[]) array).length;
    }

    if (array instanceof short[]) {
      return ((short[]) array).length;
    }

    if (array instanceof int[]) {
      return ((int[]) array).length;
    }

    if (array instanceof long[]) {
      return ((long[]) array).length;
    }

    if (array instanceof float[]) {
      return ((float[]) array).length;
    }

    if (array instanceof double[]) {
      return ((double[]) array).length;
    }

    if (array == null) {
      throw new NullPointerException();
    }
    throw new IllegalArgumentException("Not an array");
  }

  public static void setLength(Object array, int length) {
    throw new UnsupportedOperationException();
  }

  public static void push(Object array, Object o) {
    throw new UnsupportedOperationException();
  }

  public static <T> T setAt(T[] array, int index, T value) {
    throw new UnsupportedOperationException();
  }

  public static void removeFrom(Object[] array, int index, int deleteCount) {
    throw new UnsupportedOperationException();
  }

  public static void insertTo(Object[] array, int index, Object value) {
    throw new UnsupportedOperationException();
  }

  public static void insertTo(Object[] array, int index, Object[] values) {
    throw new UnsupportedOperationException();
  }

  public static void copy(Object array, int srcOfs, Object dest, int destOfs, int len) {
    throw new UnsupportedOperationException();
  }

  /** TODO(b/237266658): Remove */
  public interface CompareFunction {
    double compare(Object d1, Object d2);
  }

  public static void sort(Object array, CompareFunction fn) {
    throw new UnsupportedOperationException();
  }

  public static <T> Iterator<T> arrayIterator(T[] array) {
    throw new UnsupportedOperationException();
  }
}
