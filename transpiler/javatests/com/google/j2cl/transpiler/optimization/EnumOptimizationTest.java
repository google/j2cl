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
package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
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
}
