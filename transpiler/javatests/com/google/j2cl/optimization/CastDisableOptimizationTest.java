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

import java.util.Random;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests cast checks are optimized out when cast checking is disabled. */
@RunWith(JUnit4.class)
public class CastDisableOptimizationTest {

  private static class TestObject {}

  private interface TestInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private static class NativeObject {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Function")
  private static class NativeFunction {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  private static class NativeArray {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Number")
  private static class NativeNumber {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private static class NativeString {}

  private static Object field = createField();

  private static int randomNumber = new Random().nextInt(42);

  private static Object createField() {
    // Makes sure that field type is not upgradable even the compiler becomes really smart and also
    // no types are pruned otherwise casts can be statically evaluated.
    switch (randomNumber) {
      case 0:
        return new TestObject();
      case 2:
        return new TestInterface() {};
      case 3:
        return "Some string";
      default:
        // We always hit this case but compiler is not smart enough to know that.
        return new Object();
    }
  }

  public TestObject castOp() {
    return ((TestObject) field);
  }

  private TestInterface castOpInterface() {
    return ((TestInterface) field);
  }

  private String castOpString() {
    return ((String) field);
  }

  private NativeFunction castNativeFunction() {
    return ((NativeFunction) field);
  }

  private NativeObject castNativeObject() {
    return ((NativeObject) field);
  }

  private NativeArray castNativeArray() {
    return ((NativeArray) field);
  }

  private NativeNumber castNativeNumber() {
    return ((NativeNumber) field);
  }

  @JsMethod
  private void failingCasts() {
    // All casts here are chosen to be failing casts to be extra sure that the compiler doesn't
    // optimize them out by somehow proving the cast is valid.
    castOp();
    castOpInterface();
    castOpString();
    castNativeFunction();
    castNativeNumber();
    castNativeArray();
    castNativeObject();
  }

  @JsProperty
  private native Object getFailingCasts();

  @Test
  public void castsAreRemoved() throws Exception {
    assertFunctionMatches(getFailingCasts(), "");
  }
}
