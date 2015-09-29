package com.google.j2cl.transpiler.readable.argumentimplicitcasts;

/**
 * Tests implicit conversion in method call argument values.
 * <p>
 * Illegal conversions are commented out.
 */
public class ArgumentImplicitCasts {

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
    // The right hand sides are explicitly cast and the left hand sides *should* have implicit casts
    // being inserted when the value is arriving as an argument to the function. As a result if
    // implicit casts are being inserted then all of these comparisons should come out true.

    // Also around long / non long boundaries if we fail to insert implicit casts it will be a
    // JSComp compile error since it will see 'Long' on one side and 'number' on the other.

    // assert getChar(byteValue) == (char) byteValue; // illegal
    assert getShort(BYTE_MAX_VALUE) == (short) BYTE_MAX_VALUE;
    assert getInt(BYTE_MAX_VALUE) == (int) BYTE_MAX_VALUE;
    assert getLong(BYTE_MAX_VALUE) == (long) BYTE_MAX_VALUE;
    assert getFloat(BYTE_MAX_VALUE) == (float) BYTE_MAX_VALUE;
    assert getDouble(BYTE_MAX_VALUE) == (double) BYTE_MAX_VALUE;

    // assert getByte(longValue) == (byte) longValue; // illegal
    // assert getChar(longValue) == (char) longValue; // illegal
    // assert getShort(longValue) == (short) longValue; // illegal
    // assert getInt(longValue) == (int) longValue; // illegal
    assert getFloat(LONG_MAX_VALUE) == (float) LONG_MAX_VALUE;
    assert getDouble(LONG_MAX_VALUE) == (double) LONG_MAX_VALUE;

    // assert getByte(floatValue) == (byte) floatValue; // illegal
    // assert getChar(floatValue) == (char) floatValue; // illegal
    // assert getShort(floatValue) == (short) floatValue; // illegal
    // assert getInt(floatValue) == (int) floatValue; // illegal
    // assert getLong(floatValue) == (long) floatValue; // illegal
    assert getDouble(FLOAT_MAX_VALUE) == (double) FLOAT_MAX_VALUE;

    // Do some of the same checks for NewInstance invocations.
    assert new IntValueHolder(BYTE_MAX_VALUE).value == (int) BYTE_MAX_VALUE;
    assert new IntValueHolder(CHAR_MAX_VALUE).value == (int) CHAR_MAX_VALUE;
    assert new IntValueHolder(SHORT_MAX_VALUE).value == (int) SHORT_MAX_VALUE;
    // assert new IntValueHolder(longValue).value == (int) longValue; // illegal
    // assert new IntValueHolder(floatValue).value == (int) floatValue; // illegal
    // assert new IntValueHolder(doubleValue).value == (int) doubleValue; // illegal

    assert new LongValueHolder(BYTE_MAX_VALUE).value == (long) BYTE_MAX_VALUE;
    assert new LongValueHolder(CHAR_MAX_VALUE).value == (long) CHAR_MAX_VALUE;
    assert new LongValueHolder(SHORT_MAX_VALUE).value == (long) SHORT_MAX_VALUE;
    // assert new LongValueHolder(intValue).value == (long) intValue; // illegal
    // assert new LongValueHolder(floatValue).value == (long) floatValue; // illegal
    // assert new LongValueHolder(doubleValue).value == (long) doubleValue; // illegal
  }
}
