/*
 * Copyright 2026 Google Inc.
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
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class NullEqualityOptimizationTest {

  @JsMethod
  public boolean compareDouble(Double value) {
    return value == null;
  }

  @JsProperty
  private native Object getCompareDouble();

  @Test
  public void doubleComparisonOptimizes() {
    assertFunctionMatches(getCompareDouble(), "return value==null;");
  }

  @JsMethod
  public boolean compareObject(Object value) {
    return value == null;
  }

  @JsProperty
  private native Object getCompareObject();

  @Test
  public void objectComparisonOptimizes() {
    assertFunctionMatches(getCompareObject(), "return value==null;");
  }

  @JsMethod
  public boolean compareString(String value) {
    return value == null;
  }

  @JsProperty
  private native Object getCompareString();

  @Test
  public void stringComparisonOptimizes() {
    assertFunctionMatches(getCompareString(), "return value==null;");
  }

  @JsMethod
  public boolean compareInteger(Integer value) {
    return value == null;
  }

  @JsProperty
  private native Object getCompareInteger();

  @Test
  public void integerComparisonOptimizes() {
    assertFunctionMatches(getCompareInteger(), "return !value;");
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "gbigint")
  private static final class GBigInt {}

  @JsMethod
  public boolean compareGBigInt(GBigInt value) {
    return value == null;
  }

  @JsProperty
  private native Object getCompareGBigInt();

  @Test
  public void gbigintComparisonOptimizes() {
    // TODO(b/485937103): gbigint is a hidden primitive and not correctly handled at the moment.
    assertFunctionMatches(getCompareGBigInt(), "return !value;");
  }
}
