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
import static org.junit.Assert.assertFalse;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IntegerOptimizationTest {

  @Before
  public void setUp() {
    // Refer to static fields of Integer so that $clinit is not empty and does not get pruned.
    assertFalse(Integer.TYPE.getName().isEmpty());
  }

  @JsMethod
  public void unusedValueOf() {
    var unused = Integer.valueOf(1);
  }

  @JsProperty
  private native Object getUnusedValueOf();

  @Test
  public void unusedValueOfOptimizes() {
    assertFunctionMatches(getUnusedValueOf(), "");
  }
}
