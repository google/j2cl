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

// TODO(b/222484176): This version of the file works for bootclasspath but in its current state it
// cannot be used as a source file for the J2KT JRE for Kotlin Native.
public final class Math {
  public static /* final */ double E;

  public static /* final */ double PI;

  private Math() {}

  public static native double abs(double d);

  public static native float abs(float f);

  public static native int abs(int i);

  public static native long abs(long l);

  public static native double acos(double d);

  public static native int addExact(int x, int y);

  public static native long addExact(long x, long y);

  public static native double asin(double d);

  public static native double atan(double d);

  public static native double atan2(double y, double x);

  public static native double cbrt(double d);

  public static native double ceil(double d);

  public static native double cos(double d);

  public static native double cosh(double d);

  public static native double exp(double d);

  public static native double expm1(double d);

  public static native double floor(double d);

  public static native int floorDiv(int dividend, int divisor);

  public static native long floorDiv(long dividend, long divisor);

  public static native int floorMod(int dividend, int divisor);

  public static native long floorMod(long dividend, long divisor);

  public static native double hypot(double x, double y);

  public static native double IEEEremainder(double x, double y);

  public static native double log(double d);

  public static native double log10(double d);

  public static native double log1p(double d);

  public static native double max(double d1, double d2);

  public static native float max(float f1, float f2);

  public static native int max(int i1, int i2);

  public static native long max(long l1, long l2);

  public static native double min(double d1, double d2);

  public static native float min(float f1, float f2);

  public static native int min(int i1, int i2);

  public static native long min(long l1, long l2);

  public static native int multiplyExact(int x, int y);

  public static native long multiplyExact(long x, long y);

  public static native int negateExact(int x);

  public static native long negateExact(long x);

  public static native double pow(double x, double y);

  public static native double rint(double d);

  public static native long round(double d);

  public static native int round(float f);

  public static native double signum(double d);

  public static native float signum(float f);

  public static native double sin(double d);

  public static native double sinh(double d);

  public static native double sqrt(double d);

  public static native int subtractExact(int x, int y);

  public static native long subtractExact(long x, long y);

  public static native double tan(double d);

  public static native double tanh(double d);

  public static native double random();

  public static native double toRadians(double angdeg);

  public static native double toDegrees(double angrad);

  public static native int toIntExact(long x);

  public static native double ulp(double d);

  public static native float ulp(float f);

  public static native double copySign(double magnitude, double sign);

  public static native float copySign(float magnitude, float sign);

  public static native int getExponent(float f);

  public static native int getExponent(double d);

  public static native double nextAfter(double start, double direction);

  public static native float nextAfter(float start, double direction);

  public static native double nextUp(double d);

  public static native float nextUp(float f);

  public static native double scalb(double d, int scaleFactor);

  public static native float scalb(float d, int scaleFactor);
}
