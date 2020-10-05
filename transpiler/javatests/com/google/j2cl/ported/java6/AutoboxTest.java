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
package com.google.j2cl.ported.java6;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests Autoboxing. */
@RunWith(JUnit4.class)
@SuppressWarnings("BoxedPrimitiveConstructor")
public class AutoboxTest {

  private Boolean boxedBoolean = Boolean.TRUE;

  private Byte boxedByte = new Byte((byte) 0xAB);

  private Character boxedChar = new Character('c');

  private Double boxedDouble = new Double(1.2323);

  private Float boxedFloat = new Float(1.2F);

  private Integer boxedInt = new Integer(20000000);

  private Long boxedLong = new Long(1231231231231L);

  private Short boxedShort = new Short((short) 6550);

  private boolean unboxedBoolean = true;

  private byte unboxedByte = (byte) 0xAB;

  private char unboxedChar = 'c';

  private double unboxedDouble = 1.2323;

  private float unboxedFloat = 1.2F;

  private int unboxedInt = 20000000;

  private long unboxedLong = 1231231231231L;

  private short unboxedShort = 6550;

  @Test
  public void testBoxing() {
    Boolean boolean_ = unboxedBoolean;
    assertThat(boolean_.booleanValue() == unboxedBoolean).isTrue();
    Byte byte_ = unboxedByte;
    assertThat(byte_.byteValue() == unboxedByte).isTrue();
    Character char_ = unboxedChar;
    assertThat(char_.charValue() == unboxedChar).isTrue();
    Short short_ = unboxedShort;
    assertThat(short_.shortValue() == unboxedShort).isTrue();
    Integer int_ = unboxedInt;
    assertThat(int_.intValue() == unboxedInt).isTrue();
    Long long_ = unboxedLong;
    assertThat(long_.longValue() == unboxedLong).isTrue();
    Float float_ = unboxedFloat;
    assertThat(float_.floatValue() == unboxedFloat).isTrue();
    Double double_ = unboxedDouble;
    assertThat(double_.doubleValue() == unboxedDouble).isTrue();

    // test boxing of return values
    assertThat(box(unboxedBoolean).booleanValue() == unboxedBoolean).isTrue();
    assertThat(box(unboxedByte).byteValue() == unboxedByte).isTrue();
    assertThat(box(unboxedShort).shortValue() == unboxedShort).isTrue();
    assertThat(box(unboxedChar).charValue() == unboxedChar).isTrue();
    assertThat(box(unboxedInt).intValue() == unboxedInt).isTrue();
    assertThat(box(unboxedLong).longValue() == unboxedLong).isTrue();
    assertThat(box(unboxedFloat).floatValue() == unboxedFloat).isTrue();
    assertThat(box(unboxedDouble).doubleValue() == unboxedDouble).isTrue();

    // test boxing of parameters
    assertThat(unbox(unboxedBoolean) == unboxedBoolean).isTrue();
    assertThat(unbox(unboxedByte) == unboxedByte).isTrue();
    assertThat(unbox(unboxedShort) == unboxedShort).isTrue();
    assertThat(unbox(unboxedChar) == unboxedChar).isTrue();
    assertThat(unbox(unboxedInt) == unboxedInt).isTrue();
    assertThat(unbox(unboxedLong) == unboxedLong).isTrue();
    assertThat(unbox(unboxedFloat) == unboxedFloat).isTrue();
    assertThat(unbox(unboxedDouble) == unboxedDouble).isTrue();
  }

  /**
   * JLS 5.2 has a special case for assignment of a constant to a variable of type Byte, Short, or
   * Character. Such an assignment is allowed so long as the constant fits within the type's range.
   * In such cases, the box type can be different from the type of the constant.
   */
  @Test
  public void testBoxingDifferentType() {
    Character c = 1;
    assertThat((Object) c).isEqualTo(Character.valueOf((char) 1));

    Byte b = 2;
    assertThat((Object) b).isEqualTo(Byte.valueOf((byte) 2));

    Short s = 3;
    assertThat((Object) s).isEqualTo(Short.valueOf((short) 3));
  }

