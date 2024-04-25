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
package autoboxing;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;
import static com.google.j2cl.integration.testing.TestUtils.getUndefined;
import static com.google.j2cl.integration.testing.TestUtils.isJavaScript;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsMethod;

@SuppressWarnings({
  "BoxedPrimitiveConstructor",
  "unchecked",
  "MissingDefault",
  "FallThrough",
  "TypeParameterShadowing",
  "NarrowingCompoundAssignment",
  "ReferenceEquality",
  "IdentityBinaryExpression",
  "SelfAssignment",
  "ReturnValueIgnored"
})
public class Main {

  public static void main(String... args) {
    testBox_byParameter();
    testBox_numberAsDouble();
    testBox_byAssignment();
    testBox_byCompoundAssignment();
    testUnbox_byParameter();
    testUnbox_byAssignment();
    testUnbox_byOperator();
    testUnbox_fromTypeVariable();
    testUnbox_fromIntersectionType();
    testUnbox_conditionals();
    testUnbox_switchExpression();
    testAutoboxing_arithmetic();
    testAutoboxing_equals();
    testAutoboxing_ternary();
    testAutoboxing_casts();
    testAutoboxing_arrayExpressions();
    testAutoboxing_compoundAssignmentSequence();
    testSideEffects_nestedIncrement();
    testSideEffects_compoundAssignment();
    testSideEffects_incrementDecrement();
    testNoBoxing_null();
  }

  private static void testBox_byParameter() {
    byte b = (byte) 100;
    double d = 1111.0;
    float f = 1111.0f;
    int i = 1111;
    long l = 1111L;
    short s = (short) 100;
    boolean bool = true;
    char c = 'a';
    assertTrue((unbox(b) == b));
    assertTrue((unbox(d) == d));
    assertTrue((unbox(f) == f));
    assertTrue((unbox(i) == i));
    assertTrue((unbox(l) == l));
    assertTrue((unbox(s) == s));
    assertTrue((unbox(bool) == bool));
    assertTrue((unbox(c) == c));

    assertTrue(boxToObject((byte) 100) instanceof Byte);
    assertTrue(boxToObject(1111.0) instanceof Double);
    assertTrue(boxToObject(1111.0f) instanceof Float);
    assertTrue(boxToObject(1111) instanceof Integer);
    assertTrue(boxToObject(1111L) instanceof Long);
    assertTrue(boxToObject((short) 100) instanceof Short);
    assertTrue(boxToObject(true) instanceof Boolean);
    assertTrue(boxToObject('a') instanceof Character);
  }

  private static void testBox_numberAsDouble() {
    // Works only in closure.
    if (!isJavaScript()) {
      return;
    }
    assertTrue((takesObjectAndReturnsPrimitiveDouble(3) == 3));
    assertTrue((sumWithoutBoxing(1, 1.5, (byte) 1, (short) 1, (float) 1) == 5.5));
    assertTrue((sumWithoutBoxingJsVarargs(1, 1.5, (byte) 1, (short) 1, (float) 1) == 5.5));
  }

  private static double takesObjectAndReturnsPrimitiveDouble(@DoNotAutobox Object o) {
    return (Double) o;
  }

  private static double sumWithoutBoxing(@DoNotAutobox Object... numbers) {
    double sum = 0;
    for (Object number : numbers) {
      sum += (Double) number;
    }
    return sum;
  }

  // JsMethods have different varargs semantics.
  @JsMethod
  private static double sumWithoutBoxingJsVarargs(@DoNotAutobox Object... numbers) {
    double sum = 0;
    for (Object number : numbers) {
      sum += (Double) number;
    }
    return sum;
  }

  private static byte unbox(Byte b) {
    return b.byteValue();
  }

  private static double unbox(Double d) {
    return d.doubleValue();
  }

  private static float unbox(Float f) {
    return f.floatValue();
  }

  private static int unbox(Integer i) {
    return i.intValue();
  }

  private static long unbox(Long l) {
    return l.longValue();
  }

  private static short unbox(Short s) {
    return s.shortValue();
  }

  private static boolean unbox(Boolean b) {
    return b.booleanValue();
  }

  private static char unbox(Character c) {
    return c.charValue();
  }

  private static Object boxToObject(Object o) {
    return o;
  }

