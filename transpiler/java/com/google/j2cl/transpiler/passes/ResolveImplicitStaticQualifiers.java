/*
 * Copyright 2021 Google Inc.
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
import com.google.j2cl.transpiler.ast.JavaScriptConstructorReference;
import com.google.j2cl.transpiler.ast.MemberDescriptor;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.Type;

/**
 * Qualifies all static method calls and field accesses with the corresponding JavaScript
 * constructor reference for uniform handling in the Closure backend.
 */
public class ResolveImplicitStaticQualifiers extends NormalizationPass {

  @Override
  public void applyTo(Type type) {
    type.accept(
        new AbstractRewriter() {
          @Override
          public MemberReference rewriteMemberReference(MemberReference memberReference) {
            MemberDescriptor target = memberReference.getTarget();
            if (target.isStatic() && memberReference.getQualifier() == null) {
              return MemberReference.Builder.from(memberReference)
                  .setQualifier(
                      new JavaScriptConstructorReference(
                          target.getEnclosingTypeDescriptor().getTypeDeclaration()))
                  .build();
            }
            return memberReference;
          }
        });
  }
}
