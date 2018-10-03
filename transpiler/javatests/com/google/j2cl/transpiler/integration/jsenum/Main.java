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
package com.google.j2cl.transpiler.integration.jsenum;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsOverlay;

public class Main {

  private static final Object OK_STRING = "Ok";
  private static final Object HELLO_STRING = "Hello";
  private static final Object ONE_DOUBLE = 1.0d;
  private static final Object FALSE_BOOLEAN = false;

  public static void main(String... args) {
    testNativeJsEnum();
    testStringNativeJsEnum();
    testJsEnum();
    testBooleanJsEnum();
    testStringJsEnum();
  }

  @JsEnum(isNative = true, namespace = "test")
  enum NativeEnum {
    OK,
    CANCEL;
  }

  public static void testNativeJsEnum() {
    NativeEnum v = NativeEnum.OK;
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
          NativeEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == NativeEnum.OK);
    assertTrue(v != NativeEnum.CANCEL);
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assert v != OK_STRING;

    assertTrue(v.hashCode() == NativeEnum.OK.hashCode());
    assertTrue(v.hashCode() != NativeEnum.CANCEL.hashCode());
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // enum share instances.
    // assertTrue(v.hashCode() == StringNativeEnum.OK.hashCode());
    assertTrue(v.toString().equals(OK_STRING));
    assertTrue(v.equals(NativeEnum.OK));
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v.equals(OK_STRING));
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // enum share instances.
    // assertTrue(v.equals(StringNativeEnum.OK));

    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v instanceof Enum);
    // assertTrue(v instanceof NativeEnum);
    // assertFalse((Object) v instanceof String);
    // assertFalse(v instanceof Comparable);
    // assertFalse(v instanceof Serializable);
    // assertFalse(Object) v instanceof PlainJsEnum);
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // enum share instances.
    // assertTrue((Object) v instanceof StringNativeEnum);

    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(new Object() instanceof NativeEnum);
    // assertFalse(OK_STRING instanceof NativeEnum);
  }

  @JsEnum(isNative = true, namespace = "test", name = "NativeEnum", hasCustomValue = true)
  enum StringNativeEnum {
    OK,
    CANCEL;

    private String value;

    @JsOverlay
    public String getValue() {
      return value;
    }
  }

  public static void testStringNativeJsEnum() {
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
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // assertTrue((Object) v != OK_STRING);

    assertTrue(v.hashCode() == StringNativeEnum.OK.hashCode());
    assertTrue(v.hashCode() != StringNativeEnum.CANCEL.hashCode());
    assertTrue(v.toString().equals(OK_STRING));
    assertTrue(v.equals(StringNativeEnum.OK));
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // enum share instances.
    //
    // assertTrue(v.equals(NativeEnum.OK));
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v.equals(OK_STRING));

    assertTrue(v.getValue().equals(v.toString()));
    assertTrue(v.getValue().equals(OK_STRING));
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v instanceof Enum);
    // assertTrue(v instanceof StringNativeEnum);
    // assertFalse((Object) v instanceof String);
    // assertFalse(v instanceof Comparable);
    // assertFalse(v instanceof Serializable);
    // assertFalse(Object) v instanceof PlainJsEnum);
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // enum share instances.
    //
    // assertTrue((Object) v instanceof NativeEnum);

    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(new Object() instanceof StringNativeEnum);
    // assertFalse((Object) OK_STRING instanceof StringNativeEnum);
  }

  @JsEnum
  enum PlainJsEnum {
    ZERO,
    ONE;

    public int getValue() {
      return ordinal();
    }
  }

  @JsEnum
  enum OtherPlainJsEnum {
    NONE,
    UNIT
  }

  public static void testJsEnum() {
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

    assertThrows(
        NullPointerException.class,
        () -> {
          PlainJsEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == PlainJsEnum.ONE);
    assertTrue(v != PlainJsEnum.ZERO);
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // assertTrue((Object) v != ONE_DOUBLE);

    assertTrue(v.hashCode() == PlainJsEnum.ONE.hashCode());
    assertTrue(v.hashCode() != PlainJsEnum.ZERO.hashCode());
    assertTrue(v.toString().equals(String.valueOf(ONE_DOUBLE)));
    assertTrue(v.equals(PlainJsEnum.ONE));
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v.equals(ONE_DOUBLE));

    assertTrue(v.getValue() == 1);
    assertTrue(v.ordinal() == 1);
    assertTrue(PlainJsEnum.ONE.compareTo(v) == 0);
    assertTrue(PlainJsEnum.ZERO.compareTo(v) < 0);
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertThrows(ClassCastException.class, () -> {
    //   Comparable comparable = PlainJsEnum.ONE;
    //   comparable.compareTo(OtherPlainJsEnum.UNIT);
    // });
    // assertThrows(ClassCastException.class, () -> {
    //   Comparable comparable = PlainJsEnum.ONE;
    //   comparable.compareTo(ONE_DOUBLE);
    // });
    // assertThrows(ClassCastException.class, () -> {
    //   Comparable comparable = (Comparable) ONE_DOUBLE;
    //   comparable.compareTo(PlainJsEnum.ONE);
    // });

    // Test that boxing of special method 'ordinal()' call is not broken by normalization.
    Integer i = v.ordinal();
    assertTrue(i.intValue() == 1);

    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v instanceof Enum);
    // assertTrue(v instanceof PlainJsEnum);
    // assertFalse((Object) v instanceof Double);
    // assertTrue(v instanceof Comparable);
    // assertTrue(v instanceof Serializable);
    // assertFalse(Object) v instanceof BooleanJsEnum);

    // assertFalse(new Object() instanceof PlainJsEnum);
    // assertFalse((Object) ONE_DOUBLE instanceof PlainJsEnum);
  }

  @JsEnum(hasCustomValue = true)
  enum BooleanJsEnum {
    TRUE(true),
    FALSE(false);

    boolean value;

    BooleanJsEnum(boolean value) {
      this.value = value;
    }
  }

  public static void testBooleanJsEnum() {
    BooleanJsEnum v = BooleanJsEnum.FALSE;
    switch (v) {
      case TRUE:
        fail();
        break;
      case FALSE:
        break;
      default:
        fail();
        break;
    }

    assertThrows(
        NullPointerException.class,
        () -> {
          BooleanJsEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == BooleanJsEnum.FALSE);
    assertTrue(v != BooleanJsEnum.TRUE);
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // assert (Object) v != FALSE_BOOLEAN;

    assertTrue(v.hashCode() == BooleanJsEnum.FALSE.hashCode());
    assertTrue(v.hashCode() != BooleanJsEnum.TRUE.hashCode());
    assertTrue(v.toString().equals(String.valueOf(FALSE_BOOLEAN)));
    assertTrue(v.equals(BooleanJsEnum.FALSE));
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v.equals(FALSE_BOOLEAN));

    assertTrue((Object) v.value == FALSE_BOOLEAN);
    // Test that boxing of special field 'value' call is not broken by normalization.
    Boolean b = v.value;
    assertTrue(b == FALSE_BOOLEAN);

    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v instanceof Enum);
    // assertTrue(v instanceof BooleanJsEnum);
    // assertFalse((Object) v instanceof Boolean);
    // assertTrue(v instanceof Comparable);
    // assertTrue(v instanceof Serializable);
    // assertFalse(Object) v instanceof PlainJsEnum);

    // assertFalse(new Object() instanceof BooleanJsEnum);
    // assertFalse((Object) FALSE_BOOLEAN instanceof BooleanJsEnum);
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

  public static void testStringJsEnum() {
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

    assertThrows(
        NullPointerException.class,
        () -> {
          StringJsEnum nullJsEnum = null;
          switch (nullJsEnum) {
          }
        });

    assertTrue(v == StringJsEnum.HELLO);
    assertTrue(v != StringJsEnum.GOODBYE);
    // TODO(b/114118635): Uncomment when different native classes pointing to the same closure
    // assert (Object) v != HELLO_STRING;

    assertTrue(v.hashCode() == StringJsEnum.HELLO.hashCode());
    assertTrue(v.hashCode() != StringJsEnum.GOODBYE.hashCode());
    assertTrue(v.toString().equals(HELLO_STRING));
    assertTrue(v.equals(StringJsEnum.HELLO));
    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v.equals(HELLO_STRING));

    assertTrue(v.value.equals(HELLO_STRING));

    // TODO(b/116459408) : uncomment this when jsenum boxing is implemented.
    // assertFalse(v instanceof Enum);
    // assertTrue(v instanceof StringJsEnum);
    // assertFalse((Object) v instanceof String);
    // assertTrue(v instanceof Comparable);
    // assertTrue(v instanceof Serializable);
    // assertFalse(Object) v instanceof PlainJsEnum);

    // assertFalse(new Object() instanceof StringJsEnum);
    // assertFalse((Object) HELLO_STRING instanceof StringJsEnum);
  }

  private static <T> void assertThrows(Class<?> exceptionClass, Runnable runnable) {
    try {
      runnable.run();
    } catch (Throwable e) {
      if (e.getClass() == exceptionClass) {
        return;
      }
    }
    fail("Should have thrown " + exceptionClass.getSimpleName());
  }

  private static void assertTrue(boolean condition) {
    assert condition;
  }

  private static void assertFalse(boolean condition) {
    assertTrue(!condition);
  }

  public static void fail() {
    assert false;
  }

  public static void fail(String message) {
    assert false : message;
  }
}
