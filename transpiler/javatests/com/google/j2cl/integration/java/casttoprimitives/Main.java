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
package casttoprimitives;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

@SuppressWarnings({"BoxedPrimitiveConstructor", "unchecked"})
public class Main {
  public static void main(String... args) {
    testPrimitiveToPrimitive();
    testObjectReferenceToPrimitive();
    testBoxedToPrimitive();
    testTypeExtendsLongReferenceToPrimitive();
    testTypeExtendsIntersectionReferenceToPrimitive();
    testPrimitiveToReference();
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

    assertTrue(((char) b == 1));
    assertTrue(((short) b == 1));
    assertTrue(((int) b == 1));
    assertTrue(((long) b == 1L));
    assertTrue(((float) b == 1.0));
    assertTrue(((double) b == 1.0));

    assertTrue(((char) maxByte == 127));
    assertTrue(((short) maxByte == 127));
    assertTrue(((int) maxByte == 127));
    assertTrue(((long) maxByte == 127L));
    assertTrue(((float) maxByte == 127.0));
    assertTrue(((double) maxByte == 127.0));

    assertTrue(((byte) c == 97));
    assertTrue(((short) c == 97));
    assertTrue(((int) c == 97));
    assertTrue(((long) c == 97L));
    assertTrue(((float) c == 97.0));
    assertTrue(((double) c == 97.0));

    assertTrue(((byte) maxChar == -1));
    assertTrue(((short) maxChar == -1));
    assertTrue(((int) maxChar == 65535));
    assertTrue(((long) maxChar == 65535L));
    assertTrue(((float) maxChar == 65535.0));
    assertTrue(((double) maxChar == 65535.0));

    assertTrue(((byte) s == 2));
    assertTrue(((char) s == 2));
    assertTrue(((int) s == 2));
    assertTrue(((long) s == 2L));
    assertTrue(((float) s == 2.0));
    assertTrue(((double) s == 2.0));

    assertTrue(((byte) maxShort == -1));
    assertTrue(((char) maxShort == 32767));
    assertTrue(((int) maxShort == 32767));
    assertTrue(((long) maxShort == 32767L));
    assertTrue(((float) maxShort == 32767.0));
    assertTrue(((double) maxShort == 32767.0));

    assertTrue(((byte) i == 3));
    assertTrue(((char) i == 3));
    assertTrue(((short) i == 3));
    assertTrue(((long) i == 3L));
    assertTrue(((float) i == 3.0));
    assertTrue(((double) i == 3.0));

    assertTrue(((byte) maxInt == -1));
    assertTrue(((char) maxInt == 65535));
    assertTrue(((short) maxInt == -1));
    assertTrue(((long) maxInt == 2147483647L));
    // assertTrue(((float) maxInt == 2.14748365E9f)); // it is casted to double-precision
    // 2.147483647E9
    assertTrue(((double) maxInt == 2.147483647E9));

    assertTrue(((byte) l == 4));
    assertTrue(((char) l == 4));
    assertTrue(((short) l == 4));
    assertTrue(((int) l == 4));
    assertTrue(((float) l == 4));
    assertTrue(((double) l == 4));

    assertTrue(((byte) ll == -1));
    assertTrue(((char) ll == 65535));
    assertTrue(((short) ll == -1));
    assertTrue(((int) ll == -1879048193));
    // assertTrue(((float) ll == 2.4159191E9)); // it is casted to double-precision 2.415919103E9.
    assertTrue(((double) ll == 2.415919103E9));

    assertTrue(((byte) maxLong == -1));
    assertTrue(((char) maxLong == 65535));
    assertTrue(((short) maxLong == -1));
    assertTrue(((int) maxLong == -1));
    assertTrue(((float) maxLong == 9.223372036854776E18));
    assertTrue(((double) maxLong == 9.223372036854776E18));

    assertTrue(((byte) f == 2));
    assertTrue(((char) f == 2));
    assertTrue(((short) f == 2));
    assertTrue(((int) f == 2));
    assertTrue(((long) f == 2L));
    assertTrue((((double) f - 2.7) < 1e-7));

    assertTrue(((byte) maxFloat == -1));
    assertTrue(((char) maxFloat == 65535));
    assertTrue(((short) maxFloat == -1));
    assertTrue(((int) maxFloat == 2147483647));
    assertTrue(((long) maxFloat == 9223372036854775807L));
    assertTrue(((double) maxFloat == 3.4028234663852886E38));

    assertTrue(((byte) posInfiniteFloat == -1));
    assertTrue(((short) posInfiniteFloat == -1));
    assertTrue(((char) posInfiniteFloat == 65535));
    assertTrue(((int) posInfiniteFloat == 2147483647));
    assertTrue(((long) posInfiniteFloat == 9223372036854775807L));
    assertTrue(((double) posInfiniteFloat == Double.POSITIVE_INFINITY));

    assertTrue(((byte) negInfiniteFloat == 0));
    assertTrue(((short) negInfiniteFloat == 0));
    assertTrue(((char) negInfiniteFloat == 0));
    assertTrue(((int) negInfiniteFloat == -2147483648));
    assertTrue(((long) negInfiniteFloat == -9223372036854775808L));
    assertTrue(((double) negInfiniteFloat == Double.NEGATIVE_INFINITY));

    assertTrue(((byte) nanFloat == 0));
    assertTrue(((short) nanFloat == 0));
    assertTrue(((char) nanFloat == 0));
    assertTrue(((int) nanFloat == 0));
    assertTrue(((long) nanFloat == 0L));
    assertTrue((Double.isNaN((double) nanFloat)));

    assertTrue(((byte) d == 2));
    assertTrue(((char) d == 2));
    assertTrue(((short) d == 2));
    assertTrue(((int) d == 2));
    assertTrue(((long) d == 2L));
    // assertTrue(((float) d == 2.6f)); // float is emitted as 2.5999999046325684

    assertTrue(((byte) dd == -1));
    assertTrue(((char) dd == 65535));
    assertTrue(((short) dd == -1));
    assertTrue(((int) dd == 2147483647));
    assertTrue(((long) dd == 2415919103L));
    // assertTrue(((float) dd == 2.4159191E9)); // it is casted to double-precision 2415919103.7

    assertTrue(((byte) maxDouble == -1));
    assertTrue(((char) maxDouble == 65535));
    assertTrue(((short) maxDouble == -1));
    assertTrue(((int) maxDouble == 2147483647));
    assertTrue(((long) maxDouble == 9223372036854775807L));

    assertTrue(((byte) posInfiniteDouble == -1));
    assertTrue(((short) posInfiniteDouble == -1));
    assertTrue(((char) posInfiniteDouble == 65535));
    assertTrue(((int) posInfiniteDouble == 2147483647));
    assertTrue(((long) posInfiniteDouble == 9223372036854775807L));
    assertTrue(((float) posInfiniteDouble == Float.POSITIVE_INFINITY));

    assertTrue(((byte) negInfiniteDouble == 0));
    assertTrue(((short) negInfiniteDouble == 0));
    assertTrue(((char) negInfiniteDouble == 0));
    assertTrue(((int) negInfiniteDouble == -2147483648));
    assertTrue(((long) negInfiniteDouble == -9223372036854775808L));
    assertTrue(((float) negInfiniteDouble == Float.NEGATIVE_INFINITY));

    assertTrue(((byte) nanDouble == 0));
    assertTrue(((short) nanDouble == 0));
    assertTrue(((char) nanDouble == 0));
    assertTrue(((int) nanDouble == 0));
    assertTrue(((long) nanDouble == 0L));
    assertTrue((Float.isNaN((float) nanDouble)));
  }

