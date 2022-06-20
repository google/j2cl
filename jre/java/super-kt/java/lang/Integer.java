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
import jsinterop.annotations.JsNonNull;

@KtNative("kotlin.Int")
public final class Integer extends Number implements Comparable<Integer> {

  public static /* final */ int MAX_VALUE;

  public static /* final */ int MIN_VALUE;

  @KtName("SIZE_BITS")
  public static /* final */ int SIZE;

  public static /* final */ Class<Integer> TYPE;

  public Integer(int value) {}

  public Integer(@JsNonNull String string) throws NumberFormatException {}

  @Override
  public native byte byteValue();

  public native int compareTo(@JsNonNull Integer object);

  public static native int compare(int lhs, int rhs);

  public static native Integer decode(String string) throws NumberFormatException;

  @Override
  public native double doubleValue();

  @Override
  public native boolean equals(Object o);

  @Override
  public native float floatValue();

  public static native Integer getInteger(String string);

  public static native Integer getInteger(String string, int defaultValue);

  public static native Integer getInteger(String string, Integer defaultValue);

  @Override
  public native int hashCode();

  @Override
  public native int intValue();

  @Override
  public native long longValue();

  public static native int parseInt(@JsNonNull String string) throws NumberFormatException;

  public static native int parseInt(@JsNonNull String string, int radix)
      throws NumberFormatException;

  @Override
  public native short shortValue();

  public static native String toBinaryString(int i);

  public static native String toHexString(int i);

  public static native String toOctalString(int i);

  @Override
  public native String toString();

  public static native String toString(int i);

  public static native String toString(int i, int radix);

  public static native Integer valueOf(String string) throws NumberFormatException;

  public static native Integer valueOf(String string, int radix) throws NumberFormatException;

  public static native int highestOneBit(int i);

  public static native int lowestOneBit(int i);

  public static native int numberOfLeadingZeros(int i);

  public static native int numberOfTrailingZeros(int i);

  public static native int bitCount(int i);

  public static native int rotateLeft(int i, int distance);

  public static native int rotateRight(int i, int distance);

  public static native int reverseBytes(int i);

  public static native int reverse(int i);

  public static native int signum(int i);

  public static native Integer valueOf(int i);

  public static native int hashCode(int i);
}
