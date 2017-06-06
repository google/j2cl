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
package com.google.j2cl.transpiler.integration.buildtestexterns;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  static class FooImpl {
    @JsProperty public final String foo;

    public FooImpl(String value) {
      this.foo = value;
    }
  }

  // Is overlaying extern "Foo", not Java class FooImpl. The extern Foo is just a @typedef that
  // declares a string field named "foo".
  @JsType(isNative = true, name = "Foo", namespace = JsPackage.GLOBAL)
  interface FooOverlay {
    /** Returns the foo value. */
    @JsProperty
    String getFoo();
  }

  private static void testFooOverlay(FooOverlay fooOverlay) {
    assert fooOverlay.getFoo().equals("Hello");
  }

  public static void main(String... args) {
    testFooOverlay((FooOverlay) (Object) new FooImpl("Hello"));
  }
}
