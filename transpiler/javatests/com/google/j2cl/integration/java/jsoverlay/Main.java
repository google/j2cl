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
package jsoverlay;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

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

    @JsOverlay
    private Object getStaticField() {
      return staticField;
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

  @JsType(isNative = true, namespace = "test.foo", name = "NativeJsTypeWithOverlay")
  static final class NativesTypeWithOnlyPrivateOverlay<T> {
    @JsOverlay private static Integer field = new Integer(42);

    @JsOverlay
    private static int staticMethod() {
      return field;
    }
  }

  public static void testNativeJsWithOverlay() {
    NativeJsTypeWithOverlay object = new NativeJsTypeWithOverlay();
    assertTrue(6 == object.callM());
    assertTrue(20 == NativeJsTypeWithOverlay.fun(4, 5));
    assertTrue(1 == NativeJsTypeWithOverlay.COMPILE_TIME_CONSTANT);
    assertTrue(object.getStaticField() != null);
    assertTrue(NativeJsTypeWithOverlay.staticField != null);
    NativeJsTypeWithOverlay.staticField = null;
    assertTrue(NativeJsTypeWithOverlay.staticField == null);
    assertTrue(10 == NativeJsTypeWithOverlay.bar());
    assertTrue(20 == object.foo());
    assertTrue(30 == object.baz());
    NativeFinalJsTypeWithOverlay f = new NativeFinalJsTypeWithOverlay();
    assertTrue(36 == f.e());
    assertTrue(42 == f.buzz());
    assertEquals(42, NativesTypeWithOnlyPrivateOverlay.staticMethod());
  }

  public static void main(String... args) {
    testNativeJsWithOverlay();
  }
}
