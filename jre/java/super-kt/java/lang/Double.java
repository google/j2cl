/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.lang;

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtNative;

@KtNative("kotlin.Double")
public final class Double extends Number implements Comparable<Double> {

  public static /* final */ double MAX_VALUE;

  public static /* final */ double MIN_VALUE;

  public static /* final */ double NaN;

  public static /* final */ double POSITIVE_INFINITY;

  public static /* final */ double NEGATIVE_INFINITY;

  public static /* final */ double MIN_NORMAL;

  public static /* final */ int MAX_EXPONENT;

  public static /* final */ int MIN_EXPONENT;

  public static /* final */ Class<Double> TYPE;

  @KtName("SIZE_BITS")
  public static /* final */ int SIZE;

  public Double(double value) {}

  public Double(String string) throws NumberFormatException {}

  public native int compareTo(Double object);

  @Override
  public native byte byteValue();

  public static native long doubleToLongBits(double value);

  public static native long doubleToRawLongBits(double value);

  @Override
  public native double doubleValue();

  @Override
  public native boolean equals(Object object);

  @Override
  public native float floatValue();

  @Override
  public native int hashCode();

  @Override
  public native int intValue();

  public native boolean isInfinite();

  public static native boolean isInfinite(double d);

  public native boolean isNaN();

  public static native boolean isNaN(double d);

  @KtName("fromBits")
  public static native double longBitsToDouble(long bits);

  @Override
  public native long longValue();

  public static native double parseDouble(String string) throws NumberFormatException;

  @Override
  public native short shortValue();

  @Override
  public native String toString();

  public static native String toString(double d);

  public static native Double valueOf(String string) throws NumberFormatException;

  public static native int compare(double double1, double double2);

  public static native Double valueOf(double d);

  public static native String toHexString(double d);

  public static native int hashCode(double d);
}
