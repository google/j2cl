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

import jsinterop.annotations.JsType;

/**
 * Tests import global native js types.
 *
 * <p>In generated Date.impl.js, the NativeDate is considered an extern and thus not goog.required
 */
public class Date {
  public static double now(double x) {
    return NativeDate.now();
  }

  /** Tests for type annotation for native types. */
  public NativeDate copy(NativeDate d) {
    return d;
  }

  @JsType(isNative = true, name = "Date", namespace = GLOBAL)
  public static class NativeDate {
    // Test a static method.
    public static native double now();

    // Test an instance method.
    public native int getSeconds();
  }
}
