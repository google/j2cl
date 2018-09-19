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
package com.google.j2cl.transpiler.integration.jsfunction;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;

public class Main {
  @JsFunction
  interface Function {
    boolean invoke();

    @JsOverlay int f = 1;

    @JsOverlay
    default int fun() {
      return f + (invoke() ? 1 : 2);
    }
  }

  @JsFunction
  interface FunctionWithStaticOverlay {
    boolean invoke();

    @JsOverlay
    static int fun() {
      return 4;
    }
  }

  @JsFunction
  interface FunctionWithStaticField {
    boolean invoke();

    @JsOverlay int f = 1;
  }

  public static void main(String... args) {
    test();
  }

  public static void test() {
    assert ((Function) (() -> true)).fun() == 2;
    assert ((Function) (() -> false)).fun() == 3;
    assert FunctionWithStaticOverlay.fun() == 4;
    assert FunctionWithStaticField.f == 1;
  }
}
