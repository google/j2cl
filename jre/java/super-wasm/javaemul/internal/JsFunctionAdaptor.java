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
package javaemul.internal;

import jsinterop.annotations.JsMethod;

/** A common adapter base class for all Wasm JsFunction types. */
public class JsFunctionAdaptor {
  public WasmExtern jsFuncref;
  public WasmFuncref wasmFuncref;
  public WasmFuncref wasmExportBridgeFuncref;

  public JsFunctionAdaptor(WasmExtern jsFuncref) {
    this.jsFuncref = jsFuncref;
    this.wasmFuncref = null;
  }

  public JsFunctionAdaptor(WasmFuncref wasmFuncref, WasmFuncref wasmExportBridgeFuncref) {
    this.jsFuncref = null;
    this.wasmFuncref = wasmFuncref;
    this.wasmExportBridgeFuncref = wasmExportBridgeFuncref;
  }

  /** Converts this JsFunction to a JavaScript function reference. */
  public static WasmExtern toJs(JsFunctionAdaptor adaptor) {
    if (adaptor.jsFuncref == null) {
      adaptor.jsFuncref =
          bindJsFunction(adaptor.wasmExportBridgeFuncref, WasmExtern.convertToExtern(adaptor));
    }
    return adaptor.jsFuncref;
  }

  @JsMethod(namespace = "j2wasm.JsInteropRuntime", name = "bindJsFunction")
  private static native WasmExtern bindJsFunction(WasmFuncref wasmFuncref, WasmExtern adaptor);
}
