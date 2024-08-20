/*
 * Copyright 2018 Google Inc.
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
package jsenum;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertThrows;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.assertUnderlyingTypeEquals;
import static com.google.j2cl.integration.testing.Asserts.fail;
import static jsenum.NativeEnums.nativeClinitCalled;

import com.google.j2cl.integration.testing.TestUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import javaemul.internal.annotations.DoNotAutobox;
import javaemul.internal.annotations.UncheckedCast;
import javaemul.internal.annotations.Wasm;
import jsenum.NativeEnums.NativeEnum;
import jsenum.NativeEnums.NativeEnumWithClinit;
import jsenum.NativeEnums.NumberNativeEnum;
import jsenum.NativeEnums.StringNativeEnum;
import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {

  private static final Object OK_STRING = "Ok";
  private static final Object HELLO_STRING = "Hello";
  private static final Object ONE_DOUBLE = 1.0d;
  private static final Object FALSE_BOOLEAN = false;

  public static void main(String... args) {
    testNativeJsEnum();
    testStringNativeJsEnum();
    testCastOnNative();
    testComparableJsEnum();
    testComparableJsEnumAsSeenFromJs();
    testComparableJsEnumIntersectionCasts();
    testJsEnumVariableInitialization();
    testStringJsEnum();
    testStringJsEnumAsSeenFromJs();
    testJsEnumClassInitialization();
    testNativeEnumClassInitialization();
    testDoNotAutoboxJsEnum();
    testUnckeckedCastJsEnum();
    testReturnsAndParameters();
    testAutoBoxing_relationalOperations();
    testAutoBoxing_typeInference();
    testAutoBoxing_specialMethods();
    testAutoBoxing_parameterizedLambda();
    testAutoBoxing_intersectionCasts();
    testSpecializedSuperType();
    testSpecializedSuperTypeUnderlyingType();
    testBoxingPartialInlining();
    testNonNativeJsEnumArrays();
    testNonNativeStringJsEnumArrays();
    testNonNativeJsEnumArrayBoxing();
    testNativeJsEnumArray();
    testJsEnumVarargs();
  }

  @Wasm("nop") // TODO(b/288145698): Support native JsEnum.
  private static void testNativeJsEnum() {
    NativeEnum v = NativeEnum.ACCEPT;
    switch (v) {
      case ACCEPT:
        break;
      case CANCEL:
        fail();
        break;
      default:
        fail();
        break;
    }

    assertThrows(
        NullPointerException.class,
        () -> {
          NativeEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == NativeEnum.ACCEPT);
    assertTrue(v != NativeEnum.CANCEL);
    // Native JsEnums are not boxed.
    assertTrue(v == OK_STRING);
    assertTrue(v == (Object) StringNativeEnum.OK);

    // No boxing
    Object o = NativeEnum.ACCEPT;
    assertTrue(o == NativeEnum.ACCEPT);

    // Object methods calls on a variable of JsEnum type.
    assertTrue(v.hashCode() == NativeEnum.ACCEPT.hashCode());
    assertTrue(v.hashCode() != NativeEnum.CANCEL.hashCode());
    assertTrue(v.hashCode() == StringNativeEnum.OK.hashCode());
    assertTrue(v.toString().equals(OK_STRING));
    assertTrue(v.equals(NativeEnum.ACCEPT));
    assertTrue(v.equals(OK_STRING));
    assertTrue(v.equals(StringNativeEnum.OK));

    // Object methods calls on a variable of Object type.
    assertTrue(o.hashCode() == NativeEnum.ACCEPT.hashCode());
    assertTrue(o.hashCode() != NativeEnum.CANCEL.hashCode());
    assertTrue(o.hashCode() == StringNativeEnum.OK.hashCode());
    assertTrue(o.toString().equals(OK_STRING));
    assertTrue(o.equals(NativeEnum.ACCEPT));
    assertTrue(o.equals(OK_STRING));
    assertTrue(o.equals(StringNativeEnum.OK));

    assertFalse(v instanceof Enum);
    assertTrue((Object) v instanceof String);
    assertTrue(v instanceof Comparable);
    assertTrue(v instanceof Serializable);
    assertFalse((Object) v instanceof PlainJsEnum);

    NativeEnum ne = (NativeEnum) o;
    StringNativeEnum sne = (StringNativeEnum) o;
    Comparable ce = (Comparable) o;
    ce = (NativeEnum & Comparable<NativeEnum>) o;
    Serializable s = (Serializable) o;
    assertThrowsClassCastException(
        () -> {
          Object unused = (Enum) o;
        },
        Enum.class);
    assertThrowsClassCastException(
        () -> {
          Object unused = (Boolean) o;
        },
        Boolean.class);

    assertTrue(asSeenFromJs(NativeEnum.ACCEPT) == OK_STRING);
  }

  @JsMethod(name = "passThrough")
  @Wasm("nop") // TODO(b/288145698): Support native JsEnum.
  private static native Object asSeenFromJs(NativeEnum s);

  @Wasm("nop") // TODO(b/288145698): Support native JsEnum.
  private static void testStringNativeJsEnum() {
    StringNativeEnum v = StringNativeEnum.OK;
    switch (v) {
      case OK:
        break;
      case CANCEL:
        fail();
        break;
      default:
        fail();
        break;
    }

    assertThrows(
        NullPointerException.class,
        () -> {
          StringNativeEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == StringNativeEnum.OK);
    assertTrue(v != StringNativeEnum.CANCEL);
    assertTrue((Object) v == OK_STRING);
    assertTrue(v == (Object) NativeEnum.ACCEPT);

    Object o = StringNativeEnum.OK;
    assertTrue(o == StringNativeEnum.OK);

    // Object methods calls on a variable of JsEnum type.
    assertTrue(v.hashCode() == StringNativeEnum.OK.hashCode());
    assertTrue(v.hashCode() != StringNativeEnum.CANCEL.hashCode());
    assertTrue(v.toString().equals(OK_STRING));
    assertTrue(v.equals(StringNativeEnum.OK));
    assertTrue(v.equals(NativeEnum.ACCEPT));
    assertTrue(v.equals(OK_STRING));

    // Object methods calls on a variable of Object type.
    assertTrue(o.hashCode() == StringNativeEnum.OK.hashCode());
    assertTrue(o.hashCode() != StringNativeEnum.CANCEL.hashCode());
    assertTrue(o.toString().equals(OK_STRING));
    assertTrue(o.equals(StringNativeEnum.OK));
    assertTrue(o.equals(NativeEnum.ACCEPT));
    assertTrue(o.equals(OK_STRING));

    assertTrue(v.getValue().equals(v.toString()));
    assertTrue(v.getValue().equals(OK_STRING));

    assertFalse(v instanceof Enum);
    assertTrue((Object) v instanceof String);
    assertTrue(v instanceof Comparable);
    assertTrue(v instanceof Serializable);
    assertFalse((Object) v instanceof PlainJsEnum);

    Serializable se = (Serializable) o;
    StringNativeEnum sne = (StringNativeEnum) o;
    NativeEnum ne = (NativeEnum) o;
    Comparable ce = (Comparable) o;

    Comparable seAndC = (StringNativeEnum & Comparable<StringNativeEnum>) o;
    assertUnderlyingTypeEquals(String.class, seAndC);

    assertThrowsClassCastException(
        () -> {
          Object unused = (Enum) o;
        },
        Enum.class);
    assertThrowsClassCastException(
        () -> {
          Object unused = (Boolean) o;
        },
        Boolean.class);

    assertTrue(asSeenFromJs(StringNativeEnum.OK) == OK_STRING);
  }

  @Wasm("nop") // TODO(b/288145698): Support native JsEnum.
  public static void testCastOnNative() {
    castToNativeEnum(NativeEnum.ACCEPT);
    castToNativeEnum(StringNativeEnum.OK);
    castToNativeEnum(NumberNativeEnum.ONE);
    castToNativeEnum(PlainJsEnum.ONE);
    castToNativeEnum(OK_STRING);
    castToNativeEnum((Double) 2.0);
    castToNativeEnum((Integer) 1);

    castToStringNativeEnum(StringNativeEnum.OK);
    castToStringNativeEnum(NativeEnum.ACCEPT);
    castToStringNativeEnum(OK_STRING);
    assertThrowsClassCastException(() -> castToStringNativeEnum(NumberNativeEnum.ONE));
    assertThrowsClassCastException(() -> castToStringNativeEnum(PlainJsEnum.ONE));
    assertThrowsClassCastException(() -> castToStringNativeEnum((Integer) 1));
    assertThrowsClassCastException(() -> castToStringNativeEnum((Double) 2.0));

    castToNumberNativeEnum(NumberNativeEnum.ONE);
    castToNumberNativeEnum((Double) 2.0);
    assertThrowsClassCastException(() -> castToNumberNativeEnum(NativeEnum.ACCEPT));
    assertThrowsClassCastException(() -> castToNumberNativeEnum(StringNativeEnum.OK));
    assertThrowsClassCastException(() -> castToNumberNativeEnum(PlainJsEnum.ONE));
    assertThrowsClassCastException(() -> castToNumberNativeEnum((Integer) 1));
    assertThrowsClassCastException(() -> castToNumberNativeEnum(OK_STRING));
  }

  private static NativeEnum castToNativeEnum(Object o) {
    return (NativeEnum) o;
  }

  private static StringNativeEnum castToStringNativeEnum(Object o) {
    return (StringNativeEnum) o;
  }

  private static NumberNativeEnum castToNumberNativeEnum(Object o) {
    return (NumberNativeEnum) o;
  }

  @JsMethod(name = "passThrough")
  @Wasm("nop") // Non-native JsMethod not supported in Wasm.
  private static native Object asSeenFromJs(StringNativeEnum s);

  @JsEnum
  enum PlainJsEnum {
    @JsProperty(name = "NAUGHT")
    ZERO,
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN;

    public int getValue() {
      return ordinal();
    }
  }

  @JsEnum
  enum OtherPlainJsEnum {
    NONE,
    UNIT
  }

  private static void testComparableJsEnum() {
    PlainJsEnum v = PlainJsEnum.ONE;
    switch (v) {
      case ZERO:
        fail();
        break;
      case ONE:
        break;
      default:
        fail();
        break;
    }

    assertThrowsNullPointerException(
        () -> {
          PlainJsEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == PlainJsEnum.ONE);
    assertTrue(v != PlainJsEnum.ZERO);
    assertTrue((Object) v != ONE_DOUBLE);
    // Boxing preserves equality.
    Object o = PlainJsEnum.ONE;
    assertTrue(o == PlainJsEnum.ONE);

    // Object methods calls on a variable of JsEnum type.
    assertTrue(v.hashCode() == PlainJsEnum.ONE.hashCode());
    assertTrue(v.hashCode() != PlainJsEnum.ZERO.hashCode());
    assertTrue(v.toString().equals(String.valueOf(ONE_DOUBLE)));
    assertTrue(v.equals(PlainJsEnum.ONE));
    assertFalse(v.equals(ONE_DOUBLE));
    assertFalse(PlainJsEnum.ZERO.equals(OtherPlainJsEnum.NONE));

    assertThrows(
        NullPointerException.class,
        () -> {
          PlainJsEnum nullJsEnum = null;
          nullJsEnum.equals(PlainJsEnum.ZERO);
        });

    // Object methods calls on a variable of Object type.
    assertTrue(o.hashCode() == PlainJsEnum.ONE.hashCode());
    assertTrue(o.hashCode() != PlainJsEnum.ZERO.hashCode());
    assertTrue(o.toString().equals(String.valueOf(ONE_DOUBLE)));
    assertTrue(o.equals(PlainJsEnum.ONE));
    assertFalse(o.equals(PlainJsEnum.TWO));
    assertTrue(o.equals(v));
    assertFalse(o.equals(ONE_DOUBLE));

    assertTrue(v.getValue() == 1);
    assertTrue(v.ordinal() == 1);
    assertTrue(PlainJsEnum.ONE.compareTo(v) == 0);
    assertTrue(PlainJsEnum.ZERO.compareTo(v) < 0);
    assertThrows(
        NullPointerException.class,
        () -> {
          PlainJsEnum nullJsEnum = null;
          nullJsEnum.compareTo(PlainJsEnum.ZERO);
        });
    assertThrowsClassCastException(
        () -> {
          Comparable comparable = PlainJsEnum.ONE;
          comparable.compareTo(OtherPlainJsEnum.UNIT);
        });
    assertThrowsClassCastException(
        () -> {
          Comparable comparable = PlainJsEnum.ONE;
          comparable.compareTo(ONE_DOUBLE);
        });
    assertThrowsClassCastException(
        () -> {
          Comparable comparable = (Comparable) ONE_DOUBLE;
          comparable.compareTo(PlainJsEnum.ONE);
        });
    assertThrowsClassCastException(
        () -> {
          Object unused = (Enum<PlainJsEnum> & Comparable<PlainJsEnum>) PlainJsEnum.ONE;
        },
        Enum.class);

    // Test that boxing of special method 'ordinal()' call is not broken by normalization.
    Integer i = v.ordinal();
    assertTrue(i.intValue() == 1);

    if (!TestUtils.isWasm()) {
      // JsEnums are still instance of Enum in Wasm.
      assertFalse(v instanceof Enum);
    }
    assertTrue(v instanceof PlainJsEnum);
    assertFalse((Object) v instanceof Double);
    assertTrue(v instanceof Comparable);
    assertTrue(v instanceof Serializable);

    assertFalse(new Object() instanceof PlainJsEnum);
    assertFalse((Object) ONE_DOUBLE instanceof PlainJsEnum);

    PlainJsEnum pe = (PlainJsEnum) o;
    Comparable c = (Comparable) o;
    Serializable s = (Serializable) o;

    assertThrowsClassCastException(
        () -> {
          Object unused = (Enum) o;
        },
        Enum.class);
    assertThrowsClassCastException(
        () -> {
          Object unused = (Double) o;
        },
        Double.class);

    // Comparable test.
    SortedSet<Comparable> sortedSet = new TreeSet<>(Comparable::compareTo);
    sortedSet.add(PlainJsEnum.ONE);
    sortedSet.add(PlainJsEnum.ZERO);
    assertTrue(sortedSet.iterator().next() == PlainJsEnum.ZERO);
    assertTrue(sortedSet.iterator().next() instanceof PlainJsEnum);
  }

  @Wasm("nop") // Non-native JsMethod not supported in Wasm.
  private static void testComparableJsEnumAsSeenFromJs() {
    assertTrue(asSeenFromJs(PlainJsEnum.ONE) == ONE_DOUBLE);
  }

  @Wasm("nop") // TODO(b/182341814, b/295235576): DoNotAutobox not supported in Wasm. JsEnum class
  // literals not yet supported in Wasm.
  private static void testComparableJsEnumIntersectionCasts() {
    Object o = PlainJsEnum.ONE;
    // Intersection casts box/or unbox depending on the destination type.
    Comparable otherC = (PlainJsEnum & Comparable<PlainJsEnum>) o;
    assertUnderlyingTypeEquals(PlainJsEnum.class, otherC);
    PlainJsEnum otherPe = (PlainJsEnum & Comparable<PlainJsEnum>) o;
    assertUnderlyingTypeEquals(Double.class, otherPe);
  }

  @JsMethod(name = "passThrough")
  @Wasm("nop") // Non-native JsMethod not supported in Wasm.
  private static native Object asSeenFromJs(PlainJsEnum d);

  public static PlainJsEnum defaultStaticJsEnum;
  public static PlainJsEnum oneStaticJsEnum = PlainJsEnum.ONE;

  private static void testJsEnumVariableInitialization() {
    assertEquals(defaultStaticJsEnum, null);
    assertEquals(oneStaticJsEnum, PlainJsEnum.ONE);

    PlainJsEnum oneJsEnum = PlainJsEnum.ONE;
    assertEquals(oneJsEnum, PlainJsEnum.ONE);
  }

  @JsEnum(hasCustomValue = true)
  enum StringJsEnum {
    HELLO("Hello"),
    GOODBYE("Good Bye");

    String value;

    StringJsEnum(String value) {
      this.value = value;
    }
  }

  private static void testStringJsEnum() {
    StringJsEnum v = StringJsEnum.HELLO;
    switch (v) {
      case GOODBYE:
        fail();
        break;
      case HELLO:
        break;
      default:
        fail();
        break;
    }

    assertThrowsNullPointerException(
        () -> {
          StringJsEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == StringJsEnum.HELLO);
    assertTrue(v != StringJsEnum.GOODBYE);
    assertTrue((Object) v != HELLO_STRING);
    // Boxing preserves equality.
    Object o = StringJsEnum.HELLO;
    assertTrue(o == StringJsEnum.HELLO);

    // Object methods calls on a variable of JsEnum type.
    assertTrue(v.hashCode() == StringJsEnum.HELLO.hashCode());
    assertTrue(v.hashCode() != StringJsEnum.GOODBYE.hashCode());
    assertTrue(v.equals(StringJsEnum.HELLO));
    assertFalse(v.equals(HELLO_STRING));

    assertThrows(
        NullPointerException.class,
        () -> {
          StringJsEnum nullJsEnum = null;
          nullJsEnum.equals(StringJsEnum.HELLO);
        });

    // Object methods calls on a variable of Object type.
    assertTrue(o.hashCode() == StringJsEnum.HELLO.hashCode());
    assertTrue(o.hashCode() != StringJsEnum.GOODBYE.hashCode());
    assertTrue(o.equals(StringJsEnum.HELLO));
    assertFalse(o.equals(StringJsEnum.GOODBYE));
    assertTrue(o.equals(v));
    assertFalse(o.equals(HELLO_STRING));

    assertTrue(v.value.equals(HELLO_STRING));

    if (!TestUtils.isWasm()) {
      // JsEnums are still instance of Enum in Wasm.
      assertFalse(v instanceof Enum);
    }
    assertTrue(v instanceof StringJsEnum);
    assertFalse((Object) v instanceof String);
    if (!TestUtils.isWasm()) {
      // JsEnums are still instance of Enum in Wasm.
      assertFalse(v instanceof Comparable);
    }
    assertTrue(v instanceof Serializable);
    assertFalse((Object) v instanceof PlainJsEnum);

    assertFalse(new Object() instanceof StringJsEnum);
    assertFalse((Object) HELLO_STRING instanceof StringJsEnum);

    StringJsEnum se = (StringJsEnum) o;
    Serializable s = (Serializable) o;
    assertThrowsClassCastException(
        () -> {
          Object unused = (Enum) o;
        },
        Enum.class);
    assertThrowsClassCastException(
        () -> {
          Object unused = (Comparable) o;
        },
        Comparable.class);
    assertThrowsClassCastException(
        () -> {
          Object unused = (String) o;
        },
        String.class);
    assertThrowsClassCastException(
        () -> {
          Object unused = (StringJsEnum & Comparable<StringJsEnum>) o;
        },
        Comparable.class);

    if (!TestUtils.isWasm()) {
      // TODO(b/353352388): The value field is not used in toString in Wasm.
      assertTrue(v.toString().equals(HELLO_STRING));
      assertTrue(o.toString().equals(HELLO_STRING));
    }
  }

  @Wasm("nop") // Non-native JsMethod not supported in Wasm.
  private static void testStringJsEnumAsSeenFromJs() {
    assertTrue(asSeenFromJs(StringJsEnum.HELLO) == HELLO_STRING);
  }

  @JsMethod(name = "passThrough")
  @Wasm("nop") // Non-native JsMethod not supported in Wasm.
  private static native Object asSeenFromJs(StringJsEnum b);

  private static boolean nonNativeClinitCalled = false;

  @JsEnum
  enum EnumWithClinit {
    A;

    static {
      nonNativeClinitCalled = true;
    }

    int getValue() {
      return ordinal();
    }
  }

  @Wasm("nop") // In Wasm, there is no boxing logic and clinit is called for JsEnum value accesses.
  private static void testJsEnumClassInitialization() {
    assertFalse(nonNativeClinitCalled);
    // Access to an enum value does not trigger clinit.
    Object o = EnumWithClinit.A;
    assertFalse(nonNativeClinitCalled);

    // Cast and instanceof do not trigger clinit.
    if (o instanceof EnumWithClinit) {
      o = (EnumWithClinit) o;
    }
    assertFalse(nonNativeClinitCalled);

    // Access to ordinal() does not trigger clinit.
    int n = EnumWithClinit.A.ordinal();
    assertFalse(nonNativeClinitCalled);

    // Access to any devirtualized method triggers clinit.
    EnumWithClinit.A.getValue();
    assertTrue(nonNativeClinitCalled);
  }

  @Wasm("nop") // TODO(b/288145698): Support native JsEnum.
  private static void testNativeEnumClassInitialization() {
    assertFalse(nativeClinitCalled);
    // Access to an enum value does not trigger clinit.
    Object o = NativeEnumWithClinit.OK;
    assertFalse(nativeClinitCalled);

    // Cast does not trigger clinit.
    o = (NativeEnumWithClinit) o;
    assertFalse(nativeClinitCalled);

    // Access to value does not trigger clinit.
    String s = NativeEnumWithClinit.OK.value;
    assertFalse(nativeClinitCalled);

    // Access to any devirtualized method triggers clinit.
    NativeEnumWithClinit.OK.getValue();
    assertTrue(nativeClinitCalled);
  }

  @Wasm("nop") // TODO(b/182341814): DoNotAutobox not supported in Wasm.
  private static void testDoNotAutoboxJsEnum() {
    assertTrue(returnsObject(StringJsEnum.HELLO) == HELLO_STRING);
    assertTrue(returnsObject(0, StringJsEnum.HELLO) == HELLO_STRING);
  }

  private static Object returnsObject(@DoNotAutobox Object object) {
    return object;
  }

  private static Object returnsObject(int n, @DoNotAutobox Object... object) {
    return object[0];
  }

  @Wasm("nop") // Unchecked cast not supported in Wasm.
  private static void testUnckeckedCastJsEnum() {
    StringJsEnum s = uncheckedCast(HELLO_STRING);
    assertTrue(s == StringJsEnum.HELLO);
  }

  @UncheckedCast
  private static <T> T uncheckedCast(@DoNotAutobox Object object) {
    return (T) object;
  }

  private static void testReturnsAndParameters() {
    assertTrue(PlainJsEnum.ONE == returnsJsEnum());
    assertTrue(PlainJsEnum.ONE == returnsJsEnum(PlainJsEnum.ONE));
    assertTrue(null == returnsNullJsEnum());
    assertTrue(null == returnsJsEnum(null));

    Main.<PlainJsEnum>testGenericAssertNull(null);
  }

  private static PlainJsEnum returnsJsEnum() {
    return PlainJsEnum.ONE;
  }

  private static PlainJsEnum returnsJsEnum(PlainJsEnum value) {
    return value;
  }

  private static PlainJsEnum returnsNullJsEnum() {
    return null;
  }

  private static <T> void testGenericAssertNull(T obj) {
    assertTrue(obj == null);
  }

  private static void testAutoBoxing_relationalOperations() {
    PlainJsEnum one = PlainJsEnum.ONE;
    Object boxedOne = PlainJsEnum.ONE;
    assertTrue(one == boxingPassthrough(one));
    assertTrue(boxedOne == boxingPassthrough(one));
    assertTrue(boxingPassthrough(one) == one);
    assertTrue(boxingPassthrough(one) == boxedOne);
    assertFalse(one != boxedOne);
    assertFalse(boxedOne != one);
    assertFalse(one != boxingPassthrough(one));
    assertFalse(boxedOne != boxingPassthrough(one));
    assertFalse(boxingPassthrough(one) != one);
    assertFalse(boxingPassthrough(one) != boxedOne);

    // Comparison with a double object which is unboxed. Many of the comparisons, like
    // `1.0 == PlainJsEnum.ONE` are rejected by the compiler due to type incompatibility.
    assertFalse((Object) Double.valueOf(1.0) == PlainJsEnum.ONE);
    assertFalse(Double.valueOf(1.0) == boxedOne);
    assertThrowsClassCastException(
        () -> {
          boolean unused = 1.0 == (Double) boxedOne;
        });
  }

  private static <T> T boxingPassthrough(T t) {
    return t;
  }

  private static void testAutoBoxing_specialMethods() {
    assertTrue(PlainJsEnum.ONE.equals(PlainJsEnum.ONE));
    assertTrue(PlainJsEnum.ONE.compareTo(PlainJsEnum.ONE) == 0);
    assertTrue(PlainJsEnum.ONE.compareTo(PlainJsEnum.ZERO) > 0);
    assertTrue(PlainJsEnum.TWO.compareTo(PlainJsEnum.TEN) < 0);

    PlainJsEnum jsEnum = PlainJsEnum.ONE;
    PlainJsEnum nullJsEnum = null;
    Object objectJsEnum = PlainJsEnum.ONE;

    StringJsEnum stringJsEnum = StringJsEnum.HELLO;
    PlainJsEnum nullStringJsEnum = null;
    Object objectStringJsEnum = StringJsEnum.HELLO;

    assertFalse(jsEnum.equals(PlainJsEnum.TWO));
    assertTrue(jsEnum.equals(objectJsEnum));
    assertFalse(jsEnum.equals(nullJsEnum));
    assertFalse(jsEnum.equals(null));

    assertFalse(stringJsEnum.equals(StringJsEnum.GOODBYE));
    assertTrue(stringJsEnum.equals(objectStringJsEnum));
    assertFalse(stringJsEnum.equals(nullJsEnum));
    assertFalse(stringJsEnum.equals(null));

    assertFalse(jsEnum.equals(stringJsEnum));
  }

  @Wasm("nop") // TODO(b/182341814, b/295235576): DoNotAutobox not supported in Wasm. JsEnum class
  // literals not yet supported in Wasm.
  private static void testAutoBoxing_intersectionCasts() {
    Comparable c = (PlainJsEnum & Comparable<PlainJsEnum>) PlainJsEnum.ONE;
    assertTrue(c.compareTo(PlainJsEnum.ZERO) > 0);
    PlainJsEnum e = (PlainJsEnum & Comparable<PlainJsEnum>) PlainJsEnum.ONE;
    // e correcly holds an unboxed value.
    assertUnderlyingTypeEquals(Double.class, e);

    assertTrue(PlainJsEnum.ONE == (PlainJsEnum & Comparable<PlainJsEnum>) PlainJsEnum.ONE);
    // Intersection cast with a JsEnum does not unbox like the simple cast.
    assertUnderlyingTypeEquals(
        PlainJsEnum.class, (PlainJsEnum & Comparable<PlainJsEnum>) PlainJsEnum.ONE);
  }

  @Wasm("nop") // TODO(b/182341814, b/295235576): DoNotAutobox not supported in Wasm. JsEnum class
  // literals not yet supported in Wasm.
  private static void testAutoBoxing_typeInference() {
    assertUnderlyingTypeEquals(Double.class, PlainJsEnum.ONE);
    assertUnderlyingTypeEquals(PlainJsEnum.class, boxingIdentity(PlainJsEnum.ONE));

    // Make sure the enum is boxed even when assigned to a field that is inferred to be JsEnum.
    TemplatedField<PlainJsEnum> templatedField = new TemplatedField<PlainJsEnum>(PlainJsEnum.ONE);
    PlainJsEnum unboxed = templatedField.getValue();
    assertUnderlyingTypeEquals(Double.class, unboxed);
    // Boxing through specialized method parameter assignment.
    assertUnderlyingTypeEquals(PlainJsEnum.class, boxingIdentity(unboxed));
    // Unboxing as a qualifier to ordinal.
    assertUnderlyingTypeEquals(Double.class, templatedField.getValue().ordinal());

    // Boxing through specialized method parameter assignment.
    assertUnderlyingTypeEquals(PlainJsEnum.class, boxingIdentity(templatedField.getValue()));
    // Checks what is actually returned by getValue().
    assertUnderlyingTypeEquals(PlainJsEnum.class, ((TemplatedField) templatedField).getValue());

    unboxed = templatedField.value;
    assertUnderlyingTypeEquals(Double.class, unboxed);

    templatedField.value = PlainJsEnum.ONE;
    // Boxing through specialized method parameter assignment.
    assertUnderlyingTypeEquals(PlainJsEnum.class, boxingIdentity(templatedField.value));
    // Checks what is actually stored in value.
    assertUnderlyingTypeEquals(PlainJsEnum.class, ((TemplatedField) templatedField).value);
    // Unboxing as a qualifier to ordinal.
    assertUnderlyingTypeEquals(Double.class, templatedField.value.ordinal());

    // Boxing/unboxing in varargs.
    List<?> list = Arrays.asList(PlainJsEnum.ONE);
    assertUnderlyingTypeEquals(PlainJsEnum.class, list.get(0));
    unboxed = (PlainJsEnum) list.get(0);
    assertUnderlyingTypeEquals(Double.class, unboxed);

    // TODO(b/118615488): Rewrite the following checks when JsEnum arrays are allowed.
    // In Java the varargs array will be of the inferred argument type. Since non native JsEnum
    // arrays are not allowed, the created array is of the declared type.
    Object[] arr = varargsToComparableArray(PlainJsEnum.ONE);
    assertUnderlyingTypeEquals(Comparable[].class, arr);
    assertUnderlyingTypeEquals(PlainJsEnum.class, arr[0]);
    arr = varargsToObjectArray(PlainJsEnum.ONE);
    assertUnderlyingTypeEquals(Object[].class, arr);
    assertUnderlyingTypeEquals(PlainJsEnum.class, arr[0]);
  }

  private static class TemplatedField<T> {
    T value;

    TemplatedField(T value) {
      this.value = value;
    }

    T getValue() {
      return this.value;
    }
  }

  private static <T> Object boxingIdentity(T o) {
    return o;
  }

  private static <T extends Comparable> Object[] varargsToComparableArray(T... elements) {
    return elements;
  }

  private static <T> Object[] varargsToObjectArray(T... elements) {
    return elements;
  }

  private static void testAutoBoxing_parameterizedLambda() {

    Function<Object, Double> ordinalWithCast = e -> (double) ((PlainJsEnum) e).ordinal();
    assertTrue(1 == ordinalWithCast.apply(PlainJsEnum.ONE));

    Function<PlainJsEnum, Double> ordinal = e -> (double) e.ordinal();
    assertTrue(1 == ordinal.apply(PlainJsEnum.ONE));

    Function<? super PlainJsEnum, String> function =
        e -> {
          switch (e) {
            case ONE:
              return "ONE";
            default:
              return "None";
          }
        };
    assertEquals("ONE", function.apply(PlainJsEnum.ONE));

    Supplier<PlainJsEnum> supplier = () -> PlainJsEnum.ONE;
    assertEquals(PlainJsEnum.ONE, supplier.get());
  }

  private static class Container<T> {
    T field;

    T get() {
      return field;
    }

    void set(T t) {
      field = t;
    }
  }

  private static class PlainJsEnumContainer extends Container<PlainJsEnum> {
    PlainJsEnum get() {
      return super.get();
    }

    void set(PlainJsEnum plainJsEnum) {
      super.set(plainJsEnum);
    }
  }

  @JsType
  private static class JsTypeContainer<T> {
    private T field;

    public T get() {
      return field;
    }

    public void set(T t) {
      field = t;
    }
  }

  private static class JsTypePlainJsEnumContainer extends JsTypeContainer<PlainJsEnum> {
    public PlainJsEnum get() {
      return super.get();
    }

    public void set(PlainJsEnum plainJsEnum) {
      super.set(plainJsEnum);
    }
  }

  private static void testSpecializedSuperType() {
    PlainJsEnum five = PlainJsEnum.FIVE;
    PlainJsEnumContainer pc = new PlainJsEnumContainer();
    Container<PlainJsEnum> c = pc;
    pc.set(five);
    assertTrue(five == pc.get());
    assertTrue(five == ((Container<?>) c).get());
    PlainJsEnum six = PlainJsEnum.SIX;
    c.set(six);
    assertTrue(six == pc.get());
    assertTrue(six == ((Container<?>) c).get());
    // assertUnderlyingTypeEquals(PlainJsEnum.class, ((Container<?>) c).get());
    // assertUnderlyingTypeEquals(Double.class, pc.get());

    JsTypePlainJsEnumContainer jpc = new JsTypePlainJsEnumContainer();
    JsTypeContainer<PlainJsEnum> jc = jpc;
    jpc.set(five);
    assertTrue(five == jpc.get());
    assertTrue(five == ((JsTypeContainer<?>) jc).get());
    jc.set(six);
    assertTrue(six == jpc.get());
    assertTrue(six == ((JsTypeContainer<?>) jc).get());
    // assertUnderlyingTypeEquals(PlainJsEnum.class, ((JsTypeContainer<?>) jc).get());
    // assertUnderlyingTypeEquals(Double.class, jpc.get());
  }

  @Wasm("nop") // TODO(b/182341814, b/295235576): DoNotAutobox not supported in Wasm. JsEnum class
  // literals not yet supported in Wasm.
  private static void testSpecializedSuperTypeUnderlyingType() {
    PlainJsEnum five = PlainJsEnum.FIVE;
    PlainJsEnumContainer pc = new PlainJsEnumContainer();
    Container<PlainJsEnum> c = pc;
    pc.set(five);
    assertUnderlyingTypeEquals(PlainJsEnum.class, ((Container<?>) c).get());
    assertUnderlyingTypeEquals(Double.class, pc.get());

    JsTypePlainJsEnumContainer jpc = new JsTypePlainJsEnumContainer();
    JsTypeContainer<PlainJsEnum> jc = jpc;
    jpc.set(five);
    assertUnderlyingTypeEquals(PlainJsEnum.class, ((JsTypeContainer<?>) jc).get());
    assertUnderlyingTypeEquals(Double.class, jpc.get());
  }

  @JsMethod
  @Wasm("nop") // Non-native js methods not supported in Wasm.
  // Pass through an enum value as if it were coming from and going to JavaScript.
  private static Object passThrough(Object o) {
    // Supported closure enums can only have number, boolean or string as their underlying type.
    // Make sure that boxed enums are not passing though here.
    assertTrue(o instanceof String || o instanceof Double || o instanceof Boolean);
    return o;
  }

  private static void testBoxingPartialInlining() {
    // TODO(b/315214896) Check the size difference to see if cases such as these take advantage of
    // partial inlining in Wasm to turn this into a simple null check, avoiding boxing.
    PlainJsEnum nonnullJsEnum = PlainJsEnum.ONE;
    checkNotNull(nonnullJsEnum);
    // Use the local so it doesn't get removed.
    assertTrue(nonnullJsEnum == PlainJsEnum.ONE);

    PlainJsEnum nullJsEnum = null;
    assertThrowsNullPointerException(() -> checkNotNull(nullJsEnum));
    assertTrue(nullJsEnum == null);
  }

  private static void checkNotNull(Object obj) {
    if (obj == null) {
      throw new NullPointerException();
    }
  }

  private static void testNonNativeJsEnumArrays() {
    PlainJsEnum[] arr = new PlainJsEnum[] {PlainJsEnum.THREE, PlainJsEnum.TWO};
    assertTrue(arr.length == 2);
    assertTrue(arr[0] == PlainJsEnum.THREE);
    assertTrue(arr[1] == PlainJsEnum.TWO);

    PlainJsEnum[] arr2 = new PlainJsEnum[2];
    assertTrue(arr2.length == 2);
    arr2[0] = PlainJsEnum.THREE;
    arr2[1] = PlainJsEnum.TWO;
    assertTrue(arr2[0] == PlainJsEnum.THREE);
    assertTrue(arr2[1] == PlainJsEnum.TWO);

    PlainJsEnum[] arrayWithNull = new PlainJsEnum[] {null};
    assertTrue(arrayWithNull[0] == null);

    PlainJsEnum[] arrayWithDefaults = new PlainJsEnum[1];
    assertTrue(arrayWithDefaults[0] == null);

    Object[] objArray = new Object[] {PlainJsEnum.ONE};
    assertTrue(objArray[0] == PlainJsEnum.ONE);

    List<PlainJsEnum> list = new ArrayList<PlainJsEnum>();
    list.add(PlainJsEnum.ONE);
    assertTrue(list.toArray()[0] == PlainJsEnum.ONE);

    PlainJsEnum[][] nestedArr = new PlainJsEnum[][] {{PlainJsEnum.THREE}};
    assertTrue(nestedArr.length == 1);
    assertTrue(nestedArr[0].length == 1);
    assertTrue(nestedArr[0][0] == PlainJsEnum.THREE);

    nestedArr[0] = new PlainJsEnum[] {PlainJsEnum.TWO};
    assertTrue(nestedArr[0][0] == PlainJsEnum.TWO);
  }

  private static void testNonNativeStringJsEnumArrays() {
    StringJsEnum[] arr = new StringJsEnum[] {StringJsEnum.HELLO, StringJsEnum.GOODBYE};
    assertTrue(arr.length == 2);
    assertTrue(arr[0] == StringJsEnum.HELLO);
    assertTrue(arr[1] == StringJsEnum.GOODBYE);

    StringJsEnum[] arr2 = new StringJsEnum[2];
    assertTrue(arr2.length == 2);
    arr2[0] = StringJsEnum.HELLO;
    arr2[1] = StringJsEnum.GOODBYE;
    assertTrue(arr2[0] == StringJsEnum.HELLO);
    assertTrue(arr2[1] == StringJsEnum.GOODBYE);

    StringJsEnum[] arrayWithNull = new StringJsEnum[] {null};
    assertTrue(arrayWithNull[0] == null);

    StringJsEnum[] arrayWithDefaults = new StringJsEnum[1];
    assertTrue(arrayWithDefaults[0] == null);
  }

  @Wasm("nop") // JsEnum boxing not implemented in Wasm.
  private static void testNonNativeJsEnumArrayBoxing() {
    // JsEnums are stored as unboxed in an array.
    PlainJsEnum[] arr = new PlainJsEnum[] {PlainJsEnum.THREE};
    assertUnderlyingTypeEquals(Double.class, arr[0]);

    StringJsEnum[] arr2 = new StringJsEnum[] {StringJsEnum.HELLO};
    assertUnderlyingTypeEquals(String.class, arr2[0]);
  }

  @Wasm("nop") // TODO(b/288145698): Support native JsEnum.
  private static void testNativeJsEnumArray() {
    NativeEnum[] arr = new NativeEnum[] {NativeEnum.ACCEPT, NativeEnum.CANCEL};
    assertTrue(arr.length == 2);
    assertTrue(arr[0] == NativeEnum.ACCEPT);
    assertTrue(arr[1] == NativeEnum.CANCEL);

    NativeEnum[] arr2 = new NativeEnum[2];
    assertTrue(arr2.length == 2);
    arr2[0] = NativeEnum.ACCEPT;
    arr2[1] = NativeEnum.CANCEL;
    assertTrue(arr2[0] == NativeEnum.ACCEPT);
    assertTrue(arr2[1] == NativeEnum.CANCEL);

    NativeEnum[] arrayWithNull = new NativeEnum[] {null};
    assertTrue(arrayWithNull[0] == null);

    NativeEnum[] arrayWithDefaults = new NativeEnum[1];
    assertTrue(arrayWithDefaults[0] == null);

    NativeEnum[][] nestedArr = new NativeEnum[][] {{NativeEnum.ACCEPT}};
    assertTrue(nestedArr.length == 1);
    assertTrue(nestedArr[0].length == 1);
    assertTrue(nestedArr[0][0] == NativeEnum.ACCEPT);

    nestedArr[0] = new NativeEnum[] {NativeEnum.CANCEL};
    assertTrue(nestedArr[0][0] == NativeEnum.CANCEL);
  }

  private static void testJsEnumVarargs() {
    checkTVarargs(PlainJsEnum.ONE);
    checkJsEnumVarargs(PlainJsEnum.ONE);

    DerivedWithoutJsEnumVarargs d = new DerivedWithoutJsEnumVarargs();
    d.checkTVarargs(PlainJsEnum.ONE);

    BaseWithTVarargs b = new DerivedWithoutJsEnumVarargs();
    b.checkTVarargs(PlainJsEnum.ONE);
  }

  private static <T> void checkTVarargs(T... t) {
    assertTrue(t[0] == PlainJsEnum.ONE);
  }

  private static void checkJsEnumVarargs(PlainJsEnum... t) {
    assertTrue(t[0] == PlainJsEnum.ONE);
  }

  private static class BaseWithTVarargs<T> {
    public void checkTVarargs(T... t) {
      assertTrue(t[0] == PlainJsEnum.ONE);
    }
  }

  private static class DerivedWithoutJsEnumVarargs extends BaseWithTVarargs<PlainJsEnum> {}
}