  /** Verify that .valueOf() methods return identical references for types within certain ranges. */
  @Test
  public void testCaching() {
    assertThat((byte) 3).isSameInstanceAs((byte) 3);
    assertThat('A').isSameInstanceAs('A');
    assertThat((short) 120).isSameInstanceAs((short) 120);
    assertThat(-13).isSameInstanceAs(-13);
    assertThat(7L).isSameInstanceAs(7L);
  }

  /**
   * Test ++, --, and compound assignments like += when the left-hand side is a boxed Integer. Use
   * assertNotSame to ensure that a new Integer is created instead of modifying the original integer
   * in place. (Issue 2446).
   */
  @Test
  public void testCompoundAssignmentsWithInteger() {
    {
      Integer operand;
      Integer original;
      Integer result;

      original = operand = 0;
      result = operand++;
      // operand must be different object now.
      assertWithMessage("[o++] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[o++] original == result, ").that(result).isSameInstanceAs(original);
      assertWithMessage("[o++] result != operand, ").that(operand).isNotSameInstanceAs(result);
      // checks against boxedvalues cached object.
      assertWithMessage("[o++] valueOf(n) == operand, ").that(operand).isSameInstanceAs(1);
      // checks cached object's value.
      assertWithMessage("[o++] n == operand.value, ").that(operand.intValue()).isEqualTo(1);
    }

    {
      Integer operand;
      Integer original;
      Integer result;
      original = operand = 2;
      result = ++operand;
      assertWithMessage("[++o] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[++o] original != result, ").that(result).isNotSameInstanceAs(original);
      assertWithMessage("[++o] result == operand, ").that(operand).isSameInstanceAs(result);
      assertWithMessage("[++o] valueOf(n) == operand, ").that(operand).isSameInstanceAs(3);
      assertWithMessage("[++o] n == operand.value, ").that(operand.intValue()).isEqualTo(3);
    }

    {
      Integer operand;
      Integer original;
      Integer result;
      original = operand = 5;
      result = operand--;
      assertWithMessage("[o--] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[o--] original == result, ").that(result).isSameInstanceAs(original);
      assertWithMessage("[o--] result != operand, ").that(operand).isNotSameInstanceAs(result);
      assertWithMessage("[o--] valueOf(n) == operand, ").that(operand).isSameInstanceAs(4);
      assertWithMessage("[o--] n == operand.value, ").that(operand.intValue()).isEqualTo(4);
    }

    {
      Integer operand;
      Integer original;
      Integer result;
      original = operand = 7;
      result = --operand;
      assertWithMessage("[--o] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[--o] original != result, ").that(result).isNotSameInstanceAs(original);
      assertWithMessage("[--o] result == operand, ").that(operand).isSameInstanceAs(result);
      assertWithMessage("[--o] valueOf(n) == operand, ").that(operand).isSameInstanceAs(6);
      assertWithMessage("[--o] n == operand.value, ").that(operand.intValue()).isEqualTo(6);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 8;
      operand += 2;
      assertWithMessage("[+=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[+=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(10);
      assertWithMessage("[+=] n == operand.value, ").that(operand.intValue()).isEqualTo(10);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 11;
      operand -= 2;
      assertWithMessage("[-=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[-=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(9);
      assertWithMessage("[-=] n == operand.value, ").that(operand.intValue()).isEqualTo(9);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 21;
      operand *= 2;
      assertWithMessage("[*=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[*=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(42);
      assertWithMessage("[*=] n == operand.value, ").that(operand.intValue()).isEqualTo(42);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 30;
      operand /= 2;
      assertWithMessage("[/=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[/=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(15);
      assertWithMessage("[/=] n == operand.value, ").that(operand.intValue()).isEqualTo(15);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 123;
      operand %= 100;
      assertWithMessage("[%=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[%=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(23);
      assertWithMessage("[%=] n == operand.value, ").that(operand.intValue()).isEqualTo(23);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 0x55;
      operand &= 0xF;
      assertWithMessage("[&=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[&=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(0x5);
      assertWithMessage("[&=] n == operand.value, ").that(operand.intValue()).isEqualTo(0x5);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 0x55;
      operand |= 0xF;
      assertWithMessage("[|=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[|=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(0x5F);
      assertWithMessage("[|=] n == operand.value, ").that(operand.intValue()).isEqualTo(0x5F);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 0x55;
      operand ^= 0xF;
      assertWithMessage("[&=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[&=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(0x5A);
      assertWithMessage("[&=] n == operand.value, ").that(operand.intValue()).isEqualTo(0x5A);
    }

    {
      Integer operand;
      Integer original;
      original = operand = 0x3F;
      operand <<= 1;
      assertWithMessage("[<<=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[<<=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(0x7E);
      assertWithMessage("[<<=] n == operand.value, ").that(operand.intValue()).isEqualTo(0x7E);
    }

    {
      Integer operand;
      Integer original;
      original = operand = -16;
      operand >>= 1;
      assertWithMessage("[>>=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[>>=] valueOf(n) == operand, ").that(operand).isSameInstanceAs(-8);
      assertWithMessage("[>>=] n == operand.value, ").that(operand.intValue()).isEqualTo(-8);
    }

    {
      Integer operand;
      Integer original;
      original = operand = -1;
      operand >>>= 1;
      assertWithMessage("[>>>=] original != operand, ").that(operand).isNotSameInstanceAs(original);
      assertWithMessage("[>>>=] valueOf(n).equals(operand), ")
          .that(operand)
          .isEqualTo(Integer.valueOf(0x7FFFFFFF));
      assertWithMessage("[>>>=] n == operand.value, ")
          .that(operand.intValue())
          .isEqualTo(0x7FFFFFFF);
    }
  }

  /** Tests operations like += and *= where the left-hand side is a boxed Long. */
  @Test
  public void testCompoundAssignmentsWithLong() {
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 += 5;
      assertThat(long1).isEqualTo(10L);
      assertThat(long2).isEqualTo(15L);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 += 1;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(11);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 -= 1;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(9);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 *= 2;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(20);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 /= 2;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(5);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 %= 3;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(1);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 <<= 1;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(20);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 >>= 1;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(5);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 >>>= 1;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(5);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 &= 8;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(8);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 |= 1;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(11);
    }
    {
      Long long1 = 10L;
      Long long2 = long1;
      long2 ^= 1;
      assertThat(long1).isEqualTo(10);
      assertThat(long2).isEqualTo(11);
    }
  }

  /**
   * Tests ++ and -- on all boxed types. Use assertNotSame to ensure that a new wrapper is created
   * instead of modifying the original boxed value in place. (Issue 2446).
   */
  @Test
  public void testIncrDecr() {
    {
      // these initial tests are miscellaneous one-off tests
      Byte originalBoxedByte = boxedByte;

      Object value2 = (byte) boxedByte++;
      assertThat(value2).isEqualTo(unboxedByte);
      assertThat((byte) boxedByte).isEqualTo(unboxedByte + 1);
      boxedByte = originalBoxedByte;

      Integer[] ary = new Integer[] {0, 10, 20, 30, 40, 50};
      Integer idx = 2;
      Object value1 = (int) ary[idx++]++;
      assertThat(value1).isEqualTo(20);
      assertThat((int) ary[2]).isEqualTo(21);
      assertThat((int) idx).isEqualTo(3);
      Object value = (int) ary[idx += 1];
      assertThat(value).isEqualTo(40);
      assertThat((int) idx).isEqualTo(4);
    }
    // the rest of this method tests all boxed types under ++ and --
    {
      Byte originalBoxedByte = boxedByte;
      boxedByte++;
      assertWithMessage("Boxed byte modified in place")
          .that(originalBoxedByte)
          .isNotSameInstanceAs(boxedByte);
      assertThat((byte) boxedByte).isEqualTo(unboxedByte + 1);
      boxedByte = originalBoxedByte;
      ++boxedByte;
      assertWithMessage("Boxed byte modified in place")
          .that(originalBoxedByte)
          .isNotSameInstanceAs(boxedByte);
      assertThat((byte) boxedByte).isEqualTo(unboxedByte + 1);
      boxedByte = originalBoxedByte;
      boxedByte--;
      assertWithMessage("Boxed byte modified in place")
          .that(originalBoxedByte)
          .isNotSameInstanceAs(boxedByte);
      assertThat((byte) boxedByte).isEqualTo(unboxedByte - 1);
      boxedByte = originalBoxedByte;
      --boxedByte;
      assertWithMessage("Boxed byte modified in place")
          .that(originalBoxedByte)
          .isNotSameInstanceAs(boxedByte);
      assertThat((byte) boxedByte).isEqualTo(unboxedByte - 1);
      boxedByte = originalBoxedByte;
    }
    {
      Character originalBoxedChar = boxedChar;
      boxedChar++;
      assertWithMessage("Boxed character modified in place")
          .that(originalBoxedChar)
          .isNotSameInstanceAs(boxedChar);
      assertThat((char) boxedChar).isEqualTo(unboxedChar + 1);
      boxedChar = originalBoxedChar;
      ++boxedChar;
      assertWithMessage("Boxed character modified in place")
          .that(originalBoxedChar)
          .isNotSameInstanceAs(boxedChar);
      assertThat((char) boxedChar).isEqualTo(unboxedChar + 1);
      boxedChar = originalBoxedChar;
      boxedChar--;
      assertWithMessage("Boxed character modified in place")
          .that(originalBoxedChar)
          .isNotSameInstanceAs(boxedChar);
      assertThat((char) boxedChar).isEqualTo(unboxedChar - 1);
      boxedChar = originalBoxedChar;
      --boxedChar;
      assertWithMessage("Boxed character modified in place")
          .that(originalBoxedChar)
          .isNotSameInstanceAs(boxedChar);
      assertThat((char) boxedChar).isEqualTo(unboxedChar - 1);
      boxedChar = originalBoxedChar;
    }
    {
      Short originalBoxedShort = boxedShort;
      boxedShort++;
      assertWithMessage("Boxed short modified in place")
          .that(originalBoxedShort)
          .isNotSameInstanceAs(boxedShort);
      assertThat((short) boxedShort).isEqualTo(unboxedShort + 1);
      boxedShort = originalBoxedShort;
      ++boxedShort;
      assertWithMessage("Boxed short modified in place")
          .that(originalBoxedShort)
          .isNotSameInstanceAs(boxedShort);
      assertThat((short) boxedShort).isEqualTo(unboxedShort + 1);
      boxedShort = originalBoxedShort;
      boxedShort--;
      assertWithMessage("Boxed short modified in place")
          .that(originalBoxedShort)
          .isNotSameInstanceAs(boxedShort);
      assertThat((short) boxedShort).isEqualTo(unboxedShort - 1);
      boxedShort = originalBoxedShort;
      --boxedShort;
      assertWithMessage("Boxed short modified in place")
          .that(originalBoxedShort)
          .isNotSameInstanceAs(boxedShort);
      assertThat((short) boxedShort).isEqualTo(unboxedShort - 1);
      boxedShort = originalBoxedShort;
    }
    {
      Integer originalBoxedInt = boxedInt;
      boxedInt++;
      assertWithMessage("Boxed int modified in place")
          .that(originalBoxedInt)
          .isNotSameInstanceAs(boxedInt);
      assertThat((int) boxedInt).isEqualTo(unboxedInt + 1);
      boxedInt = originalBoxedInt;
      ++boxedInt;
      assertWithMessage("Boxed int modified in place")
          .that(originalBoxedInt)
          .isNotSameInstanceAs(boxedInt);
      assertThat((int) boxedInt).isEqualTo(unboxedInt + 1);
      boxedInt = originalBoxedInt;
      boxedInt--;
      assertWithMessage("Boxed int modified in place")
          .that(originalBoxedInt)
          .isNotSameInstanceAs(boxedInt);
      assertThat((int) boxedInt).isEqualTo(unboxedInt - 1);
      boxedInt = originalBoxedInt;
      --boxedInt;
      assertWithMessage("Boxed int modified in place")
          .that(originalBoxedInt)
          .isNotSameInstanceAs(boxedInt);
      assertThat((int) boxedInt).isEqualTo(unboxedInt - 1);
      boxedInt = originalBoxedInt;
    }
    {
      Long originalBoxedLong = boxedLong;
      boxedLong++;
      assertWithMessage("Boxed long modified in place")
          .that(originalBoxedLong)
          .isNotSameInstanceAs(boxedLong);
      assertThat((long) boxedLong).isEqualTo(unboxedLong + 1);
      boxedLong = originalBoxedLong;
      ++boxedLong;
      assertWithMessage("Boxed long modified in place")
          .that(originalBoxedLong)
          .isNotSameInstanceAs(boxedLong);
      assertThat((long) boxedLong).isEqualTo(unboxedLong + 1);
      boxedLong = originalBoxedLong;
      boxedLong--;
      assertWithMessage("Boxed long modified in place")
          .that(originalBoxedLong)
          .isNotSameInstanceAs(boxedLong);
      assertThat((long) boxedLong).isEqualTo(unboxedLong - 1);
      boxedLong = originalBoxedLong;
      --boxedLong;
      assertWithMessage("Boxed long modified in place")
          .that(originalBoxedLong)
          .isNotSameInstanceAs(boxedLong);
      assertThat((long) boxedLong).isEqualTo(unboxedLong - 1);
      boxedLong = originalBoxedLong;
    }
    {
      Float originalBoxedFloat = boxedFloat;
      boxedFloat++;
      assertWithMessage("Boxed float modified in place")
          .that(originalBoxedFloat)
          .isNotSameInstanceAs(boxedFloat);
      assertThat((float) boxedFloat).isEqualTo(unboxedFloat + 1);
      boxedFloat = originalBoxedFloat;
      ++boxedFloat;
      assertWithMessage("Boxed float modified in place")
          .that(originalBoxedFloat)
          .isNotSameInstanceAs(boxedFloat);
      assertThat((float) boxedFloat).isEqualTo(unboxedFloat + 1);
      boxedFloat = originalBoxedFloat;
      boxedFloat--;
      assertWithMessage("Boxed float modified in place")
          .that(originalBoxedFloat)
          .isNotSameInstanceAs(boxedFloat);
      assertThat((float) boxedFloat).isEqualTo(unboxedFloat - 1);
      boxedFloat = originalBoxedFloat;
      --boxedFloat;
      assertWithMessage("Boxed float modified in place")
          .that(originalBoxedFloat)
          .isNotSameInstanceAs(boxedFloat);
      assertThat((float) boxedFloat).isEqualTo(unboxedFloat - 1);
      boxedFloat = originalBoxedFloat;
    }
    {
      Double originalBoxedDouble = boxedDouble;
      boxedDouble++;
      assertWithMessage("Boxed double modified in place")
          .that(originalBoxedDouble)
          .isNotSameInstanceAs(boxedDouble);
      assertThat((double) boxedDouble).isEqualTo(unboxedDouble + 1);
      boxedDouble = originalBoxedDouble;
      ++boxedDouble;
      assertWithMessage("Boxed double modified in place")
          .that(originalBoxedDouble)
          .isNotSameInstanceAs(boxedDouble);
      assertThat((double) boxedDouble).isEqualTo(unboxedDouble + 1);
      boxedDouble = originalBoxedDouble;
      boxedDouble--;
      assertWithMessage("Boxed double modified in place")
          .that(originalBoxedDouble)
          .isNotSameInstanceAs(boxedDouble);
      assertThat((double) boxedDouble).isEqualTo(unboxedDouble - 1);
      boxedDouble = originalBoxedDouble;
      --boxedDouble;
      assertWithMessage("Boxed double modified in place")
          .that(originalBoxedDouble)
          .isNotSameInstanceAs(boxedDouble);
      assertThat((double) boxedDouble).isEqualTo(unboxedDouble - 1);
      boxedDouble = originalBoxedDouble;
    }
  }

  @Test
  public void testUnboxing() {
    boolean boolean_ = boxedBoolean;
    assertThat(boolean_ == boxedBoolean.booleanValue()).isTrue();
    byte byte_ = boxedByte;
    assertThat(byte_ == boxedByte.byteValue()).isTrue();
    char char_ = boxedChar;
    assertThat(char_ == boxedChar.charValue()).isTrue();
    short short_ = boxedShort;
    assertThat(short_ == boxedShort.shortValue()).isTrue();
    int int_ = boxedInt;
    assertThat(int_ == boxedInt.intValue()).isTrue();
    long long_ = boxedLong;
    assertThat(long_ == boxedLong.longValue()).isTrue();
    float float_ = boxedFloat;
    assertThat(float_ == boxedFloat.floatValue()).isTrue();
    double double_ = boxedDouble;
    assertThat(double_ == boxedDouble.doubleValue()).isTrue();

    // test unboxing of return values
    assertThat(unbox(boxedBoolean) == unboxedBoolean).isTrue();
    assertThat(unbox(boxedByte) == unboxedByte).isTrue();
    assertThat(unbox(boxedShort) == unboxedShort).isTrue();
    assertThat(unbox(boxedChar) == unboxedChar).isTrue();
    assertThat(unbox(boxedInt) == unboxedInt).isTrue();
    assertThat(unbox(boxedLong) == unboxedLong).isTrue();
    assertThat(unbox(boxedFloat) == unboxedFloat).isTrue();
    assertThat(unbox(boxedDouble) == unboxedDouble).isTrue();

    // test unboxing of parameters
    assertThat(box(boxedBoolean).booleanValue() == unboxedBoolean).isTrue();
    assertThat(box(boxedByte).byteValue() == unboxedByte).isTrue();
    assertThat(box(boxedShort).shortValue() == unboxedShort).isTrue();
    assertThat(box(boxedChar).charValue() == unboxedChar).isTrue();
    assertThat(box(boxedInt).intValue() == unboxedInt).isTrue();
    assertThat(box(boxedLong).longValue() == unboxedLong).isTrue();
    assertThat(box(boxedFloat).floatValue() == unboxedFloat).isTrue();
    assertThat(box(boxedDouble).doubleValue() == unboxedDouble).isTrue();
  }

  @Test
  public void testUnboxingDifferentType() {
    {
      short short_ = boxedByte;
      assertThat(short_ == boxedByte.byteValue()).isTrue();
      int int_ = boxedByte;
      assertThat(int_ == boxedByte.byteValue()).isTrue();
      long long_ = boxedByte;
      assertThat(long_ == boxedByte.byteValue()).isTrue();
      float float_ = boxedByte;
      assertThat(float_ == boxedByte.byteValue()).isTrue();
      double double_ = boxedByte;
      assertThat(double_ == boxedByte.byteValue()).isTrue();
    }

    {
      int int_ = boxedShort;
      assertThat(int_ == boxedShort.shortValue()).isTrue();
      long long_ = boxedShort;
      assertThat(long_ == boxedShort.shortValue()).isTrue();
      float float_ = boxedShort;
      assertThat(float_ == boxedShort.shortValue()).isTrue();
      double double_ = boxedShort;
      assertThat(double_ == boxedShort.shortValue()).isTrue();
    }

    {
      int int_ = boxedChar;
      assertThat(int_ == boxedChar.charValue()).isTrue();
      long long_ = boxedChar;
      assertThat(long_ == boxedChar.charValue()).isTrue();
      float float_ = boxedChar;
      assertThat(float_ == boxedChar.charValue()).isTrue();
      double double_ = boxedChar;
      assertThat(double_ == boxedChar.charValue()).isTrue();
    }

    {
      long long_ = boxedInt;
      assertThat(long_ == boxedInt.intValue()).isTrue();
      float float_ = boxedInt;
      assertThat(float_ == boxedInt.intValue()).isTrue();
      double double_ = boxedInt;
      assertThat(double_ == boxedInt.intValue()).isTrue();
    }
  }

  private static Boolean box(boolean b) {
    return b;
  }

  private static Byte box(byte b) {
    return b;
  }

  private static Character box(char c) {
    return c;
  }

  private static Double box(double d) {
    return d;
  }

  private static Float box(float f) {
    return f;
  }

  private static Integer box(int i) {
    return i;
  }

  private static Long box(long l) {
    return l;
  }

  private static Short box(short s) {
    return s;
  }

  private static boolean unbox(Boolean b) {
    return b;
  }

  private static byte unbox(Byte b) {
    return b;
  }

  private static char unbox(Character c) {
    return c;
  }

  private static double unbox(Double d) {
    return d;
  }

  private static float unbox(Float f) {
    return f;
  }

  private static int unbox(Integer i) {
    return i;
  }

  private static long unbox(Long l) {
    return l;
  }

  private static short unbox(Short s) {
    return s;
  }
}
