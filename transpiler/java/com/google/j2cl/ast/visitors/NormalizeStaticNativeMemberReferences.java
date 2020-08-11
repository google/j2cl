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
package com.google.j2cl.ast.visitors;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.JavaScriptConstructorReference;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Node;

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
            if (!fieldDescriptor.isExternalizedMember()) {
              return fieldAccess;
            }

            checkArgument(fieldAccess.getQualifier() instanceof JavaScriptConstructorReference);
            return FieldAccess.Builder.from(rewriteFieldDescriptor(fieldDescriptor)).build();
          }

          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            if (!methodDescriptor.isExternalizedMember()) {
              return methodCall;
            }

            checkArgument(methodCall.getQualifier() instanceof JavaScriptConstructorReference);
            return MethodCall.Builder.from(rewriteMethodDescriptor(methodDescriptor))
                .setArguments(methodCall.getArguments())
                .build();
          }
        });
  }

  private static FieldDescriptor rewriteFieldDescriptor(FieldDescriptor fieldDescriptor) {
    // A.abs -> Math.abs.
    return FieldDescriptor.Builder.from(fieldDescriptor)
        .setEnclosingTypeDescriptor(AstUtils.getNamespaceAsTypeDescriptor(fieldDescriptor))
        .setDeclarationDescriptor(
            fieldDescriptor.isDeclaration()
                ? null
                : rewriteFieldDescriptor(fieldDescriptor.getDeclarationDescriptor()))
        .build();
  }

  private static MethodDescriptor rewriteMethodDescriptor(MethodDescriptor methodDescriptor) {
    // A.abs() -> Math.abs().
    return MethodDescriptor.Builder.from(methodDescriptor)
        .setEnclosingTypeDescriptor(AstUtils.getNamespaceAsTypeDescriptor(methodDescriptor))
        .setDeclarationDescriptor(
            methodDescriptor.isDeclaration()
                ? null
                : rewriteMethodDescriptor(methodDescriptor.getDeclarationDescriptor()))
        .build();
  }
}

