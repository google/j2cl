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

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test comparison of various types of objects under various circumstances. Its main purpose is to
 * ensure that identity comparisons work properly whether object values are null, non-null, or
 * undefined (which is now being treated as equivalent to null); and also ensure no String equality
 * comparisons occur.
 */
@RunWith(JUnit4.class)
@SuppressWarnings({"ReferenceEquality", "MethodCanBeStatic", "ConstantField", "MissingDefault"})
public class ObjectIdentityTest {

  /**
   * An object that toString()s to a given value, used to ensure String equality is never used
   * inappropriately.
   */
  private static class Foo {
    private String s;

    public Foo(String s) {
      this.s = s;
    }

    @Override
    public String toString() {
      return s;
    }
  }

  private static volatile boolean TRUE = true;

  /*
   * Using volatile to ensure that the compiler is unsure about null-ness.
   */
  private volatile String maybeNullStringIsNull = null;
  private volatile Object maybeNullObjectIsNull = null;
  private volatile Foo maybeNullFooIsNull = null;
  private Object maybeNullTightenStringIsNull = maybeNullStringIsNull;
  private Object maybeNullTightenFooIsNull = maybeNullFooIsNull;

  private volatile String maybeNullStringIsUndefined = undefinedString();
  private volatile Object maybeNullObjectIsUndefined = undefinedObject();
  private volatile Foo maybeNullFooIsUndefined = undefinedFoo();
  private Object maybeNullTightenStringIsUndefined = maybeNullStringIsUndefined;
  private Object maybeNullTightenFooIsUndefined = maybeNullFooIsUndefined;

  private String notNullString = "foo";
  private String notNullStringOther = "bar";
  private Object notNullObject = TRUE ? new Foo(notNullString) : new Object();
  private Object notNullObjectOther = TRUE ? new Foo(notNullStringOther) : new Object();
  private Foo notNullFoo = new Foo(notNullString);
  private Foo notNullFooOther = new Foo(notNullStringOther);
  private Object notNullTightenString = notNullString;
  private Object notNullTightenFoo = notNullFoo;

  private volatile String maybeNullString = notNullString;
  private volatile String maybeNullStringOther = notNullStringOther;
  private volatile Object maybeNullObject = notNullObject;
  private volatile Object maybeNullObjectOther = notNullObjectOther;
  private volatile Foo maybeNullFoo = notNullFoo;
  private volatile Foo maybeNullFooOther = notNullFooOther;
  private Object maybeNullTightenString = maybeNullString;
  private Object maybeNullTightenFoo = maybeNullFoo;

  @SuppressWarnings("SelfEquality")
  @Test
  public void test_MaybeNullFoo_MaybeNullFoo() {
    assertThat(maybeNullFoo == maybeNullFoo).isTrue();
    assertThat(maybeNullFoo != maybeNullFooOther).isTrue();
    assertThat(maybeNullFooIsNull != maybeNullFoo).isTrue();
    assertThat(maybeNullFooIsUndefined != maybeNullFoo).isTrue();
    assertThat(maybeNullFooIsNull == maybeNullFooIsUndefined).isTrue();
    assertThat(maybeNullFooIsUndefined == maybeNullFooIsNull).isTrue();
  }

  @Test
  public void test_MaybeNullFoo_MaybeNullObject() {
    assertThat(maybeNullFoo != maybeNullObject).isTrue();
    assertThat(maybeNullFooIsNull != maybeNullObject).isTrue();
    assertThat(maybeNullFooIsUndefined != maybeNullObject).isTrue();
    assertThat(maybeNullFooIsNull == maybeNullObjectIsNull).isTrue();
    assertThat(maybeNullFooIsNull == maybeNullObjectIsUndefined).isTrue();
    assertThat(maybeNullFooIsUndefined == maybeNullObjectIsNull).isTrue();
    assertThat(maybeNullFooIsUndefined == maybeNullObjectIsUndefined).isTrue();
  }

