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

import com.google.common.collect.Streams;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.RuntimeMethods;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

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
                              replaceStringWithNativeString(p.getTypeDescriptor())));
              method = Method.Builder.from(method).setMethodDescriptor(newDescriptor).build();
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
        Invocation.Builder.from(invocation)
            .setArguments(
                Streams.zip(
                        methodDescriptor.getParameterTypeDescriptors().stream(),
                        invocation.getArguments().stream(),
                        (paramType, arg) ->
                            TypeDescriptors.isJavaLangString(paramType)
                                ? RuntimeMethods.createJsStringFromStringMethodCall(arg)
                                : arg)
                    .collect(toImmutableList()))
            .setTarget(createExportedMethodDescriptor(methodDescriptor))
            .build();
    return TypeDescriptors.isJavaLangString(methodDescriptor.getReturnTypeDescriptor())
        ? RuntimeMethods.createStringFromJsStringMethodCall(newInvocation)
        : newInvocation;
  }

  private static MethodDescriptor createExportedMethodDescriptor(MethodDescriptor descriptor) {
    if (!isJavaScriptMethod(descriptor)) {
      return descriptor;
    }
    return descriptor.transform(
        builder ->
            builder
                .setReturnTypeDescriptor(
                    replaceStringWithNativeString(builder.getReturnTypeDescriptor()))
                .updateParameterTypeDescriptors(
                    builder.getParameterTypeDescriptors().stream()
                        .map(x -> replaceStringWithNativeString(x))
                        .collect(toImmutableList())));
  }

  private static TypeDescriptor replaceStringWithNativeString(TypeDescriptor typeDescriptor) {
    if (TypeDescriptors.isJavaLangString(typeDescriptor)) {
      return TypeDescriptors.getNativeStringType().toNullable(typeDescriptor.isNullable());
    }
    return typeDescriptor;
  }

  private static boolean isJavaScriptMethod(MethodDescriptor descriptor) {
    if (descriptor.getWasmInfo() != null) {
      return false;
    }
    return descriptor.isNative()
        // TODO(b/264676817): Consider refactoring to have MethodDescriptor.isNative return true for
        // native constructors, or exposing isNativeConstructor from MethodDescriptor.
        || (descriptor.isConstructor() && descriptor.getEnclosingTypeDescriptor().isNative());
  }
}
