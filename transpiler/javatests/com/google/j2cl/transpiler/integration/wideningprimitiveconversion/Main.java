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
package com.google.j2cl.transpiler.integration.wideningprimitiveconversion;

public class Main {
  private static class ClassTakesDouble {
    public ClassTakesDouble(double widenValue, double expectValue) {
      assert widenValue == expectValue;
    }
  }

  private static class ClassTakesFloat {
    public ClassTakesFloat(float widenValue, float expectValue) {
      assert widenValue == expectValue;
    }
  }

  private static class ClassTakesInt {
    public ClassTakesInt(int widenValue, int expectValue) {
      assert widenValue == expectValue;
    }
  }

  private static class ClassTakesLong {
    public ClassTakesLong(long widenValue, long expectValue) {
      assert widenValue == expectValue;
    }
  }

  private static int fieldIntFromChar = (char) 456;
  private static long fieldLongFromInt = 456;
  private static float fieldFloatFromLong = 456L;
  private static double fieldDoubleFromLong = 456L;

  public static void main(String[] args) {
    testAssignment();
    testBinaryNumericPromotion();
    testCast();
    testMethodInvocation();
  }

  private static double returnDoubleFromLong() {
    return 123L;
  }

  private static float returnFloatFromLong() {
    return 123L;
  }

  private static int returnIntFromChar() {
    return (char) 123;
  }

  private static long returnLongFromInt() {
    return 123;
  }

  private static void takesDouble(double widenValue, double expectValue) {
    assert widenValue == expectValue;
  }

  private static void takesFloat(float widenValue, float expectValue) {
    assert widenValue == expectValue;
  }

  private static void takesInt(int widenValue, int expectValue) {
    assert widenValue == expectValue;
  }

  private static void takesLong(long widenValue, long expectValue) {
    assert widenValue == expectValue;
  }

  private static void testArraysAssignment() {
    byte b = 1;
    char c = 'a';

    // Arrays
    int[] ints = new int[c];
    assert ints.length == 97;
    ints[b] = b;
    assert ints[1] == 1;
    double[] doubles = new double[] {100L, 200L, 300L};
    assert doubles[0] == 100d;
  }

  private static void testAssignment() {
    testArraysAssignment();
    testCompoundAssignment();
    testSimpleAssignment();
    testReturnAssignment();
    testFieldInitializerAssignment();
    testVariableInitializerAssignment();
    testTernaryAssignment();
  }

  private static void testBinaryNumericPromotion() {
    byte b = 1;
    byte mb = 127; // Byte.MAX_VALUE
    char c = 'a';
    char mc = 65535; // Character.MAX_VALUE;
    short s = 2;
    short ms = 32767; // Short.MAX_VALUE;
    int i = 3;
    int mi = 2147483647; // Integer.MAX_VALUE;
    long l = 4L;
    long ll = 2415919103L; // max_int < ll < max_int * 2, used for testing for signs.
    long ml = 9223372036854775807L; // Long.MAX_VALUE;
    float f = 2.7f;
    float mf = 3.4028235E38f; // Float.MAX_VALUE;
    double d = 2.6;

    // Anything below int promotes to int.
    assert (mb * mb) == ((int) mb * (int) mb);
    assert (mb * mc) == ((int) mb * (int) mc);
    assert (mb * ms) == ((int) mb * (int) ms);
    assert (mb * mi) == ((int) mb * (int) mi);

    // If there is a long then anything below long promotes to long.
    assert (l * mb) == ((long) l * (long) mb);
    assert (l * mc) == ((long) l * (long) mc);
    assert (l * ms) == ((long) l * (long) ms);
    assert (l * mi) == ((long) l * (long) mi);

    // If there is a float then anything below float promotes to float.
    assert (f * mb) == ((float) f * (float) mb);
    assert (f * mc) == ((float) f * (float) mc);
    assert (f * ms) == ((float) f * (float) ms);
    assert (f * mi) == ((float) f * (float) mi);
    assert (f * ml) == ((float) f * (float) ml);

    // If there is a double then anything below double promotes to double.
    assert (d * mb) == ((double) d * (double) mb);
    assert (d * mc) == ((double) d * (double) mc);
    assert (d * ms) == ((double) d * (double) ms);
    assert (d * mi) == ((double) d * (double) mi);
    assert (d * ml) == ((double) d * (double) ml);
    assert (d * mf) == ((double) d * (double) mf);

    // And with equality operators.
    assert i == l - 1L;
    assert i != l;
    assert i < l;
    assert !(i > l);
    assert i <= l;
    assert !(i >= l);
  }