  private static void testBox_byAssignment() {
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
    assertTrue((boxB.byteValue() == b));
    assertTrue((boxD.doubleValue() == d));
    assertTrue((boxF.floatValue() == f));
    assertTrue((boxI.intValue() == i));
    assertTrue((boxL.longValue() == l));
    assertTrue((boxS.shortValue() == s));
    assertTrue((boxBool.booleanValue() == bool));
    assertTrue((boxC.charValue() == c));

    Object o = (byte) 100;
    assertTrue(o instanceof Byte);
    o = 1111.0;
    assertTrue(o instanceof Double);
    o = 1111.0f;
    assertTrue(o instanceof Float);
    o = 1111;
    assertTrue(o instanceof Integer);
    o = 1111L;
    assertTrue(o instanceof Long);
    o = (short) 100;
    assertTrue(o instanceof Short);
    o = true;
    assertTrue(o instanceof Boolean);
    o = 'a';
    assertTrue(o instanceof Character);
  }

  private static void testBox_byCompoundAssignment() {
    Integer i = 10;
    assertIsBoxedInteger(i++);
    assertIsBoxedInteger(--i);
    assertTrue((i++).getClass() == Integer.class);
    assertTrue((--i).intValue() == 10);
  }

  private static void testUnbox_byParameter() {
    Byte boxB = new Byte((byte) 100);
    Double boxD = new Double(1111.0);
    Float boxF = new Float(1111.0f);
    Integer boxI = new Integer(1111);
    Long boxL = new Long(1111L);
    Short boxS = new Short((short) 100);
    Boolean boxBool = new Boolean(true);
    Character boxC = new Character('c');

    // Unbox
    assertTrue((box(boxB).equals(boxB)));
    assertTrue((box(boxD).equals(boxD)));
    assertTrue((box(boxF).equals(boxF)));
    assertTrue((box(boxI).equals(boxI)));
    assertTrue((box(boxL).equals(boxL)));
    assertTrue((box(boxS).equals(boxS)));
    assertTrue((box(boxBool).equals(boxBool)));
    assertTrue((box(boxC).equals(boxC)));

    // Unbox and widen
    assertTrue(takesAndReturnsPrimitiveDouble(boxB) == boxB.byteValue());
    assertTrue(takesAndReturnsPrimitiveDouble(boxC) == boxC.charValue());
    assertTrue(takesAndReturnsPrimitiveDouble(boxS) == boxS.shortValue());
    assertTrue(takesAndReturnsPrimitiveDouble(boxI) == boxI.intValue());
    assertTrue(takesAndReturnsPrimitiveDouble(boxL) == boxL.longValue());
    assertTrue(takesAndReturnsPrimitiveDouble(boxF) == boxF.floatValue());
  }

  private static double takesAndReturnsPrimitiveDouble(double d) {
    return d;
  }

  private static Byte box(byte b) {
    return new Byte(b);
  }

  private static Double box(double d) {
    return new Double(d);
  }

  private static Float box(float f) {
    return new Float(f);
  }

  private static Integer box(int i) {
    return new Integer(i);
  }

  private static Long box(long l) {
    return new Long(l);
  }

  private static Short box(short s) {
    return new Short(s);
  }

  private static Boolean box(boolean b) {
    return new Boolean(b);
  }

  private static Character box(char a) {
    return new Character(a);
  }

  private static void testUnbox_byAssignment() {
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
    assertTrue((b == boxB.byteValue()));
    assertTrue((d == boxD.doubleValue()));
    assertTrue((f == boxF.floatValue()));
    assertTrue((i == boxI.intValue()));
    assertTrue((l == boxL.longValue()));
    assertTrue((s == boxS.shortValue()));
    assertTrue((bool == boxBool.booleanValue()));
    assertTrue((c == boxC.charValue()));

    // Unbox and widen
    d = boxB;
    assertTrue((d == boxB.byteValue()));
    d = boxD;
    assertTrue((d == boxD.doubleValue()));
    d = boxF;
    assertTrue((d == boxF.floatValue()));
    d = boxI;
    assertTrue((d == boxI.intValue()));
    d = boxL;
    assertTrue((d == boxL.longValue()));
    d = boxS;
    assertTrue((d == boxS.shortValue()));
    d = boxC;
    assertTrue((d == boxC.charValue()));
  }

