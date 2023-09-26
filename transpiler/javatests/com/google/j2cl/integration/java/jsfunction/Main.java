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
package jsfunction;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;

public class Main {
  public static void main(String... args) {
    testJsFunction();
    testSpecializedJsFunction();
    testParameterizedJsFunctionMethod();
  }

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

  private static void testJsFunction() {
    assertTrue(((Function) (() -> true)).fun() == 2);
    assertTrue(((Function) (() -> false)).fun() == 3);
    assertTrue(FunctionWithStaticOverlay.fun() == 4);
    assertTrue(FunctionWithStaticField.f == 1);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testSpecializedJsFunction() {
    Consumer<String> stringConsumer =
        s -> {
          Object unused = s.substring(2);
        };
    Consumer rawConsumer = stringConsumer;

    assertThrowsClassCastException(() -> rawConsumer.accept(new Object()), String.class);
  }

  @JsFunction
  interface Consumer<T> {
    void accept(T t);
  }

  @JsFunction
  interface ParameterizedInterface<T> {
    T f(T t);
  }

  static <T> T identity(T t) {
    return t;
  }

  static <T> T nullFn(T t) {
    return null;
  }

  private static void testParameterizedJsFunctionMethod() {
    class A {
      String m() {
        return "HelloA";
      }
    }

    class B extends A {
      String m() {
        return "HelloB";
      }
    }

    ParameterizedInterface<B> parameterInterfaceFn;
    // Use a random to make the compiler not know which method is actually passed so that
    // whatever optimization is made on the call to B::m is based solely on types.
    if (Math.random() < -1) {
      parameterInterfaceFn = Main::nullFn;
    } else {
      parameterInterfaceFn = Main::identity;
    }
    new A();
    assertEquals("HelloB", parameterInterfaceFn.f(new B()).m());
  }
}
