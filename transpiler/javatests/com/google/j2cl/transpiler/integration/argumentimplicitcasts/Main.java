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
package com.google.j2cl.transpiler.integration.argumentimplicitcasts;

/**
 * Tests implicit conversion in method call argument values.
 * <p>
 * Illegal conversions are commented out.
 */
public class Main {

  private static class IntValueHolder {
    private int value;

    public IntValueHolder(int value) {
      this.value = value;
    }
  }

  private static class LongValueHolder {
    private long value;

    public LongValueHolder(long value) {
      this.value = value;
    }
  }

  public static final byte BYTE_MAX_VALUE = 127;
  public static final char CHAR_MAX_VALUE = 65535;
  public static final short SHORT_MAX_VALUE = 32767;
  public static final int INTEGER_MAX_VALUE = 2147483647;
  public static final long LONG_MAX_VALUE = 9223372036854775807L;
  public static final float FLOAT_MAX_VALUE = 3.4028235E38f;
  public static final double DOUBLE_MAX_VALUE = 1.7976931348623157E308d;

  public static final byte BYTE_MIN_VALUE = -128;
  public static final char CHAR_MIN_VALUE = 0;
  public static final short SHORT_MIN_VALUE = -32768;
  public static final int INTEGER_MIN_VALUE = -2147483648;
  public static final long LONG_MIN_VALUE = -9223372036854775808L;
  public static final float FLOAT_MIN_VALUE = 1.4E-45f;
  public static final double DOUBLE_MIN_VALUE = 4.9E-324;

  @SuppressWarnings("unused")
  private static char getChar(char value) {
    return value;
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

  @SuppressWarnings("unused")
  private static byte getByte(byte value) {
    return value;
  }

  public static void main(String... args) {
    assertAllTypeConversions(
        BYTE_MAX_VALUE,
        CHAR_MAX_VALUE,
        SHORT_MAX_VALUE,
        INTEGER_MAX_VALUE,
        LONG_MAX_VALUE,
        FLOAT_MAX_VALUE,
        DOUBLE_MAX_VALUE);

    assertAllTypeConversions(
        BYTE_MIN_VALUE,
        CHAR_MIN_VALUE,
        SHORT_MIN_VALUE,
        INTEGER_MIN_VALUE,
        LONG_MIN_VALUE,
        FLOAT_MIN_VALUE,
        DOUBLE_MIN_VALUE);
  }

  @SuppressWarnings("unused")
  private static void assertAllTypeConversions(
      byte byteValue,
      char charValue,
      short shortValue,
      int integerValue,
      long longValue,
      float floatValue,
      double doubleValue) {
    // The right hand sides are explicitly cast and the left hand sides *should* have implicit casts
    // being inserted when the value is arriving as an argument to the function. As a result if
    // implicit casts are being inserted then all of these comparisons should come out true.

    // Also around long / non long boundaries if we fail to insert implicit casts it will be a
    // JSComp compile error since it will see 'Long' on one side and 'number' on the other.

    // assert getChar(byteValue) == (char) byteValue; // illegal
    assert getShort(byteValue) == (short) byteValue;
    assert getInt(byteValue) == (int) byteValue;
    assert getLong(byteValue) == (long) byteValue;
    assert getFloat(byteValue) == (float) byteValue;
    assert getDouble(byteValue) == (double) byteValue;

    // assert getByte(charValue) == (byte) charValue; // illegal
    // assert getShort(charValue) == (short) charValue; // illegal
    assert getInt(charValue) == (int) charValue;
    assert getLong(charValue) == (long) charValue;
    assert getFloat(charValue) == (float) charValue;
    assert getDouble(charValue) == (double) charValue;

    // assert getByte(shortValue) == (byte) shortValue; // illegal
    // assert getChar(shortValue) == (char) shortValue; // illegal
    assert getInt(shortValue) == (int) shortValue;
    assert getLong(shortValue) == (long) shortValue;
    assert getFloat(shortValue) == (float) shortValue;
    assert getDouble(shortValue) == (double) shortValue;

    // assert getByte(integerValue) == (byte) integerValue; // illegal
    // assert getChar(integerValue) == (char) integerValue; // illegal
    // assert getShort(integerValue) == (short) integerValue; // illegal
    assert getLong(integerValue) == (long) integerValue;
    assert getFloat(integerValue) == (float) integerValue;
    assert getDouble(integerValue) == (double) integerValue;

    // assert getByte(longValue) == (byte) longValue; // illegal
    // assert getChar(longValue) == (char) longValue; // illegal
    // assert getShort(longValue) == (short) longValue; // illegal
    // assert getInt(longValue) == (int) longValue; // illegal
    assert getFloat(longValue) == (float) longValue;
    assert getDouble(longValue) == (double) longValue;

    // assert getByte(floatValue) == (byte) floatValue; // illegal
    // assert getChar(floatValue) == (char) floatValue; // illegal
    // assert getShort(floatValue) == (short) floatValue; // illegal
    // assert getInt(floatValue) == (int) floatValue; // illegal
    // assert getLong(floatValue) == (long) floatValue; // illegal
    assert getDouble(floatValue) == (double) floatValue;

    // assert getByte(doubleValue) == (byte) doubleValue; // illegal
    // assert getChar(doubleValue) == (char) doubleValue; // illegal
    // assert getShort(doubleValue) == (short) doubleValue; // illegal
    // assert getInt(doubleValue) == (int) doubleValue; // illegal
    // assert getLong(doubleValue) == (long) doubleValue; // illegal
    // assert getFloat(doubleValue) == (float) doubleValue; // illegal

    // Do some of the same checks for NewInstance invocations.
    assert new IntValueHolder(byteValue).value == (int) byteValue;
    assert new IntValueHolder(charValue).value == (int) charValue;
    assert new IntValueHolder(shortValue).value == (int) shortValue;
    // assert new IntValueHolder(longValue).value == (int) longValue; // illegal
    // assert new IntValueHolder(floatValue).value == (int) floatValue; // illegal
    // assert new IntValueHolder(doubleValue).value == (int) doubleValue; // illegal

    assert new LongValueHolder(byteValue).value == (long) byteValue;
    assert new LongValueHolder(charValue).value == (long) charValue;
    assert new LongValueHolder(shortValue).value == (long) shortValue;
    // assert new LongValueHolder(intValue).value == (long) intValue; // illegal
    // assert new LongValueHolder(floatValue).value == (long) floatValue; // illegal
    // assert new LongValueHolder(doubleValue).value == (long) doubleValue; // illegal
  }
}
