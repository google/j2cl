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

import jsinterop.annotations.JsMethod;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Double.html">the
 * official Java API doc</a> for details.
 */
public class Double extends Number implements Comparable<Double> {

  public static Double valueOf(double d) {
    return null;
  }

  public Double(double value) {
    // Super-source replaced.
  }

  @Override
  public byte byteValue() {
    return (byte) doubleValue();
  }

  @Override
  public double doubleValue() {
    return checkNotNull(this);
  }

  @Override
  public float floatValue() {
    return (float) doubleValue();
  }

  @Override
  public int intValue() {
    return (int) doubleValue();
  }

  @Override
  public long longValue() {
    return (long) doubleValue();
  }

  @Override
  public short shortValue() {
    return (short) doubleValue();
  }

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public int hashCode() {
    return (int) doubleValue();
  }

  @Override
  public String toString() {
    // Super-source replaced.
    throw new RuntimeException();
  }

  public static int compare(double x, double y) {
    if (x < y) {
      return -1;
    }
    if (x > y) {
      return 1;
    }
    return 0;
  }

  @Override
  public int compareTo(Double d) {
    return compare(doubleValue(), d.doubleValue());
  }

  public static double checkNotNull(Double d) {
    if (d == null) {
      throw new NullPointerException();
    }
    return d;
  }

  @JsMethod(name = "$create__double")
  public static double create(double d) {
    return nativeCreate(d);
  }

  private static native double nativeCreate(double b) /*-{Double.$clinit(); return b;}-*/;

  @JsMethod(name = "$isInstance")
  public static boolean isInstance(Object instance) {
    return nativeIsInstance(instance);
  }

  private static native boolean nativeIsInstance(
      Object instance) /*-{return typeof instance == 'number';}-*/;
}
