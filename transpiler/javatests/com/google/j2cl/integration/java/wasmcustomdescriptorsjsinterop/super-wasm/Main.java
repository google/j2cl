/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package wasmcustomdescriptorsjsinterop;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/** Tests J2WASM custom descriptor jsinterop features. */
public final class Main {
  public static void main(String... args) throws Exception {
    SomeJsType someJsType = internalize(newSomeJsType(123));

    assertTrue(someJsType.value == 123);
  }

  @JsMethod(namespace = "nativehelper")
  static native WasmExtern newSomeJsType(int value);

  @Wasm("extern.internalize")
  public static native <T> T internalize(WasmExtern t);

  // TODO(b/479214877): Remove this externref when we can pass exported types from JS to Wasm or we
  // implement the necessary wrappers.
  @JsType(isNative = true)
  interface WasmExtern {}
}
