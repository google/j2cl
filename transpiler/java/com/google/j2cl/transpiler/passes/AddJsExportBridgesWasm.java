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
package com.google.j2cl.transpiler.passes;

import static com.google.common.base.Predicates.not;

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.WasmExportBridgesUtils;

/**
 * Generates forwarding methods for Wasm JsInterop exported methods. The forwarding methods are then
 * exported (instead of the original methods). The forwarding methods perform polymorphic dispatch
 * and any necessary conversions between {@code java.lang.String} and Wasm strings.
 */
public class AddJsExportBridgesWasm extends LibraryNormalizationPass {
  private final boolean enableCustomDescriptorsJsInterop;

  public AddJsExportBridgesWasm(boolean enableCustomDescriptorsJsInterop) {
    this.enableCustomDescriptorsJsInterop = enableCustomDescriptorsJsInterop;
  }

  @Override
  public void applyTo(Library library) {
    if (!enableCustomDescriptorsJsInterop) {
      return;
    }

    library
        .streamTypes()
        .filter(not(Type::isInterface))
        .forEach(
            type -> {
              for (Method method : type.getMethods()) {
                if (!AstUtils.needsWasmJsExport(method.getDescriptor())) {
                  continue;
                }

                Method bridge =
                    WasmExportBridgesUtils.generateBridge(
                        method.getDescriptor(),
                        method.getSourcePosition(),
                        getBridgeOrigin(method.getDescriptor()));
                type.addMember(bridge);
              }

              for (Field field : type.getFields()) {
                if (!AstUtils.needsWasmJsExport(field.getDescriptor())) {
                  continue;
                }

                Method getter =
                    WasmExportBridgesUtils.generateGetterBridge(
                        field.getDescriptor(), field.getSourcePosition());
                type.addMember(getter);

                if (!field.isCompileTimeConstant()) {
                  Method setter =
                      WasmExportBridgesUtils.generateSetterBridge(
                          field.getDescriptor(), field.getSourcePosition());
                  type.addMember(setter);
                }
              }
            });
  }

  private static MethodDescriptor.MethodOrigin getBridgeOrigin(MethodDescriptor descriptor) {
    return switch (descriptor.getJsInfo().getJsMemberType()) {
      case CONSTRUCTOR -> MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_CONSTRUCTOR_EXPORT;
      case METHOD -> MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_METHOD_EXPORT;
      case GETTER -> MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_GETTER_EXPORT;
      case SETTER -> MethodDescriptor.MethodOrigin.SYNTHETIC_WASM_JS_SETTER_EXPORT;
      default ->
          throw new AssertionError(
              "Unexpected JsMemberType: " + descriptor.getJsInfo().getJsMemberType().name());
    };
  }
}
