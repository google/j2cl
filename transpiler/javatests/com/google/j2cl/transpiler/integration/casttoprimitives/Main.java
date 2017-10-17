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
package com.google.j2cl.transpiler.integration.casttoprimitives;

@SuppressWarnings({"BoxedPrimitiveConstructor", "unchecked"})
public class Main {
  public static void main(String[] args) {
    testPrimitiveToPrimitive();
    testObjectReferenceToPrimitive();
    testBoxedToPrimitive();
    testTypeExtendsLongReferenceToPrimitive();
    testTypeExtendsIntersectionReferenceToPrimitive();
  }

  @SuppressWarnings("unused")
  public static void testPrimitiveToPrimitive() {
    byte b = 1;
    byte maxByte = 127; // Byte.MAX_VALUE
    char c = 'a';
    char maxChar = 65535; // Character.MAX_VALUE;
    short s = 2;
    short maxShort = 32767; // Short.MAX_VALUE;
    int i = 3;
    int maxInt = 2147483647; // Integer.MAX_VALUE;
    long l = 4L;
    long ll = 2415919103L; // max_int < ll < max_int * 2, used for testing for signs.
    long maxLong = 9223372036854775807L; // Long.MAX_VALUE;
    float f = 2.7f;
    float maxFloat = 3.4028235E38f; // Float.MAX_VALUE;
    float posInfiniteFloat = Float.POSITIVE_INFINITY;
    float negInfiniteFloat = Float.NEGATIVE_INFINITY;
    float nanFloat = Float.NaN;
    double d = 2.6;
    double dd = 2415919103.7; // dd > max_int
    double maxDouble = 1.7976931348623157E308; // Double.MAX_VALUE;
    double posInfiniteDouble = Double.POSITIVE_INFINITY;
    double negInfiniteDouble = Double.NEGATIVE_INFINITY;
    double nanDouble = Double.NaN;

    assert ((char) b == 1);
    assert ((short) b == 1);
    assert ((int) b == 1);
    assert ((long) b == 1L);
    assert ((float) b == 1.0);
    assert ((double) b == 1.0);

    assert ((char) maxByte == 127);
    assert ((short) maxByte == 127);
    assert ((int) maxByte == 127);
    assert ((long) maxByte == 127L);
    assert ((float) maxByte == 127.0);
    assert ((double) maxByte == 127.0);

    assert ((byte) c == 97);
    assert ((short) c == 97);
    assert ((int) c == 97);
    assert ((long) c == 97L);
    assert ((float) c == 97.0);
    assert ((double) c == 97.0);

    assert ((byte) maxChar == -1);
    assert ((short) maxChar == -1);
    assert ((int) maxChar == 65535);
    assert ((long) maxChar == 65535L);
    assert ((float) maxChar == 65535.0);
    assert ((double) maxChar == 65535.0);

    assert ((byte) s == 2);
    assert ((char) s == 2);
    assert ((int) s == 2);
    assert ((long) s == 2L);
    assert ((float) s == 2.0);
    assert ((double) s == 2.0);

    assert ((byte) maxShort == -1);
    assert ((char) maxShort == 32767);
    assert ((int) maxShort == 32767);
    assert ((long) maxShort == 32767L);
    assert ((float) maxShort == 32767.0);
    assert ((double) maxShort == 32767.0);

    assert ((byte) i == 3);
    assert ((char) i == 3);
    assert ((short) i == 3);
    assert ((long) i == 3L);
    assert ((float) i == 3.0);
    assert ((double) i == 3.0);

    assert ((byte) maxInt == -1);
    assert ((char) maxInt == 65535);
    assert ((short) maxInt == -1);
    assert ((long) maxInt == 2147483647L);
    // assert ((float) maxInt == 2.14748365E9f); // it is casted to double-precision 2.147483647E9
    assert ((double) maxInt == 2.147483647E9);

    assert ((byte) l == 4);
    assert ((char) l == 4);
    assert ((short) l == 4);
    assert ((int) l == 4);
    assert ((float) l == 4);
    assert ((double) l == 4);

    assert ((byte) ll == -1);
    assert ((char) ll == 65535);
    assert ((short) ll == -1);
    assert ((int) ll == -1879048193);
    // assert ((float) ll == 2.4159191E9); // it is casted to double-precision 2.415919103E9.
    assert ((double) ll == 2.415919103E9);

    assert ((byte) maxLong == -1);
    assert ((char) maxLong == 65535);
    assert ((short) maxLong == -1);
    assert ((int) maxLong == -1);
    assert ((float) maxLong == 9.223372036854776E18);
    assert ((double) maxLong == 9.223372036854776E18);

    assert ((byte) f == 2);
    assert ((char) f == 2);
    assert ((short) f == 2);
    assert ((int) f == 2);
    assert ((long) f == 2L);
    assert (((double) f - 2.7) < 1e-7);

    assert ((byte) maxFloat == -1);
    assert ((char) maxFloat == 65535);
    assert ((short) maxFloat == -1);
    assert ((int) maxFloat == 2147483647);
    assert ((long) maxFloat == 9223372036854775807L);
    assert ((double) maxFloat == 3.4028234663852886E38);

    assert ((byte) posInfiniteFloat == -1);
    assert ((short) posInfiniteFloat == -1);
    assert ((char) posInfiniteFloat == 65535);
    assert ((int) posInfiniteFloat == 2147483647);
    assert ((long) posInfiniteFloat == 9223372036854775807L);
    assert ((double) posInfiniteFloat == Double.POSITIVE_INFINITY);

    assert ((byte) negInfiniteFloat == 0);
    assert ((short) negInfiniteFloat == 0);
    assert ((char) negInfiniteFloat == 0);
    assert ((int) negInfiniteFloat == -2147483648);
    assert ((long) negInfiniteFloat == -9223372036854775808L);
    assert ((double) negInfiniteFloat == Double.NEGATIVE_INFINITY);

    assert ((byte) nanFloat == 0);
    assert ((short) nanFloat == 0);
    assert ((char) nanFloat == 0);
    assert ((int) nanFloat == 0);
    assert ((long) nanFloat == 0L);
    assert (Double.isNaN((double) nanFloat));

    assert ((byte) d == 2);
    assert ((char) d == 2);
    assert ((short) d == 2);
    assert ((int) d == 2);
    assert ((long) d == 2L);
    // assert ((float) d == 2.6f); // float is emitted as 2.5999999046325684

    assert ((byte) dd == -1);
    assert ((char) dd == 65535);
    assert ((short) dd == -1);
    assert ((int) dd == 2147483647);
    assert ((long) dd == 2415919103L);
    // assert ((float) dd == 2.4159191E9); // it is casted to double-precision 2415919103.7

    assert ((byte) maxDouble == -1);
    assert ((char) maxDouble == 65535);
    assert ((short) maxDouble == -1);
    assert ((int) maxDouble == 2147483647);
    assert ((long) maxDouble == 9223372036854775807L);

    assert ((byte) posInfiniteDouble == -1);
    assert ((short) posInfiniteDouble == -1);
    assert ((char) posInfiniteDouble == 65535);
    assert ((int) posInfiniteDouble == 2147483647);
    assert ((long) posInfiniteDouble == 9223372036854775807L);
    assert ((float) posInfiniteDouble == Float.POSITIVE_INFINITY);

    assert ((byte) negInfiniteDouble == 0);
    assert ((short) negInfiniteDouble == 0);
    assert ((char) negInfiniteDouble == 0);
    assert ((int) negInfiniteDouble == -2147483648);
    assert ((long) negInfiniteDouble == -9223372036854775808L);
    assert ((float) negInfiniteDouble == Float.NEGATIVE_INFINITY);

    assert ((byte) nanDouble == 0);
    assert ((short) nanDouble == 0);
    assert ((char) nanDouble == 0);
    assert ((int) nanDouble == 0);
    assert ((long) nanDouble == 0L);
    assert (Float.isNaN((float) nanDouble));
  }