  private static void testUnbox_byOperator() {
    // non side effect prefix operations
    Integer i = new Integer(1111);
    i = +i;
    assertTrue(i.intValue() == 1111);
    i = -i;
    assertTrue(i.intValue() == -1111);
    i = ~i;
    assertTrue(i.intValue() == 1110);
    Boolean bool = new Boolean(true);
    bool = !bool;
    assertTrue(!bool.booleanValue());

    // non side effect binary operations
    Integer i1 = new Integer(100);
    Integer i2 = new Integer(200);
    Integer i3 = new Integer(4);
    int sumI = i1 + i2;
    Integer boxSumI = i1 + i2;
    assertTrue(sumI == 300);
    assertTrue(boxSumI.intValue() == 300);
    assertTrue(boxSumI != 0);
    assertTrue(boxSumI < 400);
    assertTrue(boxSumI > 200);
    assertTrue(boxSumI <= 300);
    assertTrue(boxSumI >= 300);
    int shiftedI = i2 << i3;
    assertTrue(shiftedI == 3200);

    Long l1 = new Long(1000L);
    Long l2 = new Long(2000L);
    long sumL = l1 + l2;
    Long boxSumL = l1 + l2;
    assertTrue((sumL == 3000L));
    assertTrue((boxSumL.longValue() == sumL));

    Double d1 = new Double(1111.1);
    Double d2 = new Double(2222.2);
    double sumD = d1 + d2;
    Double boxSumD = d1 + d2;
    assertTrue((boxSumD.doubleValue() == sumD));

    Boolean b1 = new Boolean(true);
    Boolean b2 = new Boolean(false);
    Boolean boxB = b1 && b2;
    boolean b3 = b1 || b2;
    assertTrue((!boxB.booleanValue()));
    assertTrue((b3));

    // Unboxing can cause NPE.
    Boolean b = null;
    assertThrowsNullPointerException(
        () -> {
          Object unused = b && b;
        });
    assertThrowsNullPointerException(
        () -> {
          Object unused = !b;
        });

    Double d = null;
    assertThrowsNullPointerException(
        () -> {
          Object unused = +d;
        });

    Integer n = null;
    assertThrowsNullPointerException(
        () -> {
          Object unused = -n;
        });

    Ref<Integer> shortInIntegerRef = (Ref) new Ref<Short>((short) 1);
    Ref<Integer> booleanInIntegerRef = (Ref) new Ref<Boolean>(true);
    Ref<Boolean> integerInBooleanRef = (Ref) new Ref<Integer>(1);
    Ref<String> integerInStringRef = (Ref) new Ref<Integer>(1);

    // Unboxing can cause ClassCastException.
    assertThrowsClassCastException(() -> booleanInIntegerRef.field++, Integer.class);

    assertThrowsClassCastException(
        () -> {
          Object unused = -booleanInIntegerRef.field;
        },
        Integer.class);

    assertThrowsClassCastException(
        () -> {
          Object unused = !integerInBooleanRef.field;
        },
        Boolean.class);

    assertThrowsClassCastException(() -> booleanInIntegerRef.field += 1, Integer.class);

    assertThrowsClassCastException(
        () -> {
          Object unused = 1 + booleanInIntegerRef.field;
        },
        Integer.class);

    assertThrowsClassCastException(
        () -> {
          Object unused = integerInBooleanRef.field || integerInBooleanRef.field;
        },
        Boolean.class);

    assertThrowsClassCastException(
        () -> {
          Object unused = integerInStringRef.field + integerInStringRef.field;
        },
        String.class);

    // TODO(b/254148464): J2CL is more strict about erasure casts than JVM. Here JVM does not throw
    // assertThrows while J2CL does.
    if (!isJvm()) {
      assertThrowsClassCastException(
          () -> integerInStringRef.field = integerInStringRef.field, String.class);
    }

    assertThrowsClassCastException(
        () -> {
          int unused = shortInIntegerRef.field;
        },
        Integer.class);

    assertThrowsClassCastException(() -> acceptsInt(shortInIntegerRef.field), Integer.class);

    // Should not throw since it should be converted into a string using String.valueOf(Object) and
    // thus does not require an erasure casts (the JLS requires just enough erasure casts to
    // make the program type safe).
    String unusedS = "" + booleanInIntegerRef.field;
  }

  private static void acceptsInt(int x) {}

  private static class Ref<T> {
    T field;

    Ref(T value) {
      field = value;
    }
  }

  private static int foo = 0;

