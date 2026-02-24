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
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Tests J2WASM custom descriptor jsinterop features. */
public final class Main {
  public static void main(String... args) throws Exception {
    testConstructor();
    testMethod();
    testInheritedMethod();
  }

  private static void testConstructor() {
    BaseJsType baseJsType = internalize(newBaseJsType());

    SomeJsType someJsType = internalize(newSomeJsType(123));
    assertTrue(someJsType.value == 123);
  }

  private static void testMethod() {
    SomeJsType someJsType = new SomeJsType(123);
    assertTrue(callGetNumber(externalize(someJsType)) == 11);
    assertTrue(callGetString(externalize(someJsType)).equals("str"));
  }

  private static void testInheritedMethod() {
    SubJsType subJsType = new SubJsType();
    assertTrue(subJsType.value == 12);
    assertTrue(callGetNumber(externalize(subJsType)) == 22);
    assertTrue(callGetString(externalize(subJsType)).equals("str"));
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class BaseJsType {
    @JsConstructor
    public BaseJsType() {}
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class SomeJsType extends BaseJsType {
    public int value;

    public SomeJsType(int value) {
      this.value = value;
    }

    public int getNumber() {
      return 11;
    }

    public String getString() {
      return "str";
    }
  }

  @JsType(namespace = "wasmcustomdescriptorsjsinterop")
  static class SubJsType extends SomeJsType {
    public SubJsType() {
      super(12);
    }

    @Override
    public int getNumber() {
      return 22;
    }
  }

  @JsMethod(namespace = "nativehelper")
  static native WasmExtern newBaseJsType();

  @JsMethod(namespace = "nativehelper")
  static native WasmExtern newSomeJsType(int value);

  @JsMethod(namespace = "nativehelper")
  static native int callGetNumber(WasmExtern someJsType);

  @JsMethod(namespace = "nativehelper")
  static native String callGetString(WasmExtern someJsType);

  @Wasm("extern.internalize")
  public static native <T> T internalize(WasmExtern t);

  @Wasm("extern.externalize")
  public static native WasmExtern externalize(Object t);

  // TODO(b/479214877): Remove this externref when we can pass exported types from JS to Wasm or we
  // implement the necessary wrappers.
  @JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL)
  interface WasmExtern {}
}
