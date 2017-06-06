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
package com.google.j2cl.transpiler.integration.simpleautoboxing;

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsMethod;

@SuppressWarnings("BoxedPrimitiveConstructor")
public class Main {
  public Byte box(byte b) {
    return new Byte(b);
  }

  public Double box(double d) {
    return new Double(d);
  }

  public Float box(float f) {
    return new Float(f);
  }

  public Integer box(int i) {
    return new Integer(i);
  }

  public Long box(long l) {
    return new Long(l);
  }

  public Short box(short s) {
    return new Short(s);
  }

  public Boolean box(boolean b) {
    return new Boolean(b);
  }

  public Character box(char a) {
    return new Character(a);
  }

  public byte unbox(Byte b) {
    return b.byteValue();
  }

  public double unbox(Double d) {
    return d.doubleValue();
  }

  public float unbox(Float f) {
    return f.floatValue();
  }

  public int unbox(Integer i) {
    return i.intValue();
  }

  public long unbox(Long l) {
    return l.longValue();
  }

  public short unbox(Short s) {
    return s.shortValue();
  }

  public boolean unbox(Boolean b) {
    return b.booleanValue();
  }

  public char unbox(Character c) {
    return c.charValue();
  }

  public double takesAndReturnsPrimitiveDouble(double d) {
    return d;
  }

  public double takesObjectAndReturnsPrimitiveDouble(@DoNotAutobox Object o) {
    return (Double) o;
  }

  public double sumWithoutBoxing(@DoNotAutobox Object... numbers) {
    double sum = 0;
    for (Object number : numbers) {
      sum += (Double) number;
    }
    return sum;
  }

  @JsMethod
  public double sumWithoutBoxingJsVarargs(@DoNotAutobox Object... numbers) {
    double sum = 0;
    for (Object number : numbers) {
      sum += (Double) number;
    }
    return sum;
  }

  public void testBoxByParameter() {
    byte b = (byte) 100;
    double d = 1111.0;
    float f = 1111.0f;
    int i = 1111;
    long l = 1111L;
    short s = (short) 100;
    boolean bool = true;
    char c = 'a';
    assert (unbox(b) == b);
    assert (unbox(d) == d);
    assert (unbox(f) == f);
    assert (unbox(i) == i);
    assert (unbox(l) == l);
    assert (unbox(s) == s);
    assert (unbox(bool) == bool);
    assert (unbox(c) == c);
    assert (takesAndReturnsPrimitiveDouble(3) == 3);
    assert (sumWithoutBoxing(1, 1.5, (byte) 1, (short) 1, (float) 1) == 5.5);
    assert (sumWithoutBoxingJsVarargs(1, 1.5, (byte) 1, (short) 1, (float) 1) == 5.5);
  }

  public void testBoxByAssignment() {
    byte b = (byte) 100;
    double d = 1111.0;
    float f = 1111.0f;
    int i = 1111;
    long l = 1111L;
    short s = (short) 100;
    boolean bool = true;
    char c = 'a';
    Byte boxB = b;
    Double boxD = d;
    Float boxF = f;
    Integer boxI = i;
    Long boxL = l;
    Short boxS = s;
    Boolean boxBool = bool;
    Character boxC = c;
    assert (boxB.byteValue() == b);
    assert (boxD.doubleValue() == d);
    assert (boxF.floatValue() == f);
    assert (boxI.intValue() == i);
    assert (boxL.longValue() == l);
    assert (boxS.shortValue() == s);
    assert (boxBool.booleanValue() == bool);
    assert (boxC.charValue() == c);
  }

  public void testUnboxByParameter() {
    Byte boxB = new Byte((byte) 100);
    Double boxD = new Double(1111.0);
    Float boxF = new Float(1111.0f);
    Integer boxI = new Integer(1111);
    Long boxL = new Long(1111L);
    Short boxS = new Short((short) 100);
    Boolean boxBool = new Boolean(true);
    Character boxC = new Character('c');

    // Unbox
    assert (box(boxB).equals(boxB));
    assert (box(boxD).equals(boxD));
    assert (box(boxF).equals(boxF));
    assert (box(boxI).equals(boxI));
    assert (box(boxL).equals(boxL));
    assert (box(boxS).equals(boxS));
    assert (box(boxBool).equals(boxBool));
    assert (box(boxC).equals(boxC));

    // Unbox and widen
    assert takesAndReturnsPrimitiveDouble(boxB) == boxB.byteValue();
    assert takesAndReturnsPrimitiveDouble(boxC) == boxC.charValue();
    assert takesAndReturnsPrimitiveDouble(boxS) == boxS.shortValue();
    assert takesAndReturnsPrimitiveDouble(boxI) == boxI.intValue();
    assert takesAndReturnsPrimitiveDouble(boxL) == boxL.longValue();
    assert takesAndReturnsPrimitiveDouble(boxF) == boxF.floatValue();
  }

