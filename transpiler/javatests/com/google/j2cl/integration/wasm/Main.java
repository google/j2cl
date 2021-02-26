/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.integration.wasm;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;

/**
 * Incrementally tests wasm features as they are being added.
 *
 * <p>This test will be removed when all WASM features are implemented and all integration tests are
 * enabled for WASM.
 */
public class Main {

  public static void main(String... args) {
    testDynamicClassMethodDispatch();
    testSwitch();
    testArrays();
    testWasmAnnotation();
  }

  static class A {
    public int m() {
      return 1;
    }
  }

  static class B extends A {
    @Override
    public int m() {
      return 2;
    }
  }

  private static void testDynamicClassMethodDispatch() {
    A a = new A();
    B b = new B();
    assertEquals(1, a.m());
    assertEquals(2, b.m());
    a = b;
    assertEquals(2, b.m());
  }

  enum MyEnum {
    A,
    B;
  }

  private static int next = 0;

  private static int returnsNext() {
    return next++;
  }

  private static void testSwitch() {
    // returnsNext = 0;
    switch (returnsNext()) {
      case 1:
        fail();
      case 0:
        break;
      default:
        fail();
    }

    // returnsNext() = 1
    switch (returnsNext()) {
      case 2:
        fail();
      default:
        break;
      case 0:
        fail();
    }

    MyEnum a = MyEnum.A;
    switch (a) {
      case B:
        fail();
      case A:
        break;
      default:
        fail();
    }
  }

  private static void testArrays() {
    Object o1 = new Object();
    Object o2 = new Object();

    Object[] oneD = new Object[2];
    assertTrue(oneD.length == 2);
    oneD[0] = o1;
    assertTrue(oneD[0] == o1);

    Object[][] twoD = new Object[2][3];
    assertTrue(twoD.length == 2);
    assertTrue(twoD[0].length == 3);
    twoD[0][1] = o1;
    assertTrue(twoD[0][1] == o1);
    twoD[1] = oneD;
    assertTrue(twoD[1][0] == o1);

    Object[][] partial2D = new Object[2][];
    assertTrue(partial2D.length == 2);
    partial2D[0] = oneD;
    assertTrue(partial2D[0][0] == o1);

    // primitives
    boolean[] booleans = new boolean[2];
    assertTrue(booleans.length == 2);
    assertFalse(booleans[0]);
    booleans[0] = true;
    assertTrue(booleans[0]);
    boolean[][] booleans2D = new boolean[2][2];
    assertFalse(booleans2D[0][1]);
    booleans2D[0][1] = true;
    assertTrue(booleans2D[0][1]);

    char[] chars = new char[2];
    assertTrue(chars.length == 2);
    assertTrue(chars[0] == 0);
    chars[0] = 'c';
    assertTrue(chars[0] == 'c');
    char[][] chars2D = new char[2][2];
    assertTrue(chars2D[0][1] == 0);
    chars2D[0][1] = 'a';
    assertTrue(chars2D[0][1] == 'a');

    byte[] bytes = new byte[2];
    assertTrue(bytes.length == 2);
    assertTrue(bytes[0] == 0);
    bytes[0] = 1;
    assertTrue(bytes[0] == 1);
    byte[][] bytes2D = new byte[2][2];
    assertTrue(bytes2D[0][1] == 0);
    bytes2D[0][1] = 12;
    assertTrue(bytes2D[0][1] == 12);

    short[] shorts = new short[2];
    assertTrue(shorts.length == 2);
    assertTrue(shorts[0] == 0);
    shorts[0] = 1;
    assertTrue(shorts[0] == 1);
    short[][] shorts2D = new short[2][2];
    assertTrue(shorts2D[0][1] == 0);
    shorts2D[0][1] = 12;
    assertTrue(shorts2D[0][1] == 12);

    int[] ints = new int[2];
    assertTrue(ints.length == 2);
    assertTrue(ints[0] == 0);
    ints[0] = 1;
    assertTrue(ints[0] == 1);
    int[][] ints2D = new int[2][2];
    assertTrue(ints2D[0][1] == 0);
    ints2D[0][1] = 12;
    assertTrue(ints2D[0][1] == 12);

    long[] longs = new long[2];
    assertTrue(longs.length == 2);
    assertTrue(longs[0] == 0L);
    longs[0] = Long.MAX_VALUE;
    assertTrue(longs[0] == Long.MAX_VALUE);
    long[][] longs2D = new long[2][2];
    assertTrue(longs2D[0][1] == 0L);
    longs2D[0][1] = Long.MAX_VALUE;
    assertTrue(longs2D[0][1] == Long.MAX_VALUE);

    double[] doubles = new double[2];
    assertTrue(doubles.length == 2);
    assertTrue(doubles[0] == 0d);
    doubles[0] = 12d;
    assertTrue(doubles[0] == 12d);
    double[][] doubles2D = new double[2][2];
    assertTrue(doubles2D[0][1] == 0d);
    doubles2D[0][1] = 12d;
    assertTrue(doubles2D[0][1] == 12d);

    float[] floats = new float[2];
    assertTrue(floats.length == 2);
    assertTrue(floats[0] == 0f);
    floats[0] = 2f;
    assertTrue(floats[0] == 2f);
    float[][] floats2D = new float[2][2];
    assertTrue(floats2D[0][1] == 0f);
    floats2D[0][1] = 12f;
    assertTrue(floats2D[0][1] == 12f);

    // Array literals
    Object[] empty = new Object[] {};
    assertTrue(empty.length == 0);

    Object[] oneDL = new Object[] {o1, o2};
    assertTrue(oneDL.length == 2);
    assertTrue(oneDL[0] == o1);
    assertTrue(oneDL[1] == o2);
    oneDL[1] = o1;
    assertTrue(oneDL[1] == o1);

    int[][] twoDL = new int[][] {{0, 1}, {2, 3}};
    assertTrue(twoDL[0][0] == 0);
    assertTrue(twoDL[0][1] == 1);
    assertTrue(twoDL[1][0] == 2);
    assertTrue(twoDL[1][1] == 3);

    twoDL[0][1] = 5;
    assertTrue(twoDL[0][1] == 5);
    twoDL[1] = new int[3];
    assertTrue(twoDL[1].length == 3);
    assertTrue(twoDL[1][0] == 0);

    int[] terseLiteral = {0, 1, 2};
    assertTrue(terseLiteral.length == 3);
    assertTrue(terseLiteral[0] == 0);
    assertTrue(terseLiteral[1] == 1);
    assertTrue(terseLiteral[2] == 2);

    int[][][] partial3D = new int[2][2][];
    assertTrue(partial3D.length == 2);
    assertTrue(partial3D[0].length == 2);
    partial3D[0][0] = terseLiteral;
    assertTrue(partial3D[0][0][0] == 0);
    assertTrue(partial3D[0][0][1] == 1);
    assertTrue(partial3D[0][0][2] == 2);
  }

  private static void testWasmAnnotation() {
    assertTrue(42 == multiply(6, 7));
  }

  @JsMethod // Exist to keep to test running under closure output
  @Wasm("i32.mul")
  private static native int multiply(int x, int y);
}
