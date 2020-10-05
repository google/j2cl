/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.optimization;

import static com.google.j2cl.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests that unreferenced enum values are effectively removed. */
@RunWith(JUnit4.class)
public class EnumOptimizationTest {

  enum Foo {
    UNREFERENCED_VALUE_1,
    UNREFERENCED_VALUE_2,
    UNREFERENCED_SUBCLASS_VALUE_1 {},
    UNREFERENCED_SUBCLASS_VALUE_2 {}
  }

  @JsMethod
  private void unusedEnumValues() {
    Foo v1 = Foo.UNREFERENCED_VALUE_1;
    Foo v2 = Foo.UNREFERENCED_VALUE_2;
    Foo v3 = Foo.UNREFERENCED_SUBCLASS_VALUE_1;
    Foo v4 = Foo.UNREFERENCED_SUBCLASS_VALUE_2;
  }

  @JsProperty
  private native Object getUnusedEnumValues();

  @Test
  public void unreferencedEnumValuesAreRemoved() {
    assertFunctionMatches(getUnusedEnumValues(), "");
  }

  @JsEnum
  enum MyJsEnum {
    ONE,
    TWO
  }

  @JsMethod
  private void unusedJsEnumBoxes() {
    Object o1 = MyJsEnum.ONE;
    Object o2 = MyJsEnum.TWO;
  }

  @JsProperty
  private native Object getUnusedJsEnumBoxes();

  @Test
  public void unreferencedBoxedJsEnumsAreRemoved() {
    assertFunctionMatches(getUnusedJsEnumBoxes(), "");
  }

  @JsMethod
  private boolean jsEnumReferenceEquality() {
    MyJsEnum e = MyJsEnum.TWO;
    return e == MyJsEnum.TWO;
  }

  @JsProperty
  private native Object getJsEnumReferenceEquality();

  @Test
  public void comparisonByReferenceDoesNotBox() {
    assertFunctionMatches(getJsEnumReferenceEquality(), "return !0;");
  }

  @JsMethod
  private boolean jsEnumEquals() {
    MyJsEnum e = MyJsEnum.TWO;
    return e.equals(MyJsEnum.TWO);
  }

  @JsProperty
  private native Object getJsEnumEquals();

  // TODO(b/117514489): Enable and complete the test when unnecessary boxing is not longer emitted.
  @Ignore
  public void equalsDoesNotBox() {
    assertFunctionMatches(getJsEnumEquals(), "");
  }
}
