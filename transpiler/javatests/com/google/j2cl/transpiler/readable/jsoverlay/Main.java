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
package com.google.j2cl.transpiler.readable.jsoverlay;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithOverlay {
    public static int nonJsOverlayField;

    @JsOverlay public static final int COMPILE_TIME_CONSTANT = 10;

    @JsOverlay public static Object staticField = new Object();

    public native int m();

    // non JsOverlay static method.
    public static native void n();

    @JsOverlay
    public final int callM() {
      return m();
    }

    @JsOverlay
    public static final int fun() {
      return 1;
    }

    @JsOverlay
    private static final int bar() {
      return 1;
    }

    @JsOverlay
    private final int foo() {
      return 1;
    }

    @JsOverlay
    public static int varargs(int... a) {
      return a[0];
    }

    @JsOverlay
    private int baz() {
      return 1;
    }
  }

  public void test() {
    NativeJsTypeWithOverlay n = new NativeJsTypeWithOverlay();
    n.callM();
    NativeJsTypeWithOverlay.fun();
    NativeJsTypeWithOverlay.n();
    NativeJsTypeWithOverlay.bar();
    n.foo();
    int a =
        NativeJsTypeWithOverlay.COMPILE_TIME_CONSTANT + NativeJsTypeWithOverlay.nonJsOverlayField;
    NativeJsTypeWithOverlay.staticField = null;
    NativeJsTypeWithOverlay.varargs(1, 2, 3);
    n.baz();
  }
}
