/*
 * Copyright 2017 Google Inc.
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
package array;

import java.io.Serializable;
import javaemul.internal.annotations.KtDisabled;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Arrays {
  public void testObjectArray() {
    // Creation
    Object[] objects = new Object[100];
    objects = new Object[0];
    objects = new Object[] {null, null};
    Object[][] objects2d = new Object[5][10];
    objects2d = new Object[][] {{null, null}, null};
    objects2d = new Object[20][];
    Object[][] arrayLiteral2d = {{null, null}, null};

    // Access
    Object object = objects[0];
    object = objects2d[0][1];

    // Assignment
    objects[0] = null;
    objects2d[0][1] = null;
  }

  private class SomeObject {}

  public void testJavaTypeArray() {
    // Creation
    SomeObject[] objects = new SomeObject[100];
    objects = new SomeObject[0];
    objects = new SomeObject[] {null, null};
    SomeObject[][] objects2d = new SomeObject[5][10];
    objects2d = new SomeObject[][] {{null, null}, null};
    objects2d = new SomeObject[20][];

    // Access
    SomeObject someObject = objects[0];
    someObject = objects2d[0][1];

    // Assignment
    objects[0] = null;
    objects2d[0][1] = null;
  }

  void testIntArrays() {
    // Creation
    int[] ints = new int[100];
    ints = new int[0];
    ints = new int[] {0, 1};
    int[][] ints2d = new int[5][10];
    ints2d = new int[][] {{1, 2}, null};
    ints2d = new int[20][];

    // Access
    int n = ints[0];
    n = ints2d[0][1];

    // Assignment
    ints[0] = 1;
    ints2d[0][1] = 1;

    // Widening conversion
    byte b = 1;
    char c = 'a';
    ints = new int[b];
    ints[b] = b;
    ints = new int[c];
    ints[c] = c;
    ints = new int[] {b, c};
  }

  void testLongArrays() {
    // Creation
    long[] longs = new long[100];
    longs = new long[0];
    longs = new long[] {0, 1};
    long[][] longs2d = new long[5][10];
    longs2d = new long[][] {{1, 2}, null};
    longs2d = new long[20][];

    // Access
    long n = longs[0];
    n = longs2d[0][1];

    // Assignment
    longs[0] = 1;
    longs2d[0][1] = 1;

    // Widening conversion
    byte b = 1;
    char c = 'a';
    longs = new long[b];
    longs[b] = b;
    longs = new long[c];
    longs[c] = c;
    longs = new long[] {b, c};
  }

  void testDevirtualizedTypeArrays() {
    // Creation
    Boolean[] booleans = new Boolean[100];
    booleans = new Boolean[0];
    booleans = new Boolean[] {true, false};
    Boolean[][] booleans2d = new Boolean[5][10];
    booleans2d = new Boolean[][] {{true, false}, null};
    booleans2d = new Boolean[20][];

    // Access
    boolean b = booleans[0];
    b = booleans2d[0][1];

    // Assignment
    booleans[0] = true;
    booleans2d[0][1] = false;
  }

  void testStringArrays() {
    // Creation
    String[] strings = new String[100];
    strings = new String[0];
    strings = new String[] {null, null};
    String[][] strings2d = new String[5][10];
    strings2d = new String[][] {{null, null}, null};
    strings2d = new String[20][];

    // Access
    String b = strings[0];
    b = strings2d[0][1];

    // Assignment
    strings[0] = null;
    strings2d[0][1] = null;
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private static class NativeType {}

  @Wasm("nop") // TODO(b/261079024) Remove when arrays of native types are supported.
  private void testNativeArray() {
    // Creation
    NativeType[] nativeObjects = new NativeType[100];
    nativeObjects = new NativeType[0];
    nativeObjects = new NativeType[] {null, null};
    NativeType[][] nativeObjects2d = new NativeType[5][10];
    nativeObjects2d = new NativeType[][] {{null, null}, null};
    nativeObjects2d = new NativeType[20][];

    // Access
    NativeType nativeObject = nativeObjects[0];
    nativeObject = nativeObjects2d[0][1];

    // Assignment
    nativeObjects[0] = null;
    nativeObjects2d[0][1] = null;
  }

  public void testErasureCastsOnArrayAccess() {
    ArrayContainer<String> container = null;
    String s = container.data[0];
  }

  public void testCovariance() {
    Object[] objectArray = null;
    String[] stringArray = null;
    objectArray = stringArray;
  }

  private static class ArrayContainer<T> {
    T[] data;
  }

  // TODO(b/267597125): Array is not Cloneable and Serializable in Kotlin-Native
  @KtDisabled
  public void testArraysSupertypeClosureTypes() {
    consumesCloneable(new Object[10]);
    consumesSerializable(new Object[10]);
  }

  public void consumesCloneable(Cloneable cloneable) {}

  public void consumesSerializable(Serializable serializable) {}
}
