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
package com.google.j2cl.transpiler.integration.returnimplicitcasts;

/**
 * Tests implicit conversion in function return values.
 * <p>
 * Illegal conversions are commented out.
 */
public class Main {
  public static final byte BYTE_MAX_VALUE = 127;
  public static final char CHAR_MAX_VALUE = 65535;
  public static final short SHORT_MAX_VALUE = 32767;
  public static final int INTEGER_MAX_VALUE = 2147483647;
  public static final long LONG_MAX_VALUE = 9223372036854775807L;
  public static final float FLOAT_MAX_VALUE = 3.4028235E38f;
  public static final double DOUBLE_MAX_VALUE = 1.7976931348623157E308d;

  private static char getCharFromByte() {
    return BYTE_MAX_VALUE;
  }

  private static short getShortFromByte() {
    return BYTE_MAX_VALUE;
  }

  private static int getIntFromByte() {
    return BYTE_MAX_VALUE;
  }

  private static long getLongFromByte() {
    return BYTE_MAX_VALUE;
  }

  private static float getFloatFromByte() {
    return BYTE_MAX_VALUE;
  }

  private static double getDoubleFromByte() {
    return BYTE_MAX_VALUE;
  }

  //  private static byte getByteFromChar() {
  //    return CHAR_MAX_VALUE; // illegal
  //  }
  //
  //  private static short getShortFromChar() {
  //    return CHAR_MAX_VALUE; // illegal
  //  }

  private static int getIntFromChar() {
    return CHAR_MAX_VALUE;
  }

  private static long getLongFromChar() {
    return CHAR_MAX_VALUE;
  }

  private static float getFloatFromChar() {
    return CHAR_MAX_VALUE;
  }

  private static double getDoubleFromChar() {
    return CHAR_MAX_VALUE;
  }

  //  private static byte getByteFromShort() {
  //    return SHORT_MAX_VALUE; // illegal
  //  }

  private static char getCharFromShort() {
    return SHORT_MAX_VALUE;
  }

  private static int getIntFromShort() {
    return SHORT_MAX_VALUE;
  }

  private static long getLongFromShort() {
    return SHORT_MAX_VALUE;
  }

  private static float getFloatFromShort() {
    return SHORT_MAX_VALUE;
  }

  private static double getDoubleFromShort() {
    return SHORT_MAX_VALUE;
  }

  //  private static byte getByteFromInt() {
  //    return INTEGER_MAX_VALUE; // illegal
  //  }
  //
  //  private static char getCharFromInt() {
  //    return INTEGER_MAX_VALUE; // illegal
  //  }
  //
  //  private static short getShortFromInt() {
  //    return INTEGER_MAX_VALUE; // illegal
  //  }

  private static long getLongFromInt() {
    return INTEGER_MAX_VALUE;
  }

  private static float getFloatFromInt() {
    return INTEGER_MAX_VALUE;
  }

  private static double getDoubleFromInt() {
    return INTEGER_MAX_VALUE;
  }

  //  private static byte getByteFromLong() {
  //    return LONG_MAX_VALUE; // illegal
  //  }
  //
  //  private static char getCharFromLong() {
  //    return LONG_MAX_VALUE; // illegal
  //  }
  //
  //  private static short getShortFromLong() {
  //    return LONG_MAX_VALUE; // illegal
  //  }
  //
  //  private static int getIntFromLong() {
  //    return LONG_MAX_VALUE; // illegal
  //  }

  private static float getFloatFromLong() {
    return LONG_MAX_VALUE;
  }

  private static double getDoubleFromLong() {
    return LONG_MAX_VALUE;
  }

  //  private static byte getByteFromFloat() {
  //    return FLOAT_MAX_VALUE; // illegal
  //  }
  //
  //  private static char getCharFromFloat() {
  //    return FLOAT_MAX_VALUE; // illegal
  //  }
  //
  //  private static short getShortFromFloat() {
  //    return FLOAT_MAX_VALUE; // illegal
  //  }
  //
  //  private static int getIntFromFloat() {
  //    return FLOAT_MAX_VALUE; // illegal
  //  }
  //
  //  private static long getLongFromFloat() {
  //    return FLOAT_MAX_VALUE; // illegal
  //  }

  private static double getDoubleFromFloat() {
    return FLOAT_MAX_VALUE;
  }

