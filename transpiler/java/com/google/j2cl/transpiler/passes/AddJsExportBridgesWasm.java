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

import com.google.j2cl.common.SourcePosition;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.WasmExportBridgesUtils;

/**
 * Generates forwarding methods for Wasm JsInterop exported methods. The forwarding methods are then
 * exported (instead of the original methods). The forwarding methods perform polymorphic dispatch
 * and any necessary conversions between Wasm and JS types.
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
        .filter(type -> AstUtils.declaresWasmJsExports(type.getDeclaration()))
        .forEach(
            type -> {
              // Generate bridges for declared methods.
              for (Method method : type.getMethods()) {
                if (!needsBridge(method.getDescriptor())) {
                  continue;
                }

                addBridge(
                    type,
                    WasmExportBridgesUtils.generateBridge(
                        method.getDescriptor(),
                        method.getSourcePosition(),
                        getBridgeOrigin(method.getDescriptor())));
              }

              // Generate bridges for accidental overrides of interface js methods.
              for (MethodDescriptor accidentalOverride :
                  type.getTypeDescriptor().getAccidentalOverrides()) {
                if (!needsBridge(accidentalOverride)) {
                  continue;
                }

                addBridge(
                    type,
                    WasmExportBridgesUtils.generateBridge(
                        accidentalOverride,
                        SourcePosition.NONE,
                        getBridgeOrigin(accidentalOverride)));
              }

              for (Field field : type.getFields()) {
                if (!needsBridge(field.getDescriptor())) {
                  continue;
                }

                addBridge(
                    type,
                    WasmExportBridgesUtils.generateGetterBridge(
                        field.getDescriptor(), field.getSourcePosition()));

                if (!field.isCompileTimeConstant()) {
                  addBridge(
                      type,
                      WasmExportBridgesUtils.generateSetterBridge(
                          field.getDescriptor(), field.getSourcePosition()));
                }
              }
            });
  }

  private static void addBridge(Type type, Method bridge) {
    // TODO(b/545779164): The bridges should be unique and the check should not be needed.
    if (!type.containsMethod(bridge.getDescriptor().getMangledName())) {
      type.addMember(bridge);
    }
  }

  private static boolean needsBridge(MemberDescriptor memberDescriptor) {
    return AstUtils.needsWasmJsExport(memberDescriptor)
        // TODO(b/543878914): Revisit this when refactored.
        // For interfaces, only static members need bridges. Instance members are included with
        // implementations.
        && (!memberDescriptor.getEnclosingTypeDescriptor().isInterface()
            || memberDescriptor.isStatic());
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
