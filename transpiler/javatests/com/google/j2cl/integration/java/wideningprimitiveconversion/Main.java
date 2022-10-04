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
package wideningprimitiveconversion;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  private static void testAssignment() {
    testArraysAssignment();
    testCompoundAssignment();
    testSimpleAssignment();
    testReturnAssignment();
    testFieldInitializerAssignment();
    testVariableInitializerAssignment();
    testTernaryAssignment();
    testStaticCoercions();
  }

  private static class ClassTakesDouble {
    public ClassTakesDouble(double widenValue, double expectValue) {
      assertTrue(widenValue == expectValue);
    }
  }

  private static class ClassTakesFloat {
    public ClassTakesFloat(float widenValue, float expectValue) {
      assertTrue(widenValue == expectValue);
    }
  }

  private static class ClassTakesInt {
    public ClassTakesInt(int widenValue, int expectValue) {
      assertTrue(widenValue == expectValue);
    }
  }

  private static class ClassTakesLong {
    public ClassTakesLong(long widenValue, long expectValue) {
      assertTrue(widenValue == expectValue);
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
    assertTrue(widenValue == expectValue);
  }

  private static void takesFloat(float widenValue, float expectValue) {
    assertTrue(widenValue == expectValue);
  }

  private static void takesInt(int widenValue, int expectValue) {
    assertTrue(widenValue == expectValue);
  }

  private static void takesLong(long widenValue, long expectValue) {
    assertTrue(widenValue == expectValue);
  }

  private static void testArraysAssignment() {
    byte b = 1;
    char c = 'a';

    // Arrays
    int[] ints = new int[c];
    assertTrue(ints.length == 97);
    ints[b] = b;
    assertTrue(ints[1] == 1);
    double[] doubles = new double[] {100L, 200L, 300L};
    assertTrue(doubles[0] == 100d);
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
    assertTrue((mb * mb) == ((int) mb * (int) mb));
    assertTrue((mb * mc) == ((int) mb * (int) mc));
    assertTrue((mb * ms) == ((int) mb * (int) ms));
    assertTrue((mb * mi) == ((int) mb * (int) mi));

    // If there is a long then anything below long promotes to long.
    assertTrue((l * mb) == ((long) l * (long) mb));
    assertTrue((l * mc) == ((long) l * (long) mc));
    assertTrue((l * ms) == ((long) l * (long) ms));
    assertTrue((l * mi) == ((long) l * (long) mi));

    // If there is a float then anything below float promotes to float.
    assertTrue((f * mb) == ((float) f * (float) mb));
    assertTrue((f * mc) == ((float) f * (float) mc));
    assertTrue((f * ms) == ((float) f * (float) ms));
    assertTrue((f * mi) == ((float) f * (float) mi));
    assertTrue((f * ml) == ((float) f * (float) ml));

    // If there is a double then anything below double promotes to double.
    assertTrue((d * mb) == ((double) d * (double) mb));
    assertTrue((d * mc) == ((double) d * (double) mc));
    assertTrue((d * ms) == ((double) d * (double) ms));
    assertTrue((d * mi) == ((double) d * (double) mi));
    assertTrue((d * ml) == ((double) d * (double) ml));
    assertTrue((d * mf) == ((double) d * (double) mf));

    // And with equality operators.
    assertTrue(i == l - 1L);
    assertTrue(i != l);
    assertTrue(i < l);
    assertTrue(!(i > l));
    assertTrue(i <= l);
    assertTrue(!(i >= l));
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

    assertTrue(((short) b == 1));
    assertTrue(((int) b == 1));
    assertTrue(((long) b == 1L));
    assertTrue(((float) b == 1.0));
    assertTrue(((double) b == 1.0));

    assertTrue(((short) mb == 127));
    assertTrue(((int) mb == 127));
    assertTrue(((long) mb == 127L));
    assertTrue(((float) mb == 127.0));
    assertTrue(((double) mb == 127.0));

    assertTrue(((int) c == 97));
    assertTrue(((long) c == 97L));
    assertTrue(((float) c == 97.0));
    assertTrue(((double) c == 97.0));

    assertTrue(((int) mc == 65535));
    assertTrue(((long) mc == 65535L));
    assertTrue(((float) mc == 65535.0));
    assertTrue(((double) mc == 65535.0));

    assertTrue(((int) s == 2));
    assertTrue(((long) s == 2L));
    assertTrue(((float) s == 2.0));
    assertTrue(((double) s == 2.0));

    assertTrue(((int) ms == 32767));
    assertTrue(((long) ms == 32767L));
    assertTrue(((float) ms == 32767.0));
    assertTrue(((double) ms == 32767.0));

    assertTrue(((long) i == 3L));
    assertTrue(((float) i == 3.0));
    assertTrue(((double) i == 3.0));

    assertTrue(((long) mi == 2147483647L));
    assertTrue(((double) mi == 2.147483647E9));

    assertTrue(((float) l == 4));
    assertTrue(((double) l == 4));

    assertTrue(((double) ll == 2.415919103E9));

    assertTrue(((float) ml == 9.223372036854776E18));
    assertTrue(((double) ml == 9.223372036854776E18));

    assertTrue((((double) f - 2.7) < 1e-7));

    assertTrue(((double) mf == 3.4028234663852886e+38d));
  }

  private static void testCompoundAssignment() {
    int i = 3;
    long l = 6L;

    int ri = 0;
    long rl = 0;

    // Compound Assignment
    ri += l;
    assertTrue(ri == 6);
    rl += i;
    assertTrue(rl == 3);
  }

  private static void testFieldInitializerAssignment() {
    assertTrue(fieldIntFromChar == 456);
    assertTrue(fieldLongFromInt == 456);
    assertTrue(fieldFloatFromLong == 456);
    assertTrue(fieldDoubleFromLong == 456);
  }

  private static void testMethodInvocation() {
    char c = (char) 100;
    int i = 200;
    long l = 300L;

    takesInt(c, 100);
    takesLong(i, 200L);
    takesFloat(l, 300f);
    takesDouble(l, 300d);

    Object unused1 = new ClassTakesInt(c, 100);
    Object unused2 = new ClassTakesLong(i, 200L);
    Object unused3 = new ClassTakesFloat(l, 300f);
    Object unused4 = new ClassTakesDouble(l, 300d);
  }

  private static void testReturnAssignment() {
    assertTrue(returnIntFromChar() == 123);
    assertTrue(returnLongFromInt() == 123);
    assertTrue(returnFloatFromLong() == 123);
    assertTrue(returnDoubleFromLong() == 123);
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
    assertTrue(((ri = b) == 1));
    assertTrue(((rl = b) == 1L));
    assertTrue(((rf = b) == 1.0));
    assertTrue(((rd = b) == 1.0));

    assertTrue(((ri = mb) == 127));
    assertTrue(((rl = mb) == 127L));
    assertTrue(((rf = mb) == 127.0));
    assertTrue(((rd = mb) == 127.0));

    assertTrue(((ri = c) == 97));
    assertTrue(((rl = c) == 97L));
    assertTrue(((rf = c) == 97.0));
    assertTrue(((rd = c) == 97.0));

    assertTrue(((ri = mc) == 65535));
    assertTrue(((rl = mc) == 65535L));
    assertTrue(((rf = mc) == 65535.0));
    assertTrue(((rd = mc) == 65535.0));

    assertTrue(((ri = s) == 2));
    assertTrue(((rl = s) == 2L));
    assertTrue(((rf = s) == 2.0));
    assertTrue(((rd = s) == 2.0));

    assertTrue(((ri = ms) == 32767));
    assertTrue(((rl = ms) == 32767L));
    assertTrue(((rf = ms) == 32767.0));
    assertTrue(((rd = ms) == 32767.0));

    assertTrue(((rl = i) == 3L));
    assertTrue(((rf = i) == 3.0));
    assertTrue(((rd = i) == 3.0));

    assertTrue(((rl = mi) == 2147483647L));
    assertTrue(((rd = mi) == 2.147483647E9));

    assertTrue(((rf = l) == 4));
    assertTrue(((rd = l) == 4));

    assertTrue(((rd = ll) == 2.415919103E9));

    assertTrue(((rd = ml) == 9.223372036854776E18));

    assertTrue(((rd = f - 2.7) < 1e-7f));

    assertTrue(((rd = mf) == 3.4028234663852886e+38d));
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
    assertTrue((alwaysTrue ? b : i) == 1);
    assertTrue((alwaysTrue ? i : l) == 2L);
    assertTrue((alwaysTrue ? l : f) == 3f);
    assertTrue((alwaysTrue ? l : d) == 3d);
  }

  private static void testVariableInitializerAssignment() {
    int varIntFromChar = (char) 456;
    long varLongFromInt = 456;
    float varFloatFromLong = 456L;
    double varDoubleFromLong = 456L;

    assertTrue(varIntFromChar == 456);
    assertTrue(varLongFromInt == 456);
    assertTrue(varFloatFromLong == 456);
    assertTrue(varDoubleFromLong == 456);
  }

  private static void testStaticCoercions() {
    long max = 9223372036854775807L;

    float f = (float) 9223372036854775807L; //  9.223372036854776E18
    assertTrue(f == (float) max);

    double d = (double) 9223372036854775807L; //  9.223372036854776E18
    assertTrue(d == (double) max);
  }
}
