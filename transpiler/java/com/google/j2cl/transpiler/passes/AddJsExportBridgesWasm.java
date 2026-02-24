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

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.WasmEntryPointBridgesCreator;
import java.util.HashSet;

/**
 * Generates forwarding methods for Wasm JsInterop exported methods. The forwarding methods are then
 * exported (instead of the original methods). The forwarding methods perform polymorphic dispatch
 * and any necessary conversions between {@code java.lang.String} and Wasm strings.
 */
public class AddJsExportBridgesWasm extends LibraryNormalizationPass {
  @Override
  public void applyTo(Library library) {
    library
        .streamTypes()
        .forEach(
            type -> {
              var seenMethodsByMangledName = new HashSet<String>();
              for (Method method : type.getMethods()) {
                if (!shouldGenerateBridge(method.getDescriptor())) {
                  continue;
                }

                Method bridge =
                    WasmEntryPointBridgesCreator.generateBridge(
                        method.getDescriptor(),
                        method.getSourcePosition(),
                        /* isEntryPoint= */ false);

                // Do not generate duplicate methods.
                // TODO(b/482129587): Reconsider which method to export.
                if (!seenMethodsByMangledName.add(bridge.getDescriptor().getMangledName())) {
                  continue;
                }

                type.addMember(bridge);
              }
            });
  }

  private static boolean shouldGenerateBridge(MethodDescriptor methodDescriptor) {
    return AstUtils.canBeReferencedExternallyWasm(methodDescriptor)
        // We don't expose constructors directly. Instead, the factory method is exported.
        && !methodDescriptor.isConstructor()
        // TODO(b/458472428): Support JsProperty/Getter/Setter.
        && methodDescriptor.isJsMethod();
  }
}