  @Test
  public void test_MaybeNullFoo_MaybeNullString() {
    assertThat(maybeNullFoo != maybeNullTightenString).isTrue();
    assertThat(maybeNullFooIsNull != maybeNullTightenString).isTrue();
    assertThat(maybeNullFooIsUndefined != maybeNullTightenString).isTrue();
    assertThat(maybeNullFooIsNull == maybeNullTightenStringIsNull).isTrue();
    assertThat(maybeNullFooIsNull == maybeNullTightenStringIsUndefined).isTrue();
    assertThat(maybeNullFooIsUndefined == maybeNullTightenStringIsNull).isTrue();
    assertThat(maybeNullFooIsUndefined == maybeNullTightenStringIsUndefined).isTrue();
  }

  @Test
  public void test_MaybeNullFoo_NotNullFoo() {
    assertThat(maybeNullFoo == notNullFoo).isTrue();
    assertThat(maybeNullFoo != notNullFooOther).isTrue();
    assertThat(maybeNullFooIsNull != notNullFoo).isTrue();
    assertThat(maybeNullFooIsUndefined != notNullFoo).isTrue();
  }

  @Test
  public void test_MaybeNullFoo_NotNullObject() {
    assertThat(maybeNullFoo != notNullObject).isTrue();
    assertThat(maybeNullFooIsNull != notNullObject).isTrue();
    assertThat(maybeNullFooIsUndefined != notNullObject).isTrue();
  }

  @Test
  public void test_MaybeNullFoo_NotNullString() {
    assertThat(maybeNullFoo != notNullTightenString).isTrue();
    assertThat(maybeNullFooIsNull != notNullTightenString).isTrue();
    assertThat(maybeNullFooIsUndefined != notNullTightenString).isTrue();
  }

  @Test
  public void test_MaybeNullFoo_null() {
    assertThat(maybeNullFoo != null).isTrue();
    assertThat(maybeNullFooIsNull == null).isTrue();
    assertThat(maybeNullFooIsUndefined == null).isTrue();
  }

  @Test
  public void test_MaybeNullObject_MaybeNullFoo() {
    assertThat(maybeNullObject != maybeNullFoo).isTrue();
    assertThat(maybeNullObjectIsNull != maybeNullFoo).isTrue();
    assertThat(maybeNullObjectIsUndefined != maybeNullFoo).isTrue();
    assertThat(maybeNullObjectIsNull == maybeNullFooIsNull).isTrue();
    assertThat(maybeNullObjectIsNull == maybeNullFooIsUndefined).isTrue();
    assertThat(maybeNullObjectIsUndefined == maybeNullFooIsNull).isTrue();
    assertThat(maybeNullObjectIsUndefined == maybeNullFooIsUndefined).isTrue();
  }

  @SuppressWarnings("SelfEquality")
  @Test
  public void test_MaybeNullObject_MaybeNullObject() {
    assertThat(maybeNullObject == maybeNullObject).isTrue();
    assertThat(maybeNullObject != maybeNullObjectOther).isTrue();
    assertThat(maybeNullObjectIsNull != maybeNullObject).isTrue();
    assertThat(maybeNullObjectIsUndefined != maybeNullObject).isTrue();
    assertThat(maybeNullObjectIsNull == maybeNullObjectIsUndefined).isTrue();
    assertThat(maybeNullObjectIsUndefined == maybeNullObjectIsNull).isTrue();
  }

  @Test
  public void test_MaybeNullObject_MaybeNullString() {
    assertThat(maybeNullObject != maybeNullString).isTrue();
    assertThat(maybeNullObjectIsNull != maybeNullString).isTrue();
    assertThat(maybeNullObjectIsUndefined != maybeNullString).isTrue();
    assertThat(maybeNullObjectIsNull == maybeNullStringIsNull).isTrue();
    assertThat(maybeNullObjectIsNull == maybeNullStringIsUndefined).isTrue();
    assertThat(maybeNullObjectIsUndefined == maybeNullStringIsNull).isTrue();
    assertThat(maybeNullObjectIsUndefined == maybeNullStringIsUndefined).isTrue();
  }

