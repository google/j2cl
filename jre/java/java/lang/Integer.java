/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Integer.html">the
 * official Java API doc</a> for details.
 */
public class Integer extends Number {
  private int value;

  public Integer(int value) {
    this.value = value;
  }

  public static String toHexString(int value) {
    return "hex";
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    // TODO: remove explicit cast after implicit cast is supported.
    return (long) value;
  }

  @Override
  public short shortValue() {
    return (short) value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Integer) && (((Integer) o).value == value);
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public String toString() {
    return "" + value;
  }
}