  private static void testNoBoxing_null() {
    // Avoiding a "condition always evaluates to true" error in JSComp type checking.
    Object maybeNull = foo == 0 ? null : new Object();

    Boolean bool = null;
    Double d = null;
    Integer i = null;
    Long l = null;
    assertTrue(bool == maybeNull);
    assertTrue(d == maybeNull);
    assertTrue(i == maybeNull);
    assertTrue(l == maybeNull);
  }

  private static void testAutoboxing_arithmetic() {
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

    assertTrue(b == 1);
    assertTrue(c == 1);
    assertTrue(s == 1);
    assertTrue(i == 1);
    assertTrue(l == 1L);
    assertTrue(f == 1f);
    assertTrue(d == 1d);

    for (int j = 0; j < 200; j++) {
      b++;
    }
    assertTrue(b == -55);
  }

  @SuppressWarnings("BoxedPrimitiveEquality")
  private static void testAutoboxing_equals() {
    double zero = 0.0;
    double minusZero = -0.0;
    Double boxedZero = zero;
    Double boxedMinusZero = minusZero;
    Object asObjectZero = boxedZero;
    Object asObjectMinusZero = boxedMinusZero;
    Double nullDouble = null;
    // Obtain a JavaScript undefined value by accessing an array out of bounds (which J2CL allows).
    // For the JVM the tests satisfied by undefined should be the same as if it was null.
    Double undefinedDouble = getUndefined();

    // Unboxing semantics.
    assertTrue(zero == minusZero);
    assertTrue(minusZero == zero);
    assertTrue(boxedZero == minusZero);
    assertTrue(zero == boxedMinusZero);

    // Object semantics.
    assertTrue(boxedZero != boxedMinusZero);
    assertTrue(asObjectZero != asObjectMinusZero);

    assertTrue(undefinedDouble == nullDouble);

    // Explicit unboxing.
    assertTrue(((double) asObjectZero) == (double) asObjectMinusZero);

    assertThrowsNullPointerException(
        () -> {
          boolean unused = zero == nullDouble;
        });
    assertThrowsNullPointerException(
        () -> {
          boolean unused = nullDouble == zero;
        });
    assertThrowsNullPointerException(
        () -> {
          boolean unused = zero == undefinedDouble;
        });
    assertThrowsNullPointerException(
        () -> {
          boolean unused = undefinedDouble == zero;
        });
  }

  private static void testAutoboxing_ternary() {
    Integer boxedValue = new Integer(1);
    int primitiveValue = 10;

    Integer boxedResult;
    int primitiveResult;

    // Just to avoid JSCompiler being unhappy about "suspicious code" when seeing a ternary that
    // always evaluates to true.
    boolean alwaysTrue = foo == 0;

    boxedResult = alwaysTrue ? boxedValue : boxedValue;
    assertTrue(boxedResult == 1);

    boxedResult = alwaysTrue ? boxedValue : primitiveValue;
    assertTrue(boxedResult == 1);

    boxedResult = alwaysTrue ? primitiveValue : boxedValue;
    assertTrue(boxedResult == 10);

    boxedResult = alwaysTrue ? primitiveValue : primitiveValue;
    assertTrue(boxedResult == 10);

    primitiveResult = alwaysTrue ? boxedValue : boxedValue;
    assertTrue(primitiveResult == 1);

    primitiveResult = alwaysTrue ? boxedValue : primitiveValue;
    assertTrue(primitiveResult == 1);

    primitiveResult = alwaysTrue ? primitiveValue : boxedValue;
    assertTrue(primitiveResult == 10);

    primitiveResult = alwaysTrue ? primitiveValue : primitiveValue;
    assertTrue(primitiveResult == 10);

    Boolean b = null;
    assertThrowsNullPointerException(
        () -> {
          Object unused = b ? b : b;
        });
  }

  @SuppressWarnings("cast")
  private static void testAutoboxing_casts() {
    // Box
    Integer boxedInteger = (Integer) 100;
    // Unbox
    int primitiveInteger = (int) new Integer(100);
    // Unbox and widen
    double primitiveDouble = (double) new Integer(100);

    assertTrue(boxedInteger instanceof Integer);
    assertTrue(primitiveInteger == 100);
    assertTrue(primitiveDouble == 100d);
  }

