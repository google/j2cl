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
package com.google.j2cl.transpiler.optimization;

import static com.google.j2cl.transpiler.optimization.OptimizationTestUtil.assertFunctionMatches;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StringsTest {

  @JsMethod
  public boolean stringEqualsString() {
    return "".equals("");
  }

  @JsMethod
  public boolean stringNotEqualsString() {
    return "".equals("asd");
  }

  @Test
  public void simpleEqualsOptimizes() {
    assertFunctionMatches(((MethodsAsProperties) this).getStringEqualsString(), "return !0;");
    assertFunctionMatches(((MethodsAsProperties) this).getStringNotEqualsString(), "return !1;");
  }

  @JsMethod
  public boolean stringSameString() {
    return "" == "";
  }

  @Test
  public void simpleSameOptimizes() {
    assertFunctionMatches(((MethodsAsProperties) this).getStringSameString(), "return !0;");
  }

  private static boolean staticField = "asd".equals("asd");

  @JsMethod
  public boolean stringEqualsStringOnStatic() {
    return staticField;
  }

  @Test
  public void staticFieldEqualsOptimizes() {
    assertFunctionMatches(
        ((MethodsAsProperties) this).getStringEqualsStringOnStatic(), "return !0;");
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface MethodsAsProperties {
    @JsProperty
    Object getStringEqualsString();

    @JsProperty
    Object getStringNotEqualsString();

    @JsProperty
    Object getStringSameString();

    @JsProperty
    Object getStringEqualsStringOnStatic();
  }
}
