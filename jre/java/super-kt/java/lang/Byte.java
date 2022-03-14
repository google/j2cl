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

@KtNative("kotlin.Byte")
public final class Byte extends Number implements Comparable<Byte> {

  public static /* final */ byte MAX_VALUE;

  public static /* final */ byte MIN_VALUE;

  @KtName("SIZE_BYTES")
  public static /* final */ int SIZE;

  public static /* final */ Class<Byte> TYPE;

  public Byte(byte value) {}

  public Byte(String string) throws NumberFormatException {}

  @Override
  public native byte byteValue();

  public native int compareTo(Byte object);

  public static native int compare(byte lhs, byte rhs);

  public static native Byte decode(String string) throws NumberFormatException;

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

  public static native byte parseByte(String string) throws NumberFormatException;

  public static native byte parseByte(String string, int radix) throws NumberFormatException;

  @Override
  public native short shortValue();

  @Override
  public native String toString();

  public static native String toHexString(byte b, boolean upperCase);

  public static native String toString(byte value);

  public static native Byte valueOf(String string) throws NumberFormatException;

  public static native Byte valueOf(String string, int radix) throws NumberFormatException;

  public static native Byte valueOf(byte b);

  public static native int hashCode(byte b);
}
