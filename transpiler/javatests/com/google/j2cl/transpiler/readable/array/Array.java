package com.google.j2cl.transpiler.readable.array;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Array {
  public void objectArraySample() {
    // Creation
    Object[] objects = new Object[100];
    objects = new Object[0];
    objects = new Object[] {null, null};
    Object[][] objects2d = new Object[5][10];
    objects2d = new Object[][] {{null, null}, null};
    objects2d = new Object[20][];

    // Access
    Object object = objects[0];
    object = objects2d[0][1];

    // Assignment
    objects[0] = null;
    objects2d[0][1] = null;
  }

  private class SomeObject {}

  public void javaObjectArraySample() {
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

  void primitiveArraysSample() {
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

    // Compound assignment.
    ints[0] += 1;
    ints[0] -= 1;
    ints[0] *= 1;
    ints[0] /= 1;
    ints[0] &= 1;
    ints[0] ^= 1;
    ints[0] |= 1;
    ints[0] %= 1;
    ints[0] <<= 1;
    ints[0] >>= 1;
    ints[0] >>>= 1;
    ints[0]++;
    ++ints[0];
  }

  void longArraysSample() {
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

    // Compound assignment.
    longs[0] += 1;
    longs[0]--;
    --longs[0];
  }

  void devirtualizedTypeArraysSample() {
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

    // Compound assignment.
    booleans[0] |= true;
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private static class NativeType {}

  public void nativeArraySample() {
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
}
