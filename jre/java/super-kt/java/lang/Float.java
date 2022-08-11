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

@KtNative("kotlin.Float")
public final class Float extends Number implements Comparable<Float> {

  public static /* final */ float MAX_VALUE;

  public static /* final */ float MIN_VALUE;

  public static /* final */ float NaN;

  public static /* final */ float POSITIVE_INFINITY;

  public static /* final */ float NEGATIVE_INFINITY;

  public static /* final */ float MIN_NORMAL;

  public static /* final */ int MAX_EXPONENT;

  public static /* final */ int MIN_EXPONENT;

  public static /* final */ Class<Float> TYPE;

  @KtName("SIZE_BITS")
  public static /* final */ int SIZE;

  public Float(float value) {}

  public Float(double value) {}

  public Float(String string) throws NumberFormatException {}

  public native int compareTo(Float object);

  @Override
  public native byte byteValue();

  @Override
  public native double doubleValue();

  @Override
  public native boolean equals(Object object);

  public static native int floatToIntBits(float value);

  public static native int floatToRawIntBits(float value);

  @Override
  public native float floatValue();

  @Override
  public native int hashCode();

  @KtName("fromBits")
  public static native float intBitsToFloat(int bits);

  @Override
  public native int intValue();

  public native boolean isInfinite();

  public static native boolean isInfinite(float f);

  public native boolean isNaN();

  public static native boolean isNaN(float f);

  @Override
  public native long longValue();

  public static native float parseFloat(String string) throws NumberFormatException;

  @Override
  public native short shortValue();

  @Override
  public native String toString();

  public static native String toString(float f);

  public static native Float valueOf(String string) throws NumberFormatException;

  public static native int compare(float float1, float float2);

  public static native Float valueOf(float f);

  public static native String toHexString(float f);

  public static native int hashCode(float f);
}