  @Test
  public void test_MaybeNullObject_NotNullFoo() {
    assertThat(maybeNullObject != notNullFoo).isTrue();
    assertThat(maybeNullObjectIsNull != notNullFoo).isTrue();
    assertThat(maybeNullObjectIsUndefined != notNullFoo).isTrue();
  }

  @Test
  public void test_MaybeNullObject_NotNullObject() {
    assertThat(maybeNullObject == notNullObject).isTrue();
    assertThat(maybeNullObjectIsNull != notNullObject).isTrue();
    assertThat(maybeNullObjectIsUndefined != notNullObject).isTrue();
  }

  @Test
  public void test_MaybeNullObject_NotNullString() {
    assertThat(maybeNullObject != notNullString).isTrue();
    assertThat(maybeNullObjectIsNull != notNullString).isTrue();
    assertThat(maybeNullObjectIsUndefined != notNullString).isTrue();
  }

  @Test
  public void test_MaybeNullObject_null() {
    assertThat(maybeNullObject != null).isTrue();
    assertThat(maybeNullObjectIsNull == null).isTrue();
    assertThat(maybeNullObjectIsUndefined == null).isTrue();
  }

  @Test
  public void test_MaybeNullString_MaybeNullFoo() {
    assertThat(maybeNullString != maybeNullTightenFoo).isTrue();
    assertThat(maybeNullStringIsNull != maybeNullTightenFoo).isTrue();
    assertThat(maybeNullStringIsUndefined != maybeNullTightenFoo).isTrue();
    assertThat(maybeNullStringIsNull == maybeNullTightenFooIsNull).isTrue();
    assertThat(maybeNullStringIsNull == maybeNullTightenFooIsUndefined).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullTightenFooIsNull).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullTightenFooIsUndefined).isTrue();
  }

  @Test
  public void test_MaybeNullString_MaybeNullObject() {
    assertThat(maybeNullString != maybeNullObject).isTrue();
    assertThat(maybeNullStringIsNull != maybeNullObject).isTrue();
    assertThat(maybeNullStringIsUndefined != maybeNullObject).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullObjectIsNull).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullObjectIsUndefined).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullObjectIsNull).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullObjectIsUndefined).isTrue();
  }

  @SuppressWarnings("SelfEquality")
  @Test
  public void test_MaybeNullString_MaybeNullString() {
    assertThat(maybeNullString == maybeNullString).isTrue();
    assertThat(maybeNullString != maybeNullStringOther).isTrue();
    assertThat(maybeNullStringIsNull != maybeNullString).isTrue();
    assertThat(maybeNullStringIsUndefined != maybeNullString).isTrue();
    assertThat(maybeNullStringIsNull == maybeNullStringIsNull).isTrue();
    assertThat(maybeNullStringIsNull == maybeNullStringIsUndefined).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullStringIsNull).isTrue();
    assertThat(maybeNullStringIsUndefined == maybeNullStringIsUndefined).isTrue();
  }

  @Test
  public void test_MaybeNullString_NotNullFoo() {
    assertThat(maybeNullString != notNullTightenFoo).isTrue();
    assertThat(maybeNullStringIsNull != notNullTightenFoo).isTrue();
    assertThat(maybeNullStringIsUndefined != notNullTightenFoo).isTrue();
  }

  @Test
  public void test_MaybeNullString_NotNullObject() {
    assertThat(maybeNullString != notNullObject).isTrue();
    assertThat(maybeNullStringIsNull != notNullObject).isTrue();
    assertThat(maybeNullStringIsUndefined != notNullObject).isTrue();
  }

  @Test
  public void test_MaybeNullString_NotNullString() {
    assertThat(maybeNullString == notNullString).isTrue();
    assertThat(maybeNullStringIsNull != notNullString).isTrue();
    assertThat(maybeNullStringIsUndefined != notNullString).isTrue();
  }

  @Test
  public void test_MaybeNullString_null() {
    assertThat(maybeNullString != null).isTrue();
    assertThat(maybeNullStringIsNull == null).isTrue();
    assertThat(maybeNullStringIsUndefined == null).isTrue();
  }

  @SuppressWarnings("SelfEquality")
  @Test
  public void test_NotNullFoo_NotNullFoo() {
    assertThat(notNullFoo == notNullFoo).isTrue();
    assertThat(notNullFoo != notNullFooOther).isTrue();
  }

  @Test
  public void test_NotNullFoo_NotNullObject() {
    assertThat(notNullFoo != notNullObject).isTrue();
  }

  @Test
  public void test_NotNullFoo_NotNullString() {
    assertThat(notNullFoo != notNullTightenString).isTrue();
  }

  @Test
  public void test_NotNullFoo_null() {
    assertThat(notNullFoo != null).isTrue();
  }

  @Test
  public void test_NotNullObject_NotNullFoo() {
    assertThat(notNullObject != notNullFoo).isTrue();
  }

  @SuppressWarnings("SelfEquality")
  @Test
  public void test_NotNullObject_NotNullObject() {
    assertThat(notNullObject == notNullObject).isTrue();
    assertThat(notNullObject != notNullObjectOther).isTrue();
  }

  @Test
  public void test_NotNullObject_NotNullString() {
    assertThat(notNullObject != notNullString).isTrue();
  }

  @Test
  public void test_NotNullObject_null() {
    assertThat(notNullObject != null).isTrue();
  }

  @Test
  public void test_NotNullString_NotNullFoo() {
    assertThat(notNullString != notNullTightenFoo).isTrue();
  }

  @Test
  public void test_NotNullString_NotNullObject() {
    assertThat(notNullString != notNullObject).isTrue();
  }

  @SuppressWarnings("SelfEquality")
  @Test
  public void test_NotNullString_NotNullString() {
    assertThat(notNullString == notNullString).isTrue();
    assertThat(notNullString != notNullStringOther).isTrue();
  }

  @Test
  public void test_NotNullString_null() {
    assertThat(notNullString != null).isTrue();
  }

  @Test
  public void test_null_MaybeNullFoo() {
    assertThat(null != maybeNullFoo).isTrue();
    assertThat(null == maybeNullFooIsNull).isTrue();
    assertThat(null == maybeNullFooIsUndefined).isTrue();
  }

  @Test
  public void test_null_MaybeNullObject() {
    assertThat(null != maybeNullObject).isTrue();
    assertThat(null == maybeNullObjectIsNull).isTrue();
    assertThat(null == maybeNullObjectIsUndefined).isTrue();
  }

  @Test
  public void test_null_MaybeNullString() {
    assertThat(null != maybeNullString).isTrue();
    assertThat(null == maybeNullStringIsNull).isTrue();
    assertThat(null == maybeNullStringIsUndefined).isTrue();
  }

  @Test
  public void test_null_NotNullFoo() {
    assertThat(null != notNullFoo).isTrue();
  }

  @Test
  public void test_null_NotNullObject() {
    assertThat(null != notNullObject).isTrue();
  }

  @Test
  public void test_null_NotNullString() {
    assertThat(null != notNullString).isTrue();
  }

  @Test
  public void test_null_null() {
    assertThat(null == null).isTrue();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "goog.global")
  private static class Native {
    @JsProperty(name = "undefined")
    static Foo undefinedFoo;

    @JsProperty(name = "undefined")
    static Object undefinedObject;

    @JsProperty(name = "undefined")
    static String undefinedString;
  }

  private Foo undefinedFoo() {
    return Native.undefinedFoo;
  }

  private Object undefinedObject() {
    return Native.undefinedObject;
  }

  private String undefinedString() {
    return Native.undefinedString;
  }
}