  private static void testCast() {
    byte b = 1;
    byte mb = 127; // Byte.MAX_VALUE
    char c = 'a';
    char mc = 65535; // Character.MAX_VALUE;
    short s = 2;
    short ms = 32767; // Short.MAX_VALUE;
    int i = 3;
    int mi = 2147483647; // Integer.MAX_VALUE;
    long l = 4L;
    long ll = 2415919103L; // max_int < ll < max_int * 2, used for testing for signs.
    long ml = 9223372036854775807L; // Long.MAX_VALUE;
    float f = 2.7f;
    float mf = 3.4028235E38f; // Float.MAX_VALUE;
    double d = 2.6;

    assert ((short) b == 1);
    assert ((int) b == 1);
    assert ((long) b == 1L);
    assert ((float) b == 1.0);
    assert ((double) b == 1.0);

    assert ((short) mb == 127);
    assert ((int) mb == 127);
    assert ((long) mb == 127L);
    assert ((float) mb == 127.0);
    assert ((double) mb == 127.0);

    assert ((int) c == 97);
    assert ((long) c == 97L);
    assert ((float) c == 97.0);
    assert ((double) c == 97.0);

    assert ((int) mc == 65535);
    assert ((long) mc == 65535L);
    assert ((float) mc == 65535.0);
    assert ((double) mc == 65535.0);

    assert ((int) s == 2);
    assert ((long) s == 2L);
    assert ((float) s == 2.0);
    assert ((double) s == 2.0);

    assert ((int) ms == 32767);
    assert ((long) ms == 32767L);
    assert ((float) ms == 32767.0);
    assert ((double) ms == 32767.0);

    assert ((long) i == 3L);
    assert ((float) i == 3.0);
    assert ((double) i == 3.0);

    assert ((long) mi == 2147483647L);
    assert ((float) mi == 2.147483647E9); // we don't honor float-double precision differences
    assert ((double) mi == 2.147483647E9);

    assert ((float) l == 4);
    assert ((double) l == 4);

    assert ((float) ll == 2.415919103E9); // we don't honor float-double precision differences
    assert ((double) ll == 2.415919103E9);

    assert ((float) ml == 9.223372036854776E18);
    assert ((double) ml == 9.223372036854776E18);

    assert (((double) f - 2.7) < 1e-7);

    // we don't honor float-double precision differences
    assert ((double) mf == 3.4028234663852886e+38d);
  }

  private static void testCompoundAssignment() {
    int i = 3;
    long l = 6L;

    int ri = 0;
    long rl = 0;

    // Compound Assignment
    ri += l;
    assert ri == 6;
    rl += i;
    assert rl == 3;
  }

  private static void testFieldInitializerAssignment() {
    assert fieldIntFromChar == 456;
    assert fieldLongFromInt == 456;
    assert fieldFloatFromLong == 456;
    assert fieldDoubleFromLong == 456;
  }

  private static void testMethodInvocation() {
    char c = (char) 100;
    int i = 200;
    long l = 300L;

    takesInt(c, 100);
    takesLong(i, 200L);
    takesFloat(l, 300f);
    takesDouble(l, 300d);

    new ClassTakesInt(c, 100);
    new ClassTakesLong(i, 200L);
    new ClassTakesFloat(l, 300f);
    new ClassTakesDouble(l, 300d);
  }

