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
  public static native boolean f(double x);

  public static boolean test(double x) {
    return Number.f(x);
  }

  /** Tests for generic native type. */
  @JsType(isNative = true, namespace = GLOBAL, name = "Array")
  private interface NativeArray<T> {
    @JsProperty
    int getLength();

    @Wasm("nop") // j2wasm doesn't support generic return types on native methods.
    T at(int index);
  }

  @JsMethod(name = "Array", namespace = GLOBAL)
  private static native <T> NativeArray<T> createArray();

  @Wasm("nop") // NativeArray#at is marked as nop.
  private static String getStringAt(int index) {
    return Number.<String>createArray().at(index);
  }

  private static int getArrayLength(NativeArray<?> array) {
    return array.getLength();
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  public static interface MyLiteralType {}

  public MyLiteralType testJsDocForLiteralType(MyLiteralType a) {
    return a;
  }
}