  @SuppressWarnings("cast")
  private static void testAutoboxing_arrayExpressions() {
    Integer boxedInteger1 = new Integer(100);
    Integer boxedInteger2 = new Integer(50);

    Object[] objects = new Object[boxedInteger1];
    assertTrue(objects.length == 100);
    Object marker = new Object();
    objects[boxedInteger2] = marker;
    assertTrue(objects[50] == marker);

    Integer[] boxedIntegers = new Integer[] {1, 2, 3};
    assertTrue(boxedIntegers[0] instanceof Integer);
    int[] primitiveInts = new int[] {new Integer(1), new Integer(2), new Integer(3)};
    assertTrue(primitiveInts[0] == 1);
  }

  /**
   * Actually the boolean conditional unboxings don't get inserted and aren't needed because we have
   * devirtualized Boolean.
   */
  @SuppressWarnings("LoopConditionChecker")
  private static void testUnbox_conditionals() {
    Boolean boxedFalseBoolean = new Boolean(false);

    if (boxedFalseBoolean) {
      // If unboxing is missing we'll arrive here.
      fail();
    }

    while (boxedFalseBoolean) {
      // If unboxing is missing we'll arrive here.
      fail();
    }

    int count = 0;
    do {
      if (count > 0) {
        // If unboxing is missing we'll arrive here.
        fail();
      }
      count++;
    } while (boxedFalseBoolean);

    for (; boxedFalseBoolean; ) {
      // If unboxing is missing we'll arrive here.
      fail();
    }

    Object unusedBlah = boxedFalseBoolean ? doFail() : doNothing();

    Boolean b = null;

    assertThrowsNullPointerException(
        () -> {
          if (b) {}
        });

    assertThrowsNullPointerException(
        () -> {
          while (b) {}
        });

    assertThrowsNullPointerException(
        () -> {
          for (; b; ) {}
        });

    assertThrowsNullPointerException(
        () -> {
          do {} while (b);
        });

    assertThrowsNullPointerException(
        () -> {
          Integer i = null;
          switch (i) {
              // Some logic inside the switch to prevent jscompiler from optimizing it away.
            case 1:
              i = 3;
            default:
          }
        });
  }

  private static void testUnbox_switchExpression() {
    switch (new Integer(100)) {
      case 100:
        // fine
        break;
      default:
        // If unboxing is missing we'll arrive here.
        fail();
    }
  }

  private static void testAutoboxing_compoundAssignmentSequence() {
    Integer boxI = 1;
    int i = 2;
    boxI /* 6 */ += i /* 5 */ += boxI /* 3 */ += i /* 2*/;

    assertTrue(i == 5);
    assertTrue(boxI == 6);
  }

  private static <T extends Long> void testUnbox_fromTypeVariable() {
    T n = (T) (Long) 10L;
    // Auto unboxing from variable n.
    long l = n;
    assertIsBoxedLong(n);
    assertTrue(l == 10L);

    class Local<T extends Long> {
      long toLong(T l) {
        // Auto unboxing from variable l.
        assertTrue(l.equals(11L));
        return l;
      }
    }

    // Auto boxing parameter.
    l = new Local<>().toLong(11L);
    assertTrue(l == 11L);
  }

  private static <T extends Long & Comparable<Long>> void testUnbox_fromIntersectionType() {
    T n = (T) (Long) 10L;
    // Auto unboxing from variable n.
    long l = n;
    assertIsBoxedLong(n);
    assertTrue(l == 10L);

    class Local<T extends Long & Comparable<Long>> {
      long toLong(T l) {
        // Auto unboxing from variable l.
        assertTrue(l.equals(11L));
        return l;
      }
    }

    // Auto boxing parameter.
    l = new Local<>().toLong(11L);
    assertTrue(l == 11L);
  }

  private static class SideEffectTester {
    private Boolean booleanField = new Boolean(true);
    private Double doubleField = new Double(1111.1);
    private Integer integerField = new Integer(1111);
    private Long longField = new Long(1111L);

    private int sideEffectCount;

    private SideEffectTester causeSideEffect() {
      sideEffectCount++;
      return this;
    }

    private SideEffectTester fluentAssertEquals(Object expectedValue, Object testValue) {
      assertEquals(expectedValue, testValue);
      return this;
    }
  }

