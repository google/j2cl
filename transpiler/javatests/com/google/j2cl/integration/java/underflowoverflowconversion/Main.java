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
package underflowoverflowconversion;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

@SuppressWarnings("IdentityBinaryExpression")
public class Main {

  public static void main(String... args) {
    Main m = new Main();
    m.testPostfixOperations();
    m.testPrefixOperations();
    m.testBinaryOperations();
    m.testCompoundAssignment();
    m.testVarInitializerAssignment();
    m.testArraysAssignment();
    m.testMethodInvocation();
    m.testReturnAssignment();
    m.testInlineEquality();
    testByteOverflow();
    testNestedOperationsOverflow();
    testDivisionMultiplicationOverflow();
  }

  private byte mb = 127; // Byte.MAX_VALUE
  private char mc = 65535; // Character.MAX_VALUE;
  private short ms = 32767; // Short.MAX_VALUE;
  private int mi = 2147483647; // Integer.MAX_VALUE;
  private long ml = 9223372036854775807L; // Long.MAX_VALUE;
  private float mf = 3.4028235E38f; // Float.MAX_VALUE;
  private double md = 1.7976931348623157E308; // Double.MAX_VALUE;
  private byte rb;
  private char rc;
  private short rs;
  private int ri;
  private long rl;
  private float rf;
  private double rd;

  private Main get() {
    return this;
  }

  private int returnInt() {
    return 3 / 2;
  }

  private long returnLong() {
    return 5L / 2L;
  }

  private void takesInt(int overflowValue, int expectValue) {
    assertTrue(overflowValue == expectValue);
  }

  private void takesDouble(double overflowValue, double expectValue) {
    assertTrue(overflowValue == expectValue);
  }

  private void takesLong(long overflowValue, long expectValue) {
    assertTrue(overflowValue == expectValue);
  }

  private void testArraysAssignment() {
    // All array contexts are implicitly int typed.

    int[] ints = new int[21 / 2];
    assertTrue(ints.length == 10);
    ints[11 / 2] = 1;
    assertTrue(ints[5] == 1);
    ints = new int[] {1 / 2, 3 / 2, 5 / 2};
    assertTrue(ints[0] == 0);
    assertTrue(ints[1] == 1);
    assertTrue(ints[2] == 2);
  }

  private void testBinaryOperations() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    // Int
    {
      ri = mi + mi;
      assertTrue(ri == -2);

      ri = mi / mi;
      assertTrue(ri == 1);

      ri = mi % mi;
      assertTrue(ri == 0);

      ri = mi - mi;
      assertTrue(ri == 0);

      ri = mi << mi;
      assertTrue(ri == -2147483648);

      ri = mi >> mi;
      assertTrue(ri == 0);

      ri = mi >>> mi;
      assertTrue(ri == 0);

      ri = mi ^ mi;
      assertTrue(ri == 0);

      ri = mi & mi;
      assertTrue(ri == 2147483647);

      ri = mi | mi;
      assertTrue(ri == 2147483647);

      // Int division is handled, make sure that division doesn't leave any decimals behind.
      ri = 1 / 2;
      assertTrue(ri == 0);
    }

