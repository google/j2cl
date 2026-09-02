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

import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.j2cl.transpiler.ast.AstUtils.hasOwnWasmJsPrototype;

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Field;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor.MethodOrigin;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.WasmExportBridgesUtils;
import java.util.ArrayList;
import java.util.List;

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
    if (enableCustomDescriptorsJsInterop) {
      addJsTypeExportBridges(library);
    }

    addJsFunctionExportBridges(library);
  }

  private static void addJsTypeExportBridges(Library library) {
    library
        .streamTypes()
        .filter(t -> hasOwnWasmJsPrototype(t.getDeclaration()))
        .forEach(
            type -> {
              List<Method> bridges = new ArrayList<>();

              // Create bridges for fields, static methods and constructors directly from Member
              // objects in Type to account for the effects of normalization. In particular
              // constructors of inner classes have an extra parameter for the outer class instance
              // that is not present in the type model.
              type.getMembers()
                  .forEach(
                      m -> {
                        switch (m) {
                          case Method method when AstUtils.isExposedToJsViaConstructor(method) ->
                              bridges.add(
                                  WasmExportBridgesUtils.generateBridge(
                                      type.getTypeDescriptor(),
                                      method.getDescriptor(),
                                      method.getSourcePosition(),
                                      getBridgeOrigin(method.getDescriptor())));
                          case Field field
                              when field.getDescriptor().canBeReferencedExternally() -> {
                            bridges.add(
                                WasmExportBridgesUtils.generateGetterBridge(
                                    field.getDescriptor(), field.getSourcePosition()));
                            if (!field.getDescriptor().isFinal()) {
                              bridges.add(
                                  WasmExportBridgesUtils.generateSetterBridge(
                                      field.getDescriptor(), field.getSourcePosition()));
                            }
                          }
                          default -> {}
                        }
                      });

              if (!type.isInterface()) {
                // Only create bridges for newly exposed instance methods.
                type.getTypeDescriptor()
                    .getNewlyExposedInstanceJsMethods()
                    .forEach(
                        methodDescriptor ->
                            bridges.add(
                                WasmExportBridgesUtils.generateBridge(
                                    type.getTypeDescriptor(),
                                    methodDescriptor,
                                    type.getSourcePosition(),
                                    getBridgeOrigin(methodDescriptor))));
              }

              bridges.forEach(bridge -> addBridge(type, bridge));
            });
  }

  private static void addJsFunctionExportBridges(Library library) {
    library
        .streamTypes()
        .filter(Type::isJsFunctionInterface)
        .forEach(
            type -> {
              Method jsFunctionMethod =
                  type.getMethods().stream()
                      .filter(m -> m.getDescriptor().isJsFunction())
                      .collect(onlyElement());
              addBridge(
                  type,
                  WasmExportBridgesUtils.generateJsFunctionBridge(
                      type.getTypeDescriptor(), jsFunctionMethod.getSourcePosition()));
            });
  }

  private static void addBridge(Type type, Method bridge) {
    // TODO(b/545779164): The bridges should be unique and the check should not be needed.
    if (!type.containsMethod(bridge.getDescriptor().getMangledName())) {
      type.addMember(bridge);
    }
  }

  private static MethodOrigin getBridgeOrigin(MethodDescriptor descriptor) {
    return switch (descriptor.getJsInfo().getJsMemberType()) {
      case CONSTRUCTOR -> MethodOrigin.SYNTHETIC_WASM_JS_CONSTRUCTOR_EXPORT;
      case METHOD -> MethodOrigin.SYNTHETIC_WASM_JS_METHOD_EXPORT;
      case GETTER -> MethodOrigin.SYNTHETIC_WASM_JS_GETTER_EXPORT;
      case SETTER -> MethodOrigin.SYNTHETIC_WASM_JS_SETTER_EXPORT;
      default ->
          throw new AssertionError(
              "Unexpected JsMemberType: " + descriptor.getJsInfo().getJsMemberType().name());
    };
  }
}