  @SuppressWarnings("unused")
  public static void testObjectReferenceToPrimitive() {
    {
    Object o = new Object();
      assertThrowsClassCastException(
          () -> {
            boolean b = (boolean) o;
          });
      assertThrowsClassCastException(
          () -> {
            byte b = (byte) o;
          });
      assertThrowsClassCastException(
          () -> {
            char c = (char) o;
          });
      assertThrowsClassCastException(
          () -> {
            short s = (short) o;
          });
      assertThrowsClassCastException(
          () -> {
            int i = (int) o;
          });
      assertThrowsClassCastException(
          () -> {
            long l = (long) o;
          });
      assertThrowsClassCastException(
          () -> {
            float f = (float) o;
          });
      assertThrowsClassCastException(
          () -> {
            double d = (double) o;
          });
    }
    {
      Object o = Boolean.FALSE;
      boolean bool = (boolean) o;
      assertTrue(!bool);

      o = Byte.MAX_VALUE;
      byte b = (byte) o;
      assertTrue(b == 127);

      o = new Character('a');
      char c = (char) o;
      assertTrue(c == 'a');

      o = Short.MAX_VALUE;
      short s = (short) o;
      assertTrue(s == 32767);

      o = new Integer(1);
      int i = (int) o;
      assertTrue(i == 1);

      o = new Long(1L);
      long l = (long) o;
      assertTrue(l == 1L);

      o = new Float(1.1f);
      float f = (float) o;
      assertTrue(f == 1.1f);

      o = new Double(1.2);
      double d = (double) o;
      assertTrue(d == 1.2);
    }
  }

