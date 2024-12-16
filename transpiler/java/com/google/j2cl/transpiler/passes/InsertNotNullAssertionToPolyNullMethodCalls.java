/*
 * Copyright 2024 Google Inc.
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

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Node;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Inserts not-null assertion to method calls which are known to be @PolyNull, where the argument
 * can not be null.
 */
public class InsertNotNullAssertionToPolyNullMethodCalls extends NormalizationPass {
  private final MethodDescriptor javaUtilOptionalOrElse =
      TypeDescriptors.get()
          .javaUtilOptional
          .getMethodDescriptor("orElse", TypeDescriptors.get().javaLangObject);

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public Node rewriteMethodCall(MethodCall methodCall) {
            MethodDescriptor methodDescriptor = methodCall.getTarget();
            if (isOrOverrides(methodDescriptor, javaUtilOptionalOrElse)) {
              if (!methodCall.getArguments().get(0).canBeNull()) {
                return methodCall.postfixNotNullAssertion();
              }
            }
            return methodCall;
          }
        });
  }

  private static boolean isOrOverrides(
      MethodDescriptor methodDescriptor, MethodDescriptor otherMethodDescriptor) {
    return methodDescriptor.getDeclarationDescriptor().equals(otherMethodDescriptor)
        || methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
            .anyMatch(it -> it.getDeclarationDescriptor().equals(otherMethodDescriptor));
  }
}
