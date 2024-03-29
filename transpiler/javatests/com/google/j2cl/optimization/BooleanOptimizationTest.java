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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BooleanOptimizationTest {

  @JsMethod
  public boolean simpleComp() {
    return true == true;
  }

  @JsProperty
  private native Object getSimpleComp();

  @Test
  public void simpleCompOptimizes() {
    assertFunctionMatches(getSimpleComp(), "return !0;");
  }

  @JsMethod
  public boolean boxedComp() {
    return Boolean.valueOf(true) == Boolean.valueOf(true);
  }

  @JsProperty
  private native Object getBoxedComp();

  @Test
  public void boxedCompOptimizes() {
    assertFunctionMatches(getBoxedComp(), "return !0;");
  }

  @JsMethod
  public boolean booleanFieldComp() {
    return Boolean.TRUE == Boolean.TRUE;
  }

  @JsProperty
  private native Object getBooleanFieldComp();

  @Test
  public void booleanFieldCompOptimizes() {
    assertFunctionMatches(getBooleanFieldComp(), "return !0;");
  }
}
