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
package unaryexpressions;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotSame;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isJ2Kt;

/** Test unary operations. */
public class Main {

  public static void main(String... args) {
    testBasicUnaryExpressions();
    testMinusZero();
    testUnaryNumericPromotion();
  }

  public static void testBasicUnaryExpressions() {
    int a = 10;
    assertTrue(a == 10);

    int b = a++;
    assertTrue(a == 11);
    assertTrue(b == 10);

    int c = a--;
    assertTrue(a == 10);
    assertTrue(c == 11);

    int d = ++a;
    assertTrue(a == 11);
    assertTrue(d == 11);

    int e = --a;
    assertTrue(a == 10);
    assertTrue(e == 10);

    int f = -a;
    assertTrue(f == -10);

    int g = +a;
    assertTrue(g == 10);

    int h = ~a;
    assertTrue(h == ~10);

    boolean i = (a == 100);
    assertTrue(!i);

    short s = 10;
    // Test that the result of evaluating "++s" is correct.
    // DO NOT CHANGE to "assertEquals(++s, 11)" since the tests also make sure that the expressions
    // are transformed correctly w.r.t. operator precedence.
    assertTrue(++s == 11);
    // Test that ++s correctly mutates "s". Also indirectly tests that "++s == 11" is not
    // incorrectly expanded to "s = s + 1 == 11", which would have resulted in "s" having "true" as
    // its value.
    assertTrue(s == 11);

    // Test that the result of evaluating "s++" is correct.
    assertTrue(s++ == 11);
    // Test that ++s correctly mutates "s".
    assertTrue(s == 12);

    short[] shorts = new short[] {1, 2, 3};
    // Test that the result of evaluating "++s" is correct.
    // DO NOT CHANGE to "assertEquals(++s, 11)" since the tests also make sure that the expressions
    // are transformed correctly w.r.t. operator precedence.
    assertTrue(++shorts[1] == 3);
    // Test that ++s correctly mutates "s". Also indirectly tests that "++s == 11" is not
    // incorrectly expanded to "s = s + 1 == 11", which would have resulted in "s" having "true" as
    // its value.
    assertTrue(shorts[1] == 3);

    double l = -0x80000000; // -Integer.MIN_VALUE
    assertTrue(l == Integer.MIN_VALUE);

    // Test space after - and + unary operators, so they don't behave like -- and ++.
    assertTrue(-(-1) == 1);
    assertTrue(-(-(1)) == 1);
    assertTrue(+(+1) == 1);
    assertTrue(+(+(1)) == 1);
    assertTrue(-(-1.0) == 1.0);
    assertTrue(-(-(1.0)) == 1.0);
    assertTrue(+(+1.0) == 1.0);
    assertTrue(+(+(1.0)) == 1.0);
  }

  private static void testMinusZero() {
    double zero = 0.0;
    Double boxedZero = 0.0;
    Float boxedFloatZero = 0.0f;
    assertEquals(Double.NEGATIVE_INFINITY, 1 / -zero);
    assertEquals(Double.NEGATIVE_INFINITY, 1 / -boxedZero);
    assertEquals(Float.NEGATIVE_INFINITY, 1 / -boxedFloatZero);
  }

  @SuppressWarnings("BoxedPrimitiveConstructor")
  private static void testUnaryNumericPromotion() {
    byte b = 1;
    short s = 2;
    char c = 'a';
    assertEquals(Integer.valueOf(1), box(+b));
    assertEquals(Integer.valueOf(2), box(+s));
    assertEquals(Integer.valueOf('a'), box(+c));
    assertEquals("97", "" + +c);
    assertEquals("-97", "" + -c);
    assertEquals("-98", "" + ~c);
    long l = +c;
    assertEquals(97L, l);

    Byte boxedB = 1;
    Short boxedS = 2;
    Character boxedC = 'a';
    assertEquals(Integer.valueOf(1), box(+boxedB));
    assertEquals(Integer.valueOf(2), box(+boxedS));
    assertEquals(Integer.valueOf('a'), box(+boxedC));
    assertEquals("97", "" + +boxedC);

    if (!isJ2Kt()) {
      // new Integer(...) does not create a distinct instance in J2KT.
      Integer uncachedInt = new Integer(1000);
      assertNotSame(uncachedInt, +uncachedInt);
      assertEquals(uncachedInt, box(+uncachedInt));
      // TODO(b/420648962): Missing erasure type-safety casts on J2KT.
      assertThrowsClassCastException(
          () -> box("" + +Main.<Integer>uncheckedCast(true)), Integer.class);
    }
  }

  private static Object box(Object o) {
    return o;
  }

  @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
  private static <T> T uncheckedCast(Object o) {
    return (T) o;
  }
}
