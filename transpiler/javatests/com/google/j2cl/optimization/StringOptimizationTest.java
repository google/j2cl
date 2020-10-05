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
package com.google.j2cl.optimization;

import static com.google.j2cl.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StringOptimizationTest {

  @Before
  public void setUp() {
    // Refer to static fields of String so that $clinit is not empty and does not get pruned.
    String.CASE_INSENSITIVE_ORDER.compare("aaa", "bbb");
  }

  @JsMethod
  public boolean stringEqualsString() {
    return "".equals("");
  }

  @JsProperty
  private native Object getStringEqualsString();

  @JsMethod
  public boolean stringNotEqualsString() {
    return "".equals(new Object());
  }

  @JsProperty
  private native Object getStringNotEqualsString();

  @Test
  public void simpleEqualsOptimizes() {
    assertFunctionMatches(getStringEqualsString(), "return !0;");
    assertFunctionMatches(getStringNotEqualsString(), "return !1;");
  }

  @JsMethod
  public boolean stringSameString() {
    return "" == "";
  }

  @JsProperty
  private native Object getStringSameString();

  @Test
  public void simpleSameOptimizes() {
    assertFunctionMatches(getStringSameString(), "return !0;");
  }

  private static boolean staticField = "asd".equals("asd");

  @JsMethod
  public boolean stringEqualsStringOnStatic() {
    return staticField;
  }

  @JsProperty
  private native Object getStringEqualsStringOnStatic();

  @Test
  public void staticFieldEqualsOptimizes() {
    assertFunctionMatches(getStringEqualsStringOnStatic(), "return !0;");
  }
}