  public void testUnboxByAssignment() {
    Byte boxB = new Byte((byte) 100);
    Double boxD = new Double(1111.0);
    Float boxF = new Float(1111.0f);
    Integer boxI = new Integer(1111);
    Long boxL = new Long(1111L);
    Short boxS = new Short((short) 100);
    Boolean boxBool = new Boolean(true);
    Character boxC = new Character('a');

    // Unbox
    byte b = boxB;
    double d = boxD;
    float f = boxF;
    int i = boxI;
    long l = boxL;
    short s = boxS;
    boolean bool = boxBool;
    char c = boxC;
    assert (b == boxB.byteValue());
    assert (d == boxD.doubleValue());
    assert (f == boxF.floatValue());
    assert (i == boxI.intValue());
    assert (l == boxL.longValue());
    assert (s == boxS.shortValue());
    assert (bool == boxBool.booleanValue());
    assert (c == boxC.charValue());

    // Unbox and widen
    d = boxB;
    assert (d == boxB.byteValue());
    d = boxD;
    assert (d == boxD.doubleValue());
    d = boxF;
    assert (d == boxF.floatValue());
    d = boxI;
    assert (d == boxI.intValue());
    d = boxL;
    assert (d == boxL.longValue());
    d = boxS;
    assert (d == boxS.shortValue());
    d = boxC;
    assert (d == boxC.charValue());
  }

  public void testUnboxByOperator() {
    // non side effect prefix operations
    Integer i = new Integer(1111);
    i = +i;
    assert i.intValue() == 1111;
    i = -i;
    assert i.intValue() == -1111;
    i = ~i;
    assert i.intValue() == 1110;
    Boolean bool = new Boolean(true);
    bool = !bool;
    assert !bool.booleanValue();

    // non side effect binary operations
    Integer i1 = new Integer(100);
    Integer i2 = new Integer(200);
    Integer i3 = new Integer(4);
    int sumI = i1 + i2;
    Integer boxSumI = i1 + i2;
    assert sumI == 300;
    assert boxSumI.intValue() == 300;
    assert boxSumI != 0;
    assert boxSumI < 400;
    assert boxSumI > 200;
    assert boxSumI <= 300;
    assert boxSumI >= 300;
    int shiftedI = i2 << i3;
    assert shiftedI == 3200;

    Long l1 = new Long(1000L);
    Long l2 = new Long(2000L);
    long sumL = l1 + l2;
    Long boxSumL = l1 + l2;
    assert (sumL == 3000L);
    assert (boxSumL.longValue() == sumL);

    Double d1 = new Double(1111.1);
    Double d2 = new Double(2222.2);
    double sumD = d1 + d2;
    Double boxSumD = d1 + d2;
    assert (boxSumD.doubleValue() == sumD);

    Boolean b1 = new Boolean(true);
    Boolean b2 = new Boolean(false);
    Boolean boxB = b1 && b2;
    boolean b = b1 || b2;
    assert (!boxB.booleanValue());
    assert (b);
  }

  private int foo = 0;

  public void testNull() {
    // Avoiding a "condition always evaluates to true" error in JSComp type checking.
    Object maybeNull = foo == 0 ? null : new Object();

    Boolean bool = null;
    Double d = null;
    Integer i = null;
    Long l = null;
    assert bool == maybeNull;
    assert d == maybeNull;
    assert i == maybeNull;
    assert l == maybeNull;
  }