  //  private static byte getByteFromDouble() {
  //    return DOUBLE_MAX_VALUE; // illegal
  //  }
  //
  //  private static char getCharFromDouble() {
  //    return DOUBLE_MAX_VALUE; // illegal
  //  }
  //
  //  private static short getShortFromDouble() {
  //    return DOUBLE_MAX_VALUE; // illegal
  //  }
  //
  //  private static int getIntFromDouble() {
  //    return DOUBLE_MAX_VALUE; // illegal
  //  }
  //
  //  private static long getLongFromDouble() {
  //    return DOUBLE_MAX_VALUE; // illegal
  //  }
  //
  //  private static float getFloatFromDouble() {
  //    return DOUBLE_MAX_VALUE; // illegal
  //  }

  public static void main(String... args) {
    assert getCharFromByte() == (char) BYTE_MAX_VALUE;
    assert getShortFromByte() == (short) BYTE_MAX_VALUE;
    assert getIntFromByte() == (int) BYTE_MAX_VALUE;
    assert getLongFromByte() == (long) BYTE_MAX_VALUE;
    assert getFloatFromByte() == (float) BYTE_MAX_VALUE;
    assert getDoubleFromByte() == (double) BYTE_MAX_VALUE;
    //    assert getByteFromChar() == (byte) CHAR_MAX_VALUE; // illegal
    //    assert getShortFromChar() == (short) CHAR_MAX_VALUE; // illegal
    assert getIntFromChar() == (int) CHAR_MAX_VALUE;
    assert getLongFromChar() == (long) CHAR_MAX_VALUE;
    assert getFloatFromChar() == (float) CHAR_MAX_VALUE;
    assert getDoubleFromChar() == (double) CHAR_MAX_VALUE;
    //    assert getByteFromShort() == (byte) SHORT_MAX_VALUE; // illegal
    assert getCharFromShort() == (char) SHORT_MAX_VALUE;
    assert getIntFromShort() == (int) SHORT_MAX_VALUE;
    assert getLongFromShort() == (long) SHORT_MAX_VALUE;
    assert getFloatFromShort() == (float) SHORT_MAX_VALUE;
    assert getDoubleFromShort() == (double) SHORT_MAX_VALUE;
    //    assert getByteFromInt() == (byte) INTEGER_MAX_VALUE; // illegal
    //    assert getCharFromInt() == (char) INTEGER_MAX_VALUE; // illegal
    //    assert getShortFromInt() == (short) INTEGER_MAX_VALUE; // illegal
    assert getLongFromInt() == (long) INTEGER_MAX_VALUE;
    assert getFloatFromInt() == (float) INTEGER_MAX_VALUE;
    assert getDoubleFromInt() == (double) INTEGER_MAX_VALUE;
    //    assert getByteFromLong() == (byte) LONG_MAX_VALUE; // illegal
    //    assert getCharFromLong() == (char) LONG_MAX_VALUE; // illegal
    //    assert getShortFromLong() == (short) LONG_MAX_VALUE; // illegal
    //    assert getIntFromLong() == (int) LONG_MAX_VALUE; // illegal
    assert getFloatFromLong() == (float) LONG_MAX_VALUE;
    assert getDoubleFromLong() == (double) LONG_MAX_VALUE;
    //    assert getByteFromFloat() == (byte) FLOAT_MAX_VALUE; // illegal
    //    assert getCharFromFloat() == (char) FLOAT_MAX_VALUE; // illegal
    //    assert getShortFromFloat() == (short) FLOAT_MAX_VALUE; // illegal
    //    assert getIntFromFloat() == (int) FLOAT_MAX_VALUE; // illegal
    //    assert getLongFromFloat() == (long) FLOAT_MAX_VALUE; // illegal
    assert getDoubleFromFloat() == (double) FLOAT_MAX_VALUE;
    //    assert getByteFromDouble() == (byte) DOUBLE_MAX_VALUE; // illegal
    //    assert getCharFromDouble() == (char) DOUBLE_MAX_VALUE; // illegal
    //    assert getShortFromDouble() == (short) DOUBLE_MAX_VALUE; // illegal
    //    assert getIntFromDouble() == (int) DOUBLE_MAX_VALUE; // illegal
    //    assert getLongFromDouble() == (long) DOUBLE_MAX_VALUE; // illegal
    //    assert getFloatFromDouble() == (float) DOUBLE_MAX_VALUE; // illegal
  }
}
