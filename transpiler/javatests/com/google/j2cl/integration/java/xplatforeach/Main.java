/*
 * Copyright 2024 Google Inc.
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
package xplatforeach;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import com.google.apps.docs.xplat.collections.JsArrayInteger;
import com.google.apps.docs.xplat.collections.SerializedJsArray;
import com.google.apps.docs.xplat.collections.SerializedJsMap;
import com.google.apps.docs.xplat.collections.UnsafeJsMap;
import com.google.apps.docs.xplat.collections.UnsafeJsMapInteger;
import com.google.apps.docs.xplat.collections.UnsafeJsSet;
import com.google.apps.docs.xplat.structs.SparseArray;
import com.google.gwt.corp.collections.ImmutableJsArray;
import com.google.gwt.corp.collections.JsArray;
import com.google.gwt.corp.collections.UnmodifiableJsArray;
import jsinterop.annotations.JsMethod;

public class Main {
  public static void main(String... args) {
    testJsArray();
    testImmutableJsArray();
    testUnmodifiableJsArray();
    testJsArrayInteger();
    testSerializedJsArray();
    testSerializedJsMap();
    testUnsafeJsMap();
    testUnsafeJsMapInteger();
    testUnsafeJsSet();
    testSparseArray();
  }

  private static void testJsArray() {
    JsArray<String> array = createArrayLike("a", "b", "c");
    String[] actual = new String[3];
    int i = 0;
    for (String element : array.getIterable()) {
      actual[i++] = element;
    }
    assertEquals(new String[] {"a", "b", "c"}, actual);
  }

  private static void testImmutableJsArray() {
    ImmutableJsArray<String> array = createArrayLike("a", "b", "c");
    String[] actual = new String[3];
    int i = 0;
    for (String element : array.getIterable()) {
      actual[i++] = element;
    }
    assertEquals(new String[] {"a", "b", "c"}, actual);
  }

  private static void testUnmodifiableJsArray() {
    UnmodifiableJsArray<String> array = createArrayLike("a", "b", "c");
    String[] actual = new String[3];
    int i = 0;
    for (String element : array.getIterable()) {
      actual[i++] = element;
    }
    assertEquals(new String[] {"a", "b", "c"}, actual);
  }

  private static void testJsArrayInteger() {
    JsArrayInteger array = createArrayLike();
    setAt(array, 0, 1);
    setAt(array, 1, 2);
    setAt(array, 2, 3);

    int[] actualInts = new int[3];
    int i = 0;
    for (int element : array.getIterable()) {
      actualInts[i++] = element;
    }
    assertEquals(new int[] {1, 2, 3}, actualInts);

    i = 0;
    Integer[] actualIntegers = new Integer[3];
    for (Integer element : array.getIterable()) {
      actualIntegers[i++] = element;
    }
    assertEquals(new Integer[] {1, 2, 3}, actualIntegers);
  }

  private static void testSerializedJsArray() {
    SerializedJsArray array = createArrayLike();
    setAt(array, 0, "a");
    setAt(array, 1, 2);
    setAt(array, 2, 3.3);

    Object[] actual = new Object[3];
    int i = 0;
    for (Object element : array.getIterable()) {
      actual[i++] = element;
    }
    assertEquals(new Object[] {"a", 2d, 3.3}, actual);
  }

  private static void testSerializedJsMap() {
    SerializedJsMap map = new SerializedJsMap();
    set(map, "a", 1);
    set(map, "b", 2);
    set(map, "c", 3);

    String[] actual = new String[3];
    int i = 0;
    for (String key : map.getIterableKeys()) {
      actual[i++] = key;
    }

    assertEquals(new String[] {"a", "b", "c"}, actual);
  }

  private static void testUnsafeJsMap() {
    UnsafeJsMap<?> map = new UnsafeJsMap<Object>();
    set(map, "a", 1);
    set(map, "b", 2);
    set(map, "c", 3);

    String[] actual = new String[3];
    int i = 0;
    for (String key : map.getIterableKeys()) {
      actual[i++] = key;
    }

    assertEquals(new String[] {"a", "b", "c"}, actual);
  }

  private static void testUnsafeJsMapInteger() {
    UnsafeJsMapInteger map = new UnsafeJsMapInteger();
    set(map, "a", 1);
    set(map, "b", 2);
    set(map, "c", 3);

    String[] actual = new String[3];
    int i = 0;
    for (String key : map.getIterableKeys()) {
      actual[i++] = key;
    }

    assertEquals(new String[] {"a", "b", "c"}, actual);
  }

  private static void testUnsafeJsSet() {
    UnsafeJsSet map = new UnsafeJsSet();
    set(map, "a", true);
    set(map, "b", false);
    set(map, "c", true);

    String[] actual = new String[3];
    int i = 0;
    for (String key : map.getIterableKeys()) {
      actual[i++] = key;
    }

    assertEquals(new String[] {"a", "b", "c"}, actual);
  }

  private static void testSparseArray() {
    SparseArray<String> array = createArrayLike();
    setAt(array, 2, "a");
    setAt(array, 4, "b");
    setAt(array, 8, "c");

    int i = 0;
    int[] actualIntKeys = new int[3];
    for (int key : array.getIterableKeys()) {
      actualIntKeys[i++] = key;
    }
    assertEquals(new int[] {2, 4, 8}, actualIntKeys);

    i = 0;
    int[] actualIntegerKeys = new int[3];
    for (Integer key : array.getIterableKeys()) {
      actualIntegerKeys[i++] = key;
    }
    assertEquals(new int[] {2, 4, 8}, actualIntegerKeys);
  }

  @SuppressWarnings("unchecked")
  private static <T> T createArrayLike(Object... values) {
    return (T) values;
  }

  @JsMethod(name = "set")
  private static native void setAt(Object arr, int index, Object value);

  @JsMethod(name = "set")
  private static native void setAt(Object arr, int index, int value);

  @JsMethod
  private static native void set(Object obj, String property, Object value);

  @JsMethod
  private static native void set(Object obj, String property, int value);
}
