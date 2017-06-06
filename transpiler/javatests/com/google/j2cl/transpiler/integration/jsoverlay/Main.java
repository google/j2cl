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
package com.google.j2cl.transpiler.integration.jsoverlay;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "test.foo")
  static class NativeJsTypeWithOverlay {
    @JsOverlay public static final int COMPILE_TIME_CONSTANT = 1;
    @JsOverlay public static Object staticField = new Object();
    public native int m();

    @JsOverlay
    public final int callM() {
      return m();
    }

    @JsOverlay
    public static final int fun(int a, int b) {
      return a > b ? a + b : a * b;
    }

    @JsOverlay
    private static final int bar() {
      return 10;
    }

    @JsOverlay
    private final int foo() {
      return 20;
    }

    @JsOverlay
    private int baz() {
      return 30;
    }
  }

  @JsType(isNative = true, namespace = "test.foo")
  static final class NativeFinalJsTypeWithOverlay {
    public native int e();

    @JsOverlay
    public int buzz() {
      return 42;
    }
  }

  public static void testNativeJsWithOverlay() {
    NativeJsTypeWithOverlay object = new NativeJsTypeWithOverlay();
    assert 6 == object.callM();
    assert 20 == NativeJsTypeWithOverlay.fun(4, 5);
    assert 1 == NativeJsTypeWithOverlay.COMPILE_TIME_CONSTANT;
    assert NativeJsTypeWithOverlay.staticField != null;
    NativeJsTypeWithOverlay.staticField = null;
    assert NativeJsTypeWithOverlay.staticField == null;
    assert 10 == NativeJsTypeWithOverlay.bar();
    assert 20 == object.foo();
    assert 30 == object.baz();
    NativeFinalJsTypeWithOverlay f = new NativeFinalJsTypeWithOverlay();
    assert 36 == f.e();
    assert 42 == f.buzz();
  }

  public static void main(String... args) {
    testNativeJsWithOverlay();
  }
}