    // Long
    {
      rl = ml + ml;
      assertTrue(rl == -2L); // Works since the long emulation class supports it..

      rl = ml / ml;
      assertTrue(rl == 1L);

      rl = ml % ml;
      assertTrue(rl == 0L);

      rl = ml - ml;
      assertTrue(rl == 0L);

      rl = ml << ml;
      assertTrue(rl == -9223372036854775808L);

      rl = ml >> ml;
      assertTrue(rl == 0L);

      rl = ml >>> ml;
      assertTrue(rl == 0L);

      rl = ml ^ ml;
      assertTrue(rl == 0L);

      rl = ml & ml;
      assertTrue(rl == 9223372036854775807L);

      rl = ml | ml;
      assertTrue(rl == 9223372036854775807L);

      // Long division is handled, make sure that division doesn't leave any decimals behind.
      rl = 1L / 2L;
      assertTrue(rl == 0L);
    }
  }

  @SuppressWarnings("NarrowingCompoundAssignment")
  private void testCompoundAssignment() {
    // Only bothering to test byte because this test is not about proving what is done with a type,
    // it's about proving that compound assignment rewriting doesn't interfere with underflow
    // insertion.

    rb = mb;
    get().rb++;
    assertTrue(rb == -128);

    rb = mb;
    rb++;
    assertTrue(rb == -128);

    rb = mb;
    ++get().rb;
    assertTrue(rb == -128);

    rb = mb;
    ++rb;
    assertTrue(rb == -128);

    rb = mb;
    get().rb += 1;
    assertTrue(rb == -128);

    rb = mb;
    rb += 1;
    assertTrue(rb == -128);
  }

  private static void testByteOverflow() {
    // Perform Byte.MAX_VALUE / (Byte.MAX_VALUE + Byte.MAX_VALUE) all in byte precision first ...
    byte m = Byte.MAX_VALUE;
    assertEquals((byte) -63, (byte) (m / (byte) (m + m)));

    // and note that it is different than writing it as a single expression because it will be
    // performed in integer precision an no overflow occurs.
    assertEquals((byte) 0, (byte) (m / (m + m)));
  }

  @SuppressWarnings("BadShiftAmount")
  private static void testNestedOperationsOverflow() {
    int intermediate;
    // Tests that the minimal coercions inserted by J2CL are sufficient.
    assertEquals(-1, (-1 >>> 32) >>> 32);
    intermediate = (-1 >>> 32);
    assertEquals(-1, intermediate >>> 32);

    assertEquals(-3, (-1 >>> 32) + (-1 >>> 32) + (-1 >>> 32));
    intermediate = (-1 >>> 32) + (-1 >>> 32);
    assertEquals(-3, intermediate + (-1 >>> 32));

    assertEquals(-2, (Integer.MAX_VALUE + Integer.MAX_VALUE) >>> 32);
    intermediate = Integer.MAX_VALUE + Integer.MAX_VALUE;
    assertEquals(-2, intermediate >>> 32);
  }

  private void testInlineEquality() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    int ei = 1;
    long el = 2L;

    assertTrue(3 / 2 == ei);
    assertTrue(5L / 2L == el);

    assertTrue(1 / 2 != ei);
    assertTrue(3L / 2L != el);

    assertTrue(3 / 2 <= ei);
    assertTrue(5L / 2L <= el);

    assertTrue(3 / 2 >= ei);
    assertTrue(5L / 2L >= el);

    assertTrue(4 / 2 > ei);
    assertTrue(6L / 2L > el);

    assertTrue(1 / 2 < ei);
    assertTrue(3L / 2L < el);
  }

  private void testMethodInvocation() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    takesInt(11 / 2, 5);
    takesDouble(mi + 1, Integer.MIN_VALUE); // we don't honor int overflow for some operations
    takesLong(ml + 1L, -9223372036854775808L);
  }

  private void testPostfixOperations() {
    // Does test all types because (unlike binary expressions) postfix operations do not implicitly
    // widen to int during execution. They actually start, execute, and finish all while staying
    // natively in the original type.

    rb = mb;
    rb++;
    assertTrue(rb == -128);

    rc = mc;
    rc++;
    assertTrue(rc == 0);

    rs = ms;
    rs++;
    assertTrue(rs == -32768);

    ri = mi;
    ri++;
    assertTrue(ri == -2147483648);

    rl = ml;
    rl++;
    assertTrue(rl == -9223372036854775808L);

    rf = mf;
    rf++;
    assertTrue(rf == 3.4028235E38f);

    rd = md;
    rd++;
    assertTrue(rd == 1.7976931348623157E308D);

    byte b = Byte.MIN_VALUE;
    assertTrue(b-- == Byte.MIN_VALUE);
    assertTrue(b == Byte.MAX_VALUE);

    b = Byte.MAX_VALUE;
    assertTrue(b++ == Byte.MAX_VALUE);
    assertTrue(b == Byte.MIN_VALUE);

    Byte boxedByte = Byte.MIN_VALUE;
    assertTrue(boxedByte-- == Byte.MIN_VALUE);
    assertTrue(boxedByte == Byte.MAX_VALUE);

    boxedByte = Byte.MAX_VALUE;
    assertTrue(boxedByte++ == Byte.MAX_VALUE);
    assertTrue(boxedByte == Byte.MIN_VALUE);
  }

  private void testPrefixOperations() {
    // Does test all types because (unlike binary expressions) postfix operations do not implicitly
    // widen to int during execution. They actually start, execute, and finish all while staying
    // natively in the original type.

    rb = mb;
    ++rb;
    assertTrue(rb == -128);

    rc = mc;
    ++rc;
    assertTrue(rc == 0);

    rs = ms;
    ++rs;
    assertTrue(rs == -32768);

    ri = mi;
    ++ri;
    assertTrue(ri == -2147483648);

    rl = ml;
    ++rl;
    assertTrue(rl == -9223372036854775808L);

    rf = mf;
    ++rf;
    assertTrue(rf == 3.4028235E38f);

    rd = md;
    ++rd;
    assertTrue(rd == 1.7976931348623157E308D);

    byte b = Byte.MIN_VALUE;
    assertTrue(--b == Byte.MAX_VALUE);

    b = Byte.MAX_VALUE;
    assertTrue(++b == Byte.MIN_VALUE);

    Byte boxedByte = Byte.MIN_VALUE;
    assertTrue(--boxedByte == Byte.MAX_VALUE);

    boxedByte = Byte.MAX_VALUE;
    assertTrue(++boxedByte == Byte.MIN_VALUE);
  }

  private void testReturnAssignment() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    assertTrue(returnInt() == 1);
    assertTrue(returnLong() == 2);
  }

  private void testVarInitializerAssignment() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    int ri = 1 / 2;
    assertTrue(ri == 0);
    long rl = 1L / 2L;
    assertTrue(rl == 0L);
  }

  private static void testDivisionMultiplicationOverflow() {
    byte minusOneB = -1;
    short minusOneS = -1;
    int minusOneI = -1;
    long minusOneL = -1L;

    assertTrue(Integer.MIN_VALUE * minusOneB == Integer.MIN_VALUE);
    assertTrue(Integer.MIN_VALUE / minusOneB == Integer.MIN_VALUE);
    assertTrue(Integer.MIN_VALUE * minusOneS == Integer.MIN_VALUE);
    assertTrue(Integer.MIN_VALUE / minusOneS == Integer.MIN_VALUE);
    assertTrue(Integer.MIN_VALUE * minusOneI == Integer.MIN_VALUE);
    assertTrue(Integer.MIN_VALUE / minusOneI == Integer.MIN_VALUE);
    assertTrue(Integer.MIN_VALUE * minusOneL == 2147483648L);
    assertTrue(Integer.MIN_VALUE / minusOneL == 2147483648L);

    assertTrue(Long.MIN_VALUE * minusOneB == Long.MIN_VALUE);
    assertTrue(Long.MIN_VALUE / minusOneB == Long.MIN_VALUE);
    assertTrue(Long.MIN_VALUE * minusOneS == Long.MIN_VALUE);
    assertTrue(Long.MIN_VALUE / minusOneS == Long.MIN_VALUE);
    assertTrue(Long.MIN_VALUE * minusOneI == Long.MIN_VALUE);
    assertTrue(Long.MIN_VALUE / minusOneI == Long.MIN_VALUE);
    assertTrue(Long.MIN_VALUE * minusOneL == Long.MIN_VALUE);
    assertTrue(Long.MIN_VALUE / minusOneL == Long.MIN_VALUE);
  }
}