  public static <T extends Long> void testTypeExtendsLongReferenceToPrimitive() {
    T o = (T) new Long(1);
    long l = (long) o;
    assertTrue(l == 1);

    float f = (float) o;
    assertTrue(f == 1.0);

    double d = (double) o;
    assertTrue(d == 1.0);
  }

  public static <T extends Long & Comparable<Long>>
      void testTypeExtendsIntersectionReferenceToPrimitive() {
    T o = (T) new Long(1);
    long l = (long) o;
    assertTrue(l == 1);

    float f = (float) o;
    assertTrue(f == 1.0);

    double d = (double) o;
    assertTrue(d == 1.0);
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
    assertTrue(ss == 1);

    int ii = b;
    assertTrue(ii == 1);
    ii = c;
    assertTrue(ii == 97);
    ii = s;
    assertTrue(ii == 1);

    long ll = b;
    assertTrue(ll == 1L);
    ll = s;
    assertTrue(ll == 1L);
    ll = c;
    assertTrue(ll == 97L);
    ll = i;
    assertTrue(ll == 1L);

    float ff = b;
    assertTrue(ff == 1);
    ff = s;
    assertTrue(ff == 1);
    ff = c;
    assertTrue(ff == 97);
    ff = i;
    assertTrue(ff == 1);
    ff = l;
    assertTrue(ff == 1);

    double dd = b;
    assertTrue(dd == 1);
    dd = s;
    assertTrue(dd == 1);
    dd = c;
    assertTrue(dd == 97);
    dd = i;
    assertTrue(dd == 1);
    dd = l;
    assertTrue(dd == 1);
    dd = f;
    assertTrue(dd - 1.1 < 1e-7);
  }

  @SuppressWarnings("cast")
  public static void testPrimitiveToReference() {
    boolean bool = true;
    byte b = 1;
    char c = 'a';
    short s = 1;
    int i = 1;
    long l = 1L;
    float f = 1.0f;
    double d = 1.0;
    Object o = bool;
    assertTrue(o != null);
    o = b;
    assertTrue(o != null);
    o = c;
    assertTrue(o != null);
    o = s;
    assertTrue(o != null);
    o = i;
    assertTrue(o != null);
    o = l;
    assertTrue(o != null);
    o = f;
    assertTrue(o != null);
    o = d;
    assertTrue(o != null);
    o = (Object) bool;
    assertTrue(o != null);
    o = (Object) b;
    assertTrue(o != null);
    o = (Object) c;
    assertTrue(o != null);
    o = (Object) s;
    assertTrue(o != null);
    o = (Object) i;
    assertTrue(o != null);
    o = (Object) l;
    assertTrue(o != null);
    o = (Object) f;
    assertTrue(o != null);
    o = (Object) d;
    assertTrue(o != null);
  }
}
