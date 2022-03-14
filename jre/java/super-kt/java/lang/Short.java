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

@KtNative("kotlin.Short")
public final class Short extends Number implements Comparable<Short> {

  public static /* final */ short MAX_VALUE;

  public static /* final */ short MIN_VALUE;

  @KtName("SIZE_BITS")
  public static /* final */ int SIZE;

  public static /* final */ Class<Short> TYPE;

  public Short(String string) throws NumberFormatException {}

  public Short(short value) {}

  @Override
  public native byte byteValue();

  public native int compareTo(Short object);

  public static native int compare(short lhs, short rhs);

  public static native Short decode(String string) throws NumberFormatException;

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

  @Override
  public native long longValue();

  public static native short parseShort(String string) throws NumberFormatException;

  public static native short parseShort(String string, int radix) throws NumberFormatException;

  @Override
  public native short shortValue();

  @Override
  public native String toString();

  public static native String toString(short value);

  public static native Short valueOf(String string) throws NumberFormatException;

  public static native Short valueOf(String string, int radix) throws NumberFormatException;

  public static native short reverseBytes(short s);

  public static native Short valueOf(short s);

  public static native int hashCode(short s);
}
