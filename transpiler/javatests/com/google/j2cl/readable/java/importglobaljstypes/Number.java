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
package importglobaljstypes;

import static jsinterop.annotations.JsPackage.GLOBAL;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Tests explicit import by namespaced JsMethod.
 */
public class Number {
  // import native js "Number" in a java class "Number".
  @JsMethod(name = "Number.isInteger", namespace = GLOBAL)
  public static native boolean fun(double x);

  public static boolean test(double x) {
    return Number.fun(x);
  }

  /**
   * Tests for generic native type.
   */
  @JsType(isNative = true, namespace = GLOBAL, name = "Function")
  private interface NativeFunction<T> {
    // TODO(b/193532287): Enable when arrays are supported in wasm jsinterop. Upon enabling,
    // `thisContext` will also need to be made a wasm extern, because wasm does not support
    // accepting a non-native type in a native method.
    @Wasm("nop")
    T apply(Object thisContext, int[] argsArray);
  }

  @JsProperty(name = "String.fromCharCode", namespace = GLOBAL)
  // TODO(b/193532287): Enable when arrays are supported in wasm jsinterop.
  @Wasm("nop")
  private static native NativeFunction<String> getFromCharCodeFunction();

  // TODO(b/193532287): Enable when arrays are supported in wasm jsinterop.
  @Wasm("nop")
  public static String fromCharCode(int[] array) {
    return getFromCharCodeFunction().apply(null, array);
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  public static interface MyLiteralType {}

  public MyLiteralType testJsDocForLiteralType(MyLiteralType a) {
    return a;
  }
}
