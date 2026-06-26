/*
 * Copyright 2023 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.transpiler.ast.AstUtils.isAnnotatedWithWasm;

import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.WasmExportBridgesUtils;

/**
 * Inserts conversions from {@code java.lang.String} arguments to Wasm strings when calling native
 * JS methods, and conversions from Wasm strings when getting returned values.
 */
public class InsertExternConversionsWasm extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Method rewriteMethod(Method method) {
            MethodDescriptor descriptor = method.getDescriptor();
            MethodDescriptor newDescriptor = createExportedMethodDescriptor(descriptor);
            if (descriptor != newDescriptor) {
              method
                  .getParameters()
                  .forEach(
                      p ->
                          p.setTypeDescriptor(
                              WasmExportBridgesUtils.getExternalType(
                                  p.getTypeDescriptor(), /* isExport= */ false)));
              method = method.toBuilder().setMethodDescriptor(newDescriptor).build();
            }
            return method;
          }

          @Override
          public Expression rewriteInvocation(Invocation invocation) {
            if (!isJavaScriptMethod(invocation.getTarget())) {
              return invocation;
            }
            return insertWasmExternConversions(invocation);
          }
        });
  }

  private static Expression insertWasmExternConversions(Invocation invocation) {
    MethodDescriptor methodDescriptor = invocation.getTarget().getDeclarationDescriptor();
    Invocation newInvocation =
        invocation.toBuilder()
            .setArguments(
                Streams.zip(
                        invocation.getArguments().stream(),
                        methodDescriptor.getParameterTypeDescriptors().stream(),
                        (expression, typeDescriptor) ->
                            WasmExportBridgesUtils.convertToExternal(
                                expression, typeDescriptor, /* isExport= */ false))
                    .collect(toImmutableList()))
            .setTarget(createExportedMethodDescriptor(methodDescriptor))
            .build();
    return WasmExportBridgesUtils.convertToInternal(
        newInvocation, methodDescriptor.getReturnTypeDescriptor(), /* isExport= */ false);
  }

  private static MethodDescriptor createExportedMethodDescriptor(MethodDescriptor descriptor) {
    if (!isJavaScriptMethod(descriptor)) {
      return descriptor;
    }
    return descriptor.transform(
        builder ->
            builder
                .setReturnTypeDescriptor(
                    WasmExportBridgesUtils.getExternalType(
                        builder.getReturnTypeDescriptor(), /* isExport= */ false))
                .setParameterDescriptors(
                    descriptor.getParameterDescriptors().stream()
                        .map(
                            pd ->
                                pd.toBuilder()
                                    .setTypeDescriptor(
                                        WasmExportBridgesUtils.getExternalType(
                                            pd.getTypeDescriptor(), /* isExport= */ false))
                                    .setVarargs(false)
                                    .build())
                        .collect(toImmutableList())));
  }

  private static boolean isJavaScriptMethod(MethodDescriptor descriptor) {
    if (isAnnotatedWithWasm(descriptor)) {
      return false;
    }
    return descriptor.isNative()
        // TODO(b/264676817): Consider refactoring to have MethodDescriptor.isNative return true for
        // native constructors, or exposing isNativeConstructor from MethodDescriptor.
        || (descriptor.isConstructor() && descriptor.getEnclosingTypeDescriptor().isNative());
  }
}
