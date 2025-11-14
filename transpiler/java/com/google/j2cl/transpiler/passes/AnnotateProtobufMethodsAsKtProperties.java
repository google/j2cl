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
import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MethodDescriptor;

/** Makes all instance getters in a proto message or builder @KtProperty. */
// TODO(b/378128677): Remove when proto getters and setters are annotated with @KtProperty in the
//  sources.
public class AnnotateProtobufMethodsAsKtProperties extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MemberDescriptor rewriteMethodDescriptor(MethodDescriptor methodDescriptor) {
            if (!isProtobufKtProperty(methodDescriptor)) {
              return methodDescriptor;
            }

            KtInfo rewrittenKtInfo =
                methodDescriptor.getOriginalKtInfo().toBuilder().setProperty(true).build();

            return methodDescriptor.transform(
                builder -> builder.setOriginalKtInfo(rewrittenKtInfo));
          }
        });
  }

  private boolean isProtobufKtProperty(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isStatic()) {
      return false;
    }

    if (!methodDescriptor.getEnclosingTypeDescriptor().getTypeDeclaration().isProtobuf()) {
      return false;
    }

    if (!methodDescriptor.getParameterDescriptors().isEmpty()) {
      return false;
    }

    String name = methodDescriptor.getName();
    return name.startsWith("get");
  }
}
