/*
 * Copyright 2007 Google Inc.
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
package java.lang;

import javaemul.internal.annotations.HasNoSideEffects;

/**
 * Wraps native <code>byte</code> as an object.
 */
public final class Byte extends Number implements Comparable<Byte> {

  public static final byte MIN_VALUE = (byte) 0x80;
  public static final byte MAX_VALUE = (byte) 0x7F;
  public static final int SIZE = 8;
  public static final int BYTES = SIZE / Byte.SIZE;
  public static final Class<Byte> TYPE = byte.class;

  /**
   * Use nested class to avoid clinit on outer.
   */
  private static class BoxedValues {
    private static final Byte[] boxedValues;

    static {
      // Box all values according to JLS
      Byte[] values = new Byte[256];
      for (int i = 0; i < 256; i++) {
        values[i] = new Byte((byte) (i - 128));
      }
      boxedValues = values;
    }

    @HasNoSideEffects
    private static Byte get(byte b) {
      return boxedValues[b + 128];
    }
  }

  public static int compare(byte x, byte y) {
    return x - y;
  }

  public static Byte decode(String s) throws NumberFormatException {
    return Byte.valueOf((byte) __decodeAndValidateInt(s, MIN_VALUE, MAX_VALUE));
  }

  public static int hashCode(byte b) {
    return b;
  }

  public static byte parseByte(String s) throws NumberFormatException {
    return parseByte(s, 10);
  }

  public static byte parseByte(String s, int radix)
      throws NumberFormatException {
    return (byte) __parseAndValidateInt(s, radix, MIN_VALUE, MAX_VALUE);
  }

  public static String toString(byte b) {
    return String.valueOf(b);
  }

  public static Byte valueOf(byte b) {
    return BoxedValues.get(b);
  }

  public static Byte valueOf(String s) throws NumberFormatException {
    return valueOf(s, 10);
  }

  public static Byte valueOf(String s, int radix) throws NumberFormatException {
    return Byte.valueOf(Byte.parseByte(s, radix));
  }

  private final byte value;

  public Byte(byte value) {
    this.value = value;
  }

  public Byte(String s) {
    value = parseByte(s);
  }

  @Override
  public byte byteValue() {
    return value;
  }

  @Override
  public int compareTo(Byte b) {
    return compare(value, b.value);
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Byte) && (((Byte) o).value == value);
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return hashCode(value);
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public short shortValue() {
    return value;
  }

  @Override
  public String toString() {
    return toString(value);
  }
}
