/*
 * Copyright 2021 Google Inc.
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
package javaemul.internal;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** A special class that to deal with JavaScript values as externs in Wasm. */
@JsType(isNative = true, name = "*", namespace = JsPackage.GLOBAL)
public class WasmExtern {

  @JsMethod(namespace = "j2wasm.EqualityUtils")
  public static native boolean isSame(WasmExtern left, WasmExtern right);

  @Wasm("extern.internalize")
  public static native <T> T internalize(WasmExtern t);

  @Wasm("extern.externalize")
  public static native WasmExtern externalize(Object t);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Array")
  public static native WasmExtern createArray(int length);

  @JsOverlay
  public static WasmExtern createMultiDimensionalArray(int... dimensions) {
    return createMultiDimensionalArrayRecursive(dimensions, 0);
  }

  @JsOverlay
  private static WasmExtern createMultiDimensionalArrayRecursive(int[] dimensions, int index) {
    int length = dimensions[index];
    if (length == -1) {
      return null;
    }
    WasmExtern array = createArray(length);
    if (index + 1 < dimensions.length) {
      for (int i = 0; i < length; i++) {
        setArrayAt(array, i, createMultiDimensionalArrayRecursive(dimensions, index + 1));
      }
    }
    return array;
  }

  @JsMethod(namespace = "j2wasm.ArrayUtils")
  public static native int getArrayLength(WasmExtern target);

  @JsMethod(namespace = "j2wasm.ArrayUtils")
  public static native WasmExtern getArrayAt(WasmExtern target, int index);

  @JsMethod(namespace = "j2wasm.ArrayUtils")
  public static native void setArrayAt(WasmExtern target, int index, WasmExtern value);

  private WasmExtern() {}
}
