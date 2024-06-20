/*
 * Copyright 2015 Google Inc.
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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.FieldAccess;
import com.google.j2cl.transpiler.ast.FieldDescriptor;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;

/**
 * Normalizes the static native js members accesses.
 *
 * <p>For example,
 *
 * <pre>
 * class A {
 *   {@literal @}JsMethod(namespace="Math")
 *   static native double abs(double x);
 * }
 * </pre>
 *
 * <p>A.abs() really refers to Javascript built-in Math.abs(). This pass replaces all method calls
 * to A.abs() with Math.abs().
 */
public class NormalizeStaticNativeMemberReferences extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteFieldAccess(FieldAccess fieldAccess) {
            FieldDescriptor fieldDescriptor = fieldAccess.getTarget();
            if (!fieldDescriptor.hasJsNamespace()) {
              return fieldAccess;
            }

            checkArgument(fieldAccess.getQualifier() == null);
            return FieldAccess.Builder.from(createExternFieldDescriptor(fieldDescriptor)).build();
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            if (!methodDescriptor.hasJsNamespace()) {
              return methodCall;
            }

            checkArgument(methodCall.getQualifier() == null);
            return MethodCall.Builder.from(createExternMethodDescriptor(methodDescriptor))
                .setArguments(methodCall.getArguments())
                .build();
          }
        });
  }

  private static FieldDescriptor createExternFieldDescriptor(FieldDescriptor fieldDescriptor) {
    // A.abs -> Math.abs.
    return fieldDescriptor.transform(
        builder ->
            builder.setEnclosingTypeDescriptor(
                AstUtils.getNamespaceAsTypeDescriptor(fieldDescriptor)));
  }

  private static MethodDescriptor createExternMethodDescriptor(MethodDescriptor methodDescriptor) {
    // A.abs() -> Math.abs().
    return methodDescriptor.transform(
        builder ->
            builder.setEnclosingTypeDescriptor(
                AstUtils.getNamespaceAsTypeDescriptor(methodDescriptor)));
  }
}

