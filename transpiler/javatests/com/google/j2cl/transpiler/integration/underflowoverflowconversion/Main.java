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
package com.google.j2cl.transpiler.integration.underflowoverflowconversion;

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
    assert overflowValue == expectValue;
  }

  private void takesDouble(double overflowValue, double expectValue) {
    assert overflowValue == expectValue;
  }

  private void takesLong(long overflowValue, long expectValue) {
    assert overflowValue == expectValue;
  }

  private void testArraysAssignment() {
    // All array contexts are implicitly int typed.

    int[] ints = new int[21 / 2];
    assert ints.length == 10;
    ints[11 / 2] = 1;
    assert ints[5] == 1;
    ints = new int[] {1 / 2, 3 / 2, 5 / 2};
    assert ints[0] == 0;
    assert ints[1] == 1;
    assert ints[2] == 2;
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
      assert ri == 4294967294d; // we don't honor int overflow for some operations

      ri = mi / mi;
      assert ri == 1;

      ri = mi % mi;
      assert ri == 0;

      ri = mi - mi;
      assert ri == 0;

      ri = mi << mi;
      assert ri == -2147483648;

      ri = mi >> mi;
      assert ri == 0;

      ri = mi >>> mi;
      assert ri == 0;

      ri = mi ^ mi;
      assert ri == 0;

      ri = mi & mi;
      assert ri == 2147483647;

      ri = mi | mi;
      assert ri == 2147483647;

      // Int division is handled, make sure that division doesn't leave any decimals behind.
      ri = 1 / 2;
      assert ri == 0;
    }

    // Long
    {
      rl = ml + ml;
      assert rl == -2L; // Works since the long emulation class supports it..

      rl = ml / ml;
      assert rl == 1L;

      rl = ml % ml;
      assert rl == 0L;

      rl = ml - ml;
      assert rl == 0L;

      rl = ml << ml;
      assert rl == -9223372036854775808L;

      rl = ml >> ml;
      assert rl == 0L;

      rl = ml >>> ml;
      assert rl == 0L;

      rl = ml ^ ml;
      assert rl == 0L;

      rl = ml & ml;
      assert rl == 9223372036854775807L;

      rl = ml | ml;
      assert rl == 9223372036854775807L;

      // Long division is handled, make sure that division doesn't leave any decimals behind.
      rl = 1L / 2L;
      assert rl == 0L;
    }
  }

  private void testCompoundAssignment() {
    // Only bothering to test byte because this test is not about proving what is done with a type,
    // it's about proving that compound assignment rewriting doesn't interfere with underflow
    // insertion.

    rb = mb;
    get().rb++;
    assert rb == -128;

    rb = mb;
    rb++;
    assert rb == -128;

    rb = mb;
    ++get().rb;
    assert rb == -128;

    rb = mb;
    ++rb;
    assert rb == -128;

    rb = mb;
    get().rb += 1;
    assert rb == -128;

    rb = mb;
    rb += 1;
    assert rb == -128;
  }

  private void testInlineEquality() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    int ei = 1;
    long el = 2L;

    assert 3 / 2 == ei;
    assert 5L / 2L == el;

    assert 1 / 2 != ei;
    assert 3L / 2L != el;

    assert 3 / 2 <= ei;
    assert 5L / 2L <= el;

    assert 3 / 2 >= ei;
    assert 5L / 2L >= el;

    assert 4 / 2 > ei;
    assert 6L / 2L > el;

    assert 1 / 2 < ei;
    assert 3L / 2L < el;
  }

  private void testMethodInvocation() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    takesInt(11 / 2, 5);
    takesDouble(mi + 1, 2147483648d); // we don't honor int overflow for some operations
    takesLong(ml + 1L, -9223372036854775808L);
  }

  private void testPostfixOperations() {
    // Does test all types because (unlike binary expressions) postfix operations do not implicitly
    // widen to int during execution. They actually start, execute, and finish all while staying
    // natively in the original type.

    rb = mb;
    rb++;
    assert rb == -128;

    rc = mc;
    rc++;
    assert rc == 0;

    rs = ms;
    rs++;
    assert rs == -32768;

    ri = mi;
    ri++;
    assert ri == 2147483648d; // we don't honor int overflow for some operations

    rl = ml;
    rl++;
    assert rl == -9223372036854775808L;

    rf = mf;
    rf++;
    assert rf == 3.4028235E38f;

    rd = md;
    rd++;
    assert rd == 1.7976931348623157E308D;
  }

  private void testPrefixOperations() {
    // Does test all types because (unlike binary expressions) postfix operations do not implicitly
    // widen to int during execution. They actually start, execute, and finish all while staying
    // natively in the original type.

    rb = mb;
    ++rb;
    assert rb == -128;

    rc = mc;
    ++rc;
    assert rc == 0;

    rs = ms;
    ++rs;
    assert rs == -32768;

    ri = mi;
    ++ri;
    assert ri == 2147483648d; // we don't honor int overflow for some operations

    rl = ml;
    ++rl;
    assert rl == -9223372036854775808L;

    rf = mf;
    ++rf;
    assert rf == 3.4028235E38f;

    rd = md;
    ++rd;
    assert rd == 1.7976931348623157E308D;
  }

  private void testReturnAssignment() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    assert returnInt() == 1;
    assert returnLong() == 2;
  }

  private void testVarInitializerAssignment() {
    // Shouldn't check byte/char/short because they will inevitably involve widening and narrowing
    // to and from int and so are handled in narrowing and widening primitive conversion, not in
    // underflow conversion.

    // Don't need to check float and double since they're just native double operations with native
    // underflow/overflow.

    int ri = 1 / 2;
    assert ri == 0;
    long rl = 1L / 2L;
    assert rl == 0L;
  }
}