  /** Test increment and decrement operators on boxing values. */
  private static void testSideEffects_incrementDecrement() {
    SideEffectTester tester = new SideEffectTester();

    assertTrue(tester.sideEffectCount == 0);

    assertEquals(new Integer(1111), tester.causeSideEffect().integerField++);
    assertTrue(tester.integerField.equals(new Integer(1112)));
    assertTrue(tester.sideEffectCount == 1);
    assertEquals(new Long(1111L), tester.causeSideEffect().longField++);
    assertTrue(tester.longField.equals(new Long(1112L)));
    assertTrue(tester.sideEffectCount == 2);

    assertEquals(new Integer(1112), tester.causeSideEffect().integerField--);
    assertTrue(tester.integerField.equals(new Integer(1111)));
    assertTrue(tester.sideEffectCount == 3);
    assertEquals(new Long(1112L), tester.causeSideEffect().longField--);
    assertTrue(tester.longField.equals(new Long(1111L)));
    assertTrue(tester.sideEffectCount == 4);

    assertEquals(new Integer(1112), ++tester.causeSideEffect().integerField);
    assertTrue(tester.integerField.intValue() == 1112);
    assertTrue(tester.sideEffectCount == 5);
    assertEquals(new Long(1112L), ++tester.causeSideEffect().longField);
    assertTrue(tester.longField.longValue() == 1112L);
    assertTrue(tester.sideEffectCount == 6);

    assertEquals(new Integer(1111), --tester.causeSideEffect().integerField);
    assertTrue(tester.integerField.intValue() == 1111);
    assertTrue(tester.sideEffectCount == 7);
    assertEquals(new Long(1111L), --tester.causeSideEffect().longField);
    assertTrue(tester.longField.longValue() == 1111L);
    assertTrue(tester.sideEffectCount == 8);

    assertEquals(new Double(1111.1), tester.causeSideEffect().doubleField++);
    assertTrue(tester.doubleField.equals(new Double(1112.1)));
    assertEquals(new Double(1112.1), tester.causeSideEffect().doubleField--);
    assertTrue(tester.doubleField.equals(new Double(1111.1)));
    assertEquals(new Double(1112.1), ++tester.causeSideEffect().doubleField);
    assertTrue(tester.doubleField.doubleValue() == 1112.1);
    assertEquals(new Double(1111.1), --tester.causeSideEffect().doubleField);
    assertTrue(tester.doubleField.doubleValue() == 1111.1);
  }

  /** Test compound assignment. */
  private static void testSideEffects_compoundAssignment() {
    SideEffectTester tester = new SideEffectTester();

    tester.causeSideEffect().doubleField += 10.0;
    assertTrue(tester.doubleField.doubleValue() == 1121.1);
    assertTrue(tester.sideEffectCount == 1);
    tester.causeSideEffect().integerField += 10;
    assertTrue(tester.integerField.intValue() == 1121);
    assertTrue(tester.sideEffectCount == 2);
    tester.causeSideEffect().longField += 10L;
    assertTrue(tester.longField.longValue() == 1121L);
    assertTrue(tester.sideEffectCount == 3);
    tester.causeSideEffect().booleanField &= false;
    assertTrue(!tester.booleanField.booleanValue());
  }

  /** Test nested increments. */
  private static void testSideEffects_nestedIncrement() {
    SideEffectTester tester = new SideEffectTester();

    tester.fluentAssertEquals(
        new Long(1113L),
        tester.fluentAssertEquals(
                new Long(1112L),
                tester.fluentAssertEquals(new Long(1111L), tester.longField++).longField++)
            .longField++);

    tester
        .fluentAssertEquals(new Integer(1111), tester.integerField++)
        .fluentAssertEquals(new Integer(1112), tester.integerField++)
        .fluentAssertEquals(new Integer(1113), tester.integerField++);

    tester
        .fluentAssertEquals(
            new Double(1113.1),
            tester
                .fluentAssertEquals(new Double(1111.1), tester.doubleField++)
                .fluentAssertEquals(new Double(1112.1), tester.doubleField++)
                .doubleField++)
        .fluentAssertEquals(
            new Double(1116.1),
            tester
                .fluentAssertEquals(new Double(1114.1), tester.doubleField++)
                .fluentAssertEquals(new Double(1115.1), tester.doubleField++)
                .doubleField++);
  }

  private static Object doFail() {
    fail();
    return null;
  }

  private static Object doNothing() {
    return null;
  }

  private static void assertIsBoxedInteger(@DoNotAutobox Object object) {
    assertTrue(object instanceof Integer);
  }

  private static void assertIsBoxedLong(@DoNotAutobox Object object) {
    assertTrue(object instanceof Long);
  }
}
