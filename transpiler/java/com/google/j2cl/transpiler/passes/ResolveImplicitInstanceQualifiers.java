/*
 * Copyright 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MemberReference;

/** Resolves implicit qualifiers for instance members and constructors. */
public class ResolveImplicitInstanceQualifiers extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MemberReference rewriteMemberReference(MemberReference memberReference) {
            if (!needsImplicitQualifierResolution(memberReference)) {
              return memberReference;
            }
            MemberDescriptor memberDescriptor = memberReference.getTarget();
            DeclaredTypeDescriptor targetType = memberDescriptor.getEnclosingTypeDescriptor();

            // The target type for this/super constructor calls is the enclosing instance.
            targetType =
                memberDescriptor.isConstructor()
                    ? targetType.getEnclosingTypeDescriptor()
                    : targetType;

            return MemberReference.Builder.from(memberReference)
                .setQualifier(
                    AstUtils.resolveImplicitQualifier(
                        getCurrentType().getTypeDescriptor(), targetType))
                .build();
          }
        });
  }

  private boolean needsImplicitQualifierResolution(MemberReference memberReference) {
    if (memberReference.getQualifier() != null) {
      return false;
    }
    MemberDescriptor memberDescriptor = memberReference.getTarget();
    DeclaredTypeDescriptor targetType = memberDescriptor.getEnclosingTypeDescriptor();
    return memberDescriptor.isInstanceMember()
        || (memberDescriptor.isConstructor()
            && targetType.getTypeDeclaration().isCapturingEnclosingInstance());
  }
}
