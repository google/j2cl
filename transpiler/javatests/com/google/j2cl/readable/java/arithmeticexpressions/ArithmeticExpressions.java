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
package arithmeticexpressions;

import org.jspecify.annotations.Nullable;

public class ArithmeticExpressions {
  private static final float FLOAT_CONSTANT = 1.1f;
  private static final double DOUBLE_CONSTANT = FLOAT_CONSTANT;
  private static final double DOUBLE_CONSTANT_WITH_ARITHMETIC = FLOAT_CONSTANT + FLOAT_CONSTANT;

  public void testCoercions() {
    byte b = (byte) 1L;
    char c = (char) 1L;
    short s = (short) 1L;
    int i = (int) 1L;
    float f = (float) 1L;
    double d = (double) 1L;
    b = (byte) 9223372036854775807L; // -1
    c = (char) 9223372036854775807L; // 65535
    s = (short) 9223372036854775807L; // -1
    i = (int) 9223372036854775807L; // -1
    f = (float) 9223372036854775807L; //  9.223372036854776E18
    d = (double) 9223372036854775807L; //  9.223372036854776E18
    Object o = c;
    s = (short) (char) o;
  }

  public void testPrimitives() {
    int a = 10,
        b = a++,
        c = a--,
        d = ++a,
        e = --a,
        f = -a,
        g = +a,
        h = ~a,
        i = 1 + 1 + 2 - 5,
        j = (1 + 2) * (3 + 4),
        p = 1 / 2 * 3 % 4,
        r = -0x80000000,
        t = -(-(-1)),
        u = +(+(+1)),
        v = -(+(-1)),
        w = 5 - (-4);
    boolean k = !(1 + 2 + 3 == 4), l = (1 + 2 != 4), m = Long.MAX_VALUE != 9223372036854776833d;
    double o = ((5 + 1) / 2) - 0.0;

    a = a << 31L;
    a <<= 1L;
    a += 1L;
    a /= 1L;
    a += Double.MAX_VALUE; // if not expanded and performed in double result will change.

    ((k)) |= true;

    short s = 10;
    k = ++s == 10;

    int q = 3 >> 2;
    q = 3 >>> 2;

    byte x = (byte) (((short) a + (byte) (short) b) * (short) (byte) c);

    char y = 'y';
    Long z = 121L;
    k = y == z;
  }

  // Compount assignments in static fields
  public static long one = 1;
  public static long foo = one++;
  public long bar = foo++;

  public void testCompoundArray() {
    int[] ints = null;
    ints[0] += 1;
    ints[0] -= 1;
    ints[0] *= 1;
    ints[0] /= 1;
    ints[0] &= 1;
    ints[0] ^= 1;
    ints[0] |= 1;
    ints[0] %= 1;
    ints[0] <<= 1;
    ints[0] >>= 1;
    ints[0] >>>= 1;
    ints[0]++;
    ++ints[0];
    int i = 0;
    ints[i++]++;
    ++ints[++i];
    ints[i++] /= 1;

    long[] longs = null;
    longs[0] += 1;
    longs[0]--;
    --longs[0];
    getLongArray()[0]++;

    boolean[] booleans = null;
    booleans[0] |= true;

    String[] strings = null;
    strings[0] += null;

    short[] shorts = null;
    boolean b = ++shorts[0] == 10;
  }

  private static long[] getLongArray() {
    return null;
  }

  public void testCompoundBoxedTypes() {
    Integer c = 1000;

    // Compound expressions.
    Integer d = 10000;
    d += c;

    int i = 43;
    d += i;
    d <<= i;
    i += c;

    // Prefix expressions;
    Integer e = ++c;
    e = ++c;
    Double e2 = 100d;
    ++e2;

    // Postfix expressions.
    Integer f = c++;
    f = c++;
    Byte b = 0;
    b++;
    Character ch = 'c';
    ch++;

    // Method call on result of arithmetic expressions.
    (++f).intValue();
    (f--).intValue();

    class Ref<T> {
      @Nullable T field;
    }

    Ref<Byte> ref = null;
    ref.field++;
    int n = 1 + ref.field;
  }

  private static Integer getInteger() {
    return null;
  }

  long intField;

  private static void testSideEffect() {
    getWithSideEffect().intField += 5;
  }

  private static ArithmeticExpressions getWithSideEffect() {
    return null;
  }

  // This is a readable example that exposes the nuances of JDT representation of InfixExpression,
  // where certain expressions are represented as operator, list of operands rather than a
  // nested binary tree structure.
  public void testExtendedOperands() {
    Integer boxedInteger = 3;
    int i;
    long l;
    double d;
    l = 2 - boxedInteger - 2L;
    l = 2 | boxedInteger | 2L;
    l = 1000000L * l * 60 * 60 * 24;
    l = 24 * 60 * 60 * l * 1000000L;
    d = l = i = 20;
    l = boxedInteger = i = 20;
    l = i + boxedInteger + l + 20;
    d = 20 + l + d;
  }

  public void testEffectivelyFinalVariableInAssignmentExpression(boolean condition) {
    long effectivelyFinal;
    // "condition &&" is necessary to trigger a problem from b/251115461
    if (condition && ((effectivelyFinal = bar) != 0)) {
      long unused = effectivelyFinal;
    }
  }

  private static long counter = 0;

  private static long incrementCounter() {
    return ++counter;
  }
}