  private static void testReturnAssignment() {
    assert returnIntFromChar() == 123;
    assert returnLongFromInt() == 123;
    assert returnFloatFromLong() == 123;
    assert returnDoubleFromLong() == 123;
  }

  private static void testSimpleAssignment() {
    byte b = 1;
    byte mb = 127; // Byte.MAX_VALUE
    char c = 'a';
    char mc = 65535; // Character.MAX_VALUE;
    short s = 2;
    short ms = 32767; // Short.MAX_VALUE;
    int i = 3;
    int mi = 2147483647; // Integer.MAX_VALUE;
    long l = 4L;
    long ll = 2415919103L; // max_int < ll < max_int * 2, used for testing for signs.
    long ml = 9223372036854775807L; // Long.MAX_VALUE;
    float f = 2.7f;
    float mf = 3.4028235E38f; // Float.MAX_VALUE;

    byte rb = 0;
    char rc = 0;
    short rs = 0;
    int ri = 0;
    long rl = 0;
    float rf = 0;
    double rd = 0;

    // Exhaustive simple assignment
    assert ((ri = b) == 1);
    assert ((rl = b) == 1L);
    assert ((rf = b) == 1.0);
    assert ((rd = b) == 1.0);

    assert ((ri = mb) == 127);
    assert ((rl = mb) == 127L);
    assert ((rf = mb) == 127.0);
    assert ((rd = mb) == 127.0);

    assert ((ri = c) == 97);
    assert ((rl = c) == 97L);
    assert ((rf = c) == 97.0);
    assert ((rd = c) == 97.0);

    assert ((ri = mc) == 65535);
    assert ((rl = mc) == 65535L);
    assert ((rf = mc) == 65535.0);
    assert ((rd = mc) == 65535.0);

    assert ((ri = s) == 2);
    assert ((rl = s) == 2L);
    assert ((rf = s) == 2.0);
    assert ((rd = s) == 2.0);

    assert ((ri = ms) == 32767);
    assert ((rl = ms) == 32767L);
    assert ((rf = ms) == 32767.0);
    assert ((rd = ms) == 32767.0);

    assert ((rl = i) == 3L);
    assert ((rf = i) == 3.0);
    assert ((rd = i) == 3.0);

    assert ((rl = mi) == 2147483647L);
    assert ((rf = mi) == 2.147483647E9); // we don't honor float-double precision differences
    assert ((rd = mi) == 2.147483647E9);

    assert ((rf = l) == 4);
    assert ((rd = l) == 4);

    assert ((rf = ll) == 2.415919103E9); // we don't honor float-double precision differences
    assert ((rd = ll) == 2.415919103E9);

    assert ((rf = ml) == 9.223372036854776E18);
    assert ((rd = ml) == 9.223372036854776E18);

    assert ((rd = f - 2.7) < 1e-7f);

    // we don't honor float-double precision differences
    assert ((rd = mf) == 3.4028234663852886e+38d);
  }

  private static void testTernaryAssignment() {
    byte b = (byte) 1;
    int i = 2;
    long l = 3L;
    float f = 4f;
    double d = 5d;

    // Just to avoid JSCompiler being unhappy about "suspicious code" when seeing a ternary that
    // always evaluates to true.
    boolean alwaysTrue = fieldIntFromChar == 456;

    // Below int promotes to int, if there is a long promote to long, if there is a float promote to
    // float, if there is a double promote to double.
    assert (alwaysTrue ? b : i) == 1;
    assert (alwaysTrue ? i : l) == 2L;
    assert (alwaysTrue ? l : f) == 3f;
    assert (alwaysTrue ? l : d) == 3d;
  }

  private static void testVariableInitializerAssignment() {
    int varIntFromChar = (char) 456;
    long varLongFromInt = 456;
    float varFloatFromLong = 456L;
    double varDoubleFromLong = 456L;

    assert varIntFromChar == 456;
    assert varLongFromInt == 456;
    assert varFloatFromLong == 456;
    assert varDoubleFromLong == 456;
  }
}
