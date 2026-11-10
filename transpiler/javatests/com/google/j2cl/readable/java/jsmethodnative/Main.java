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
package jsmethodnative;

import jsinterop.annotations.JsType;

/**
 * Tests native methods taking a JsType declared in the application.
 *
 * <p>In wasm these generate Js glue code (i.e. the imports) where the types of referenced by these
 * methods must be correct even when the JsType is not exported to JavaScript, i.e. without custom
 * descriptors, where its module is not provided and must not be required.
 */
public class Main {
  // Non-native JsTypes need to be treated as opaque references when wasm jsinterop is disabled, and
  // as regular JsTypes when wasm jsinterop is enabled.
  @JsType
  public static class SomeJsType {}

  // Defined in native_sources/NativeClass.js.
  @JsType(isNative = true, namespace = "jsmethodnative", name = "NativeClass")
  static class NativeClass {
    // Non-native JsTypes can only be passed to native methods with custom descriptors.
    @SuppressWarnings("unusable-by-js")
    public static native void staticMethodReferencingNonNativeJsType(SomeJsType o);

    @SuppressWarnings("unusable-by-js")
    public native void instanceMethodReferencingNonNativeJsType(SomeJsType o);
  }

  public static void test() {
    SomeJsType o = new SomeJsType();
    NativeClass.staticMethodReferencingNonNativeJsType(o);
    new NativeClass().instanceMethodReferencingNonNativeJsType(o);
  }
}