  @SuppressWarnings("unused")
  public static void testObjectReferenceToPrimitive() {
    Object o = new Object();
    try {
      boolean bool = (boolean) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      byte b = (byte) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      char c = (char) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      short s = (short) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      int i = (int) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      long l = (long) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      float f = (float) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      double d = (double) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }

    o = Boolean.FALSE;
    boolean bool = (boolean) o;
    assert !bool;

    o = Byte.MAX_VALUE;
    byte b = (byte) o;
    assert b == 127;

    o = new Character('a');
    char c = (char) o;
    assert c == 'a';

    o = Short.MAX_VALUE;
    short s = (short) o;
    assert s == 32767;

    o = new Integer(1);
    int i = (int) o;
    assert i == 1;

    o = new Long(1L);
    long l = (long) o;
    assert l == 1L;

    o = new Float(1.1f);
    float f = (float) o;
    assert f == 1.1f;

    o = new Double(1.2);
    double d = (double) o;
    assert d == 1.2;
  }

  public static <T extends Long> void testTypeExtendsLongReferenceToPrimitive() {
    T o = (T) new Long(1);
    long l = (long) o;
    assert l == 1;

    float f = (float) o;
    assert f == 1.0;

    double d = (double) o;
    assert d == 1.0;
  }

  public static <T extends Long & Comparable<Long>>
      void testTypeExtendsIntersectionReferenceToPrimitive() {
    T o = (T) new Long(1);
    long l = (long) o;
    assert l == 1;

    float f = (float) o;
    assert f == 1.0;

    double d = (double) o;
    assert d == 1.0;
  }

  @SuppressWarnings("unused")
  public static void testBoxedToPrimitive() {
    Byte b = new Byte((byte) 1);
    Character c = new Character('a');
    Short s = new Short((short) 1);
    Integer i = new Integer(1);
    Long l = new Long(1L);
    Float f = new Float(1.1f);
    Double d = new Double(1.1);

    short ss = b;
    assert ss == 1;

    int ii = b;
    assert ii == 1;
    ii = c;
    assert ii == 97;
    ii = s;
    assert ii == 1;

    long ll = b;
    assert ll == 1L;
    ll = s;
    assert ll == 1L;
    ll = c;
    assert ll == 97L;
    ll = i;
    assert ll == 1L;

    float ff = b;
    assert ff == 1;
    ff = s;
    assert ff == 1;
    ff = c;
    assert ff == 97;
    ff = i;
    assert ff == 1;
    ff = l;
    assert ff == 1;

    double dd = b;
    assert dd == 1;
    dd = s;
    assert dd == 1;
    dd = c;
    assert dd == 97;
    dd = i;
    assert dd == 1;
    dd = l;
    assert dd == 1;
    dd = f;
    assert dd - 1.1 < 1e-7;
  }
}