  public void testAllNumericTypes() {
    Byte b = 0;
    Character c = (char) 0;
    Short s = 0;
    Integer i = 0;
    Long l = 0L;
    Float f = 0f;
    Double d = 0d;

    b++;
    c++;
    s++;
    i++;
    l++;
    f++;
    d++;

    assert b == 1;
    assert c == 1;
    assert s == 1;
    assert i == 1;
    assert l == 1L;
    assert f == 1f;
    assert d == 1d;

    for (int j = 0; j < 200; j++) {
      b++;
    }
    assert b == -55;
  }

  public void testTernary() {
    Integer boxedValue = new Integer(1);
    int primitiveValue = 10;

    Integer boxedResult;
    int primitiveResult;

    // Just to avoid JSCompiler being unhappy about "suspicious code" when seeing a ternary that
    // always evaluates to true.
    boolean alwaysTrue = foo == 0;

    boxedResult = alwaysTrue ? boxedValue : boxedValue;
    assert boxedResult == 1;

    boxedResult = alwaysTrue ? boxedValue : primitiveValue;
    assert boxedResult == 1;

    boxedResult = alwaysTrue ? primitiveValue : boxedValue;
    assert boxedResult == 10;

    boxedResult = alwaysTrue ? primitiveValue : primitiveValue;
    assert boxedResult == 10;

    primitiveResult = alwaysTrue ? boxedValue : boxedValue;
    assert primitiveResult == 1;

    primitiveResult = alwaysTrue ? boxedValue : primitiveValue;
    assert primitiveResult == 1;

    primitiveResult = alwaysTrue ? primitiveValue : boxedValue;
    assert primitiveResult == 10;

    primitiveResult = alwaysTrue ? primitiveValue : primitiveValue;
    assert primitiveResult == 10;
  }

  @SuppressWarnings("cast")
  public void testCasts() {
    // Box
    Integer boxedInteger = (Integer) 100;
    // Unbox
    int primitiveInteger = (int) new Integer(100);
    // Unbox and widen
    double primitiveDouble = (double) new Integer(100);

    assert boxedInteger instanceof Integer;
    assert primitiveInteger == 100;
    assert primitiveDouble == 100d;
  }

  @SuppressWarnings("cast")
  public void testArrayExpressions() {
    Integer boxedInteger1 = new Integer(100);
    Integer boxedInteger2 = new Integer(50);

    Object[] objects = new Object[boxedInteger1];
    assert objects.length == 100;
    objects[boxedInteger2] = this;
    assert objects[50] == this;

    Integer[] boxedIntegers = new Integer[] {1, 2, 3};
    assert boxedIntegers[0] instanceof Integer;
    int[] primitiveInts = new int[] {new Integer(1), new Integer(2), new Integer(3)};
    assert primitiveInts[0] == 1;
  }

  /**
   * Actually the boolean conditional unboxings don't get inserted and aren't needed because we have
   * devirtualized Boolean.
   */
  @SuppressWarnings("LoopConditionChecker")
  public void testConditionals() {
    Boolean boxedFalseBoolean = new Boolean(false);

    if (boxedFalseBoolean) {
      // If unboxing is missing we'll arrive here.
      doFail();
    }

    while (boxedFalseBoolean) {
      // If unboxing is missing we'll arrive here.
      doFail();
    }

    int count = 0;
    do {
      if (count > 0) {
        // If unboxing is missing we'll arrive here.
        doFail();
      }
      count++;
    } while (boxedFalseBoolean);

    for (; boxedFalseBoolean; ) {
      // If unboxing is missing we'll arrive here.
      doFail();
    }

    Object unusedBlah = boxedFalseBoolean ? doFail() : doNothing();

    // This one actually matters since we don't devirtualize Integer.
    switch (new Integer(100)) {
      case 100:
        // fine
        break;
      default:
        // If unboxing is missing we'll arrive here.
        doFail();
    }
  }

  public Object doFail() {
    assert false;
    return null;
  }

  public Object doNothing() {
    return null;
  }

  public static void main(String[] args) {
    Main m = new Main();
    m.testBoxByParameter();
    m.testBoxByAssignment();
    m.testUnboxByParameter();
    m.testUnboxByAssignment();
    m.testUnboxByOperator();
    m.testNull();
    m.testAllNumericTypes();
    m.testTernary();
    m.testCasts();
    m.testArrayExpressions();
    m.testConditionals();
  }
}
