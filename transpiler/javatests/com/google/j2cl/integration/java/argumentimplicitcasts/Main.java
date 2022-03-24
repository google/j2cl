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
package argumentimplicitcasts;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Tests implicit conversion in method call argument values.
 * <p>
 * Illegal conversions are commented out.
 */
public class Main {
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

  private static final byte BYTE_MAX_VALUE = 127;
  private static final char CHAR_MAX_VALUE = 65535;
  private static final short SHORT_MAX_VALUE = 32767;
  private static final int INTEGER_MAX_VALUE = 2147483647;
  private static final long LONG_MAX_VALUE = 9223372036854775807L;
  private static final float FLOAT_MAX_VALUE = 3.4028235E38f;
  private static final double DOUBLE_MAX_VALUE = 1.7976931348623157E308d;

  private static final byte BYTE_MIN_VALUE = -128;
  private static final char CHAR_MIN_VALUE = 0;
  private static final short SHORT_MIN_VALUE = -32768;
  private static final int INTEGER_MIN_VALUE = -2147483648;
  private static final long LONG_MIN_VALUE = -9223372036854775808L;
  private static final float FLOAT_MIN_VALUE = 1.4E-45f;
  private static final double DOUBLE_MIN_VALUE = 4.9E-324;

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

    // assertTrue(getChar(byteValue) == (char) byteValue); // illegal
    assertTrue(getShort(byteValue) == (short) byteValue);
    assertTrue(getInt(byteValue) == (int) byteValue);
    assertTrue(getLong(byteValue) == (long) byteValue);
    assertTrue(getFloat(byteValue) == (float) byteValue);
    assertTrue(getDouble(byteValue) == (double) byteValue);

    // assertTrue(getByte(charValue) == (byte) charValue); // illegal
    // assertTrue(getShort(charValue) == (short) charValue); // illegal
    assertTrue(getInt(charValue) == (int) charValue);
    assertTrue(getLong(charValue) == (long) charValue);
    assertTrue(getFloat(charValue) == (float) charValue);
    assertTrue(getDouble(charValue) == (double) charValue);

    // assertTrue(getByte(shortValue) == (byte) shortValue); // illegal
    // assertTrue(getChar(shortValue) == (char) shortValue); // illegal
    assertTrue(getInt(shortValue) == (int) shortValue);
    assertTrue(getLong(shortValue) == (long) shortValue);
    assertTrue(getFloat(shortValue) == (float) shortValue);
    assertTrue(getDouble(shortValue) == (double) shortValue);

    // assertTrue(getByte(integerValue) == (byte) integerValue); // illegal
    // assertTrue(getChar(integerValue) == (char) integerValue); // illegal
    // assertTrue(getShort(integerValue) == (short) integerValue); // illegal
    assertTrue(getLong(integerValue) == (long) integerValue);
    assertTrue(getFloat(integerValue) == (float) integerValue);
    assertTrue(getDouble(integerValue) == (double) integerValue);

    // assertTrue(getByte(longValue) == (byte) longValue); // illegal
    // assertTrue(getChar(longValue) == (char) longValue); // illegal
    // assertTrue(getShort(longValue) == (short) longValue); // illegal
    // assertTrue(getInt(longValue) == (int) longValue); // illegal
    assertTrue(getFloat(longValue) == (float) longValue);
    assertTrue(getDouble(longValue) == (double) longValue);

    // assertTrue(getByte(floatValue) == (byte) floatValue); // illegal
    // assertTrue(getChar(floatValue) == (char) floatValue); // illegal
    // assertTrue(getShort(floatValue) == (short) floatValue); // illegal
    // assertTrue(getInt(floatValue) == (int) floatValue); // illegal
    // assertTrue(getLong(floatValue) == (long) floatValue); // illegal
    assertTrue(getDouble(floatValue) == (double) floatValue);

    // assertTrue(getByte(doubleValue) == (byte) doubleValue); // illegal
    // assertTrue(getChar(doubleValue) == (char) doubleValue); // illegal
    // assertTrue(getShort(doubleValue) == (short) doubleValue); // illegal
    // assertTrue(getInt(doubleValue) == (int) doubleValue); // illegal
    // assertTrue(getLong(doubleValue) == (long) doubleValue); // illegal
    // assertTrue(getFloat(doubleValue) == (float) doubleValue); // illegal

    // Do some of the same checks for NewInstance invocations.
    assertTrue(new IntValueHolder(byteValue).value == (int) byteValue);
    assertTrue(new IntValueHolder(charValue).value == (int) charValue);
    assertTrue(new IntValueHolder(shortValue).value == (int) shortValue);
    // assertTrue(new IntValueHolder(longValue).value == (int) longValue); // illegal
    // assertTrue(new IntValueHolder(floatValue).value == (int) floatValue); // illegal
    // assertTrue(new IntValueHolder(doubleValue).value == (int) doubleValue); // illegal

    assertTrue(new LongValueHolder(byteValue).value == (long) byteValue);
    assertTrue(new LongValueHolder(charValue).value == (long) charValue);
    assertTrue(new LongValueHolder(shortValue).value == (long) shortValue);
    // assertTrue(new LongValueHolder(intValue).value == (long) intValue); // illegal
    // assertTrue(new LongValueHolder(floatValue).value == (long) floatValue); // illegal
    // assertTrue(new LongValueHolder(doubleValue).value == (long) doubleValue); // illegal
  }
}
