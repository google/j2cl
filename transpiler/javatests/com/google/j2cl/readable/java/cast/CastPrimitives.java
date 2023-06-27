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
package cast;

public class CastPrimitives {
  public void testPrimitiveCasts() {
    byte b = 1;
    char c = 1;
    short s = 1;
    int i = 1;
    long l = 1L;
    float f = 1.1f;
    double d = 1.1;

    b = (byte) b;
    c = (char) b;
    s = (short) b;
    i = (int) b;
    l = (long) b;
    f = (float) b;
    d = (double) b;

    b = (byte) c;
    c = (char) c;
    s = (short) c;
    i = (int) c;
    l = (long) c;
    f = (float) c;
    d = (double) c;

    b = (byte) s;
    c = (char) s;
    s = (short) s;
    i = (int) s;
    l = (long) s;
    f = (float) s;
    d = (double) s;

    b = (byte) i;
    c = (char) i;
    s = (short) i;
    i = (int) i;
    l = (long) i;
    f = (float) i;
    d = (double) i;

    b = (byte) l;
    c = (char) l;
    s = (short) l;
    i = (int) l;
    l = (long) l;
    f = (float) l;
    d = (double) l;

    b = (byte) f;
    c = (char) f;
    s = (short) f;
    i = (int) f;
    l = (long) f;
    f = (float) f;
    d = (double) f;

    b = (byte) d;
    c = (char) d;
    s = (short) d;
    i = (int) d;
    l = (long) d;
    f = (float) d;
    d = (double) d;
  }

  public void testReferenceToPrimitive() {
    Object o = new Object();
    boolean bool = (boolean) o;
    byte b = (byte) o;
    char c = (char) o;
    short s = (short) o;
    int i = (int) o;
    long l = (long) o;
    float f = (float) o;
    double d = (double) o;
  }

  public void testLiteralToPrimitive() {
    byte b;
    char c;
    short s;
    int i;
    long l;
    float f;
    double d;

    b = (byte) 1;
    c = (char) 1;
    s = (short) 1;
    i = (int) 1;
    l = (long) 1;
    f = (float) 1;
    d = (double) 1;

    b = (byte) 1L;
    c = (char) 1L;
    s = (short) 1L;
    i = (int) 1L;
    l = (long) 1L;
    f = (float) 1L;
    d = (double) 1L;

    b = (byte) 1.2f;
    c = (char) 1.2f;
    s = (short) 1.2f;
    i = (int) 1.2f;
    l = (long) 1.2f;
    f = (float) 1.2f;
    d = (double) 1.2f;

    b = (byte) 1.2;
    c = (char) 1.2;
    s = (short) 1.2;
    i = (int) 1.2;
    l = (long) 1.2;
    f = (float) 1.2;
    d = (double) 1.2;

    b = (byte) 'a';
    c = (char) 'a';
    s = (short) 'a';
    i = (int) 'a';
    l = (long) 'a';
    f = (float) 'a';
    d = (double) 'a';
  }

  public void testUnboxAndWiden() {
    Byte boxedByte = Byte.valueOf((byte) 0);

    // char c = (char) boxedByte; // illegal
    short s = (short) boxedByte;
    int i = (int) boxedByte;
    long l = (long) boxedByte;
    float f = (float) boxedByte;
    double d = (double) boxedByte;
  }

  public void testImplicitArgumentCasts() {
    final byte b = 127;
    final char c = 65535;
    final short s = 32767;
    final int i = 2147483647;
    final long l = 9223372036854775807L;
    final float f = 3.4028235E38f;
    final double d = 1.7976931348623157E308d;

    getShort(b);
    getInt(b);
    getLong(b);
    getFloat(b);
    getDouble(b);

    getFloat(l);
    getDouble(l);
    getDouble(f);

    // Do some of the same checks for NewInstance invocations.
    new IntValueHolder(b);
    new IntValueHolder(c);
    new IntValueHolder(s);

    new LongValueHolder(b);
    new LongValueHolder(c);
    new LongValueHolder(i);
  }

  public void testImplicitArrayIndexCasts(byte[] array) {
    final byte b = 127;
    final char c = 65535;
    final short s = 32767;
    final int i = 2147483647;

    byte result;
    result = array[b];
    result = array[c];
    result = array[s];
    result = array[i];

    result = array[1];
    result = array['a'];
  }

  private static short getShort(short value) {
    return value;
  }

  private static int getInt(int value) {
    return value;
  }

  private static long getLong(long value) {
    return value;
  }

  private static float getFloat(float value) {
    return value;
  }

  private static double getDouble(double value) {
    return value;
  }

  private static class IntValueHolder {
    IntValueHolder(int value) {}
  }

  private static class LongValueHolder {
    LongValueHolder(long value) {}
  }

  public void testImplicitLongAssignmentCasts() {
    byte fbyte = 11;
    char fchar = 12;
    short fshort = 13;
    int fint = 14;
    long flong = 15;
    float ffloat = 16;
    double fdouble = 17;

    // Initialized with not-a-long.
    long tlong = 0;

    // Direct assignments from smaller types.
    {
      tlong = fbyte;
      tlong = flong;
    }

    // Implicit casts to long when performing any assignment binary operation on a long and any
    // non-long type.
    {
      tlong = fint;
      tlong += fint;
      tlong <<= fint; // Does not cast to long on right hand side.
    }

    // Implicit casts to long when performing the PLUS_EQUALS binary operation on a long and any
    // non-long type.
    {
      tlong += fchar;
      tlong += flong;
      tlong += ffloat;
    }

    // Implicit casts to long when performing any non assignment binary operation on a long and any
    // non-long type.
    {
      tlong = flong * fint;
      tlong = flong >> fint; // Does not cast to long on right hand side.
    }

    // Implicit casts to long when performing the PLUS binary operation on a long and any non-long
    // type.
    {
      tlong = flong + fshort;
      tlong = flong + flong;
    }

    // Bit shift operations should coerce the right hand side to int (NOT long).
    {
      tlong = flong << tlong;
      tlong <<= flong;
    }
    // Repro for b/67599510
    {
      tlong = 0 + 1 + 2L;
    }
  }
}
