/*
 * Copyright 2026 Google Inc.
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

package nativejstypes;

import jsinterop.annotations.JsType;

public class NativeOverNonNative {
  @JsType(namespace = "nativetypes")
  static class NonNative {
    public NonNative(int x) {}

    public void m() {}

    public static void n() {}
  }

  @JsType(isNative = true, namespace = "nativetypes", name = "NonNative")
  static class ExposesNonNative {
    public ExposesNonNative(int x) {}

    public native void m();

    public static native void n();
  }

  void main() {
    ExposesNonNative exposesNonNative = new ExposesNonNative(1);
    exposesNonNative.m();
    ExposesNonNative.n();
  }
}
