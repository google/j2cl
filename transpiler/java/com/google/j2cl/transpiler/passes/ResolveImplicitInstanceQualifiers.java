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
import com.google.j2cl.transpiler.ast.ExpressionStatement;
import com.google.j2cl.transpiler.ast.Member;
import com.google.j2cl.transpiler.ast.MemberReference;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.NewInstance;
import com.google.j2cl.transpiler.ast.Node;
import java.util.function.Predicate;

/** Resolves implicit qualifiers for instance members and constructors. */
public class ResolveImplicitInstanceQualifiers extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {

    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          public MemberReference rewriteMemberReference(MemberReference memberReference) {
            return AstUtils.resolveImplicitQualifier(
                memberReference, getCurrentType().getTypeDescriptor());
          }

          @Override
          public Node rewriteNewInstance(NewInstance newInstance) {
            // Anonymous classes are considered to capture the enclosing instance unless they
            // appear in a static method. But when declared inside a constructor they might only
            // capture the enclosing instance only if they don't appear before the super/this
            // constructor call invocation.
            // To workaround that we continue to construct anonymous inner classes as if they
            // capture the enclosing instance, but pass `null` as the instance if the instantiation
            // happens before the super/this constructor call.

            Member currentMember = getCurrentMember();
            if (newInstance
                    .getTarget()
                    .getEnclosingTypeDescriptor()
                    .getTypeDeclaration()
                    .isAnonymous()
                && newInstance.getQualifier() == null
                && currentMember.isConstructor()) {
              Method method = (Method) currentMember;
              ExpressionStatement constructorInvocationStatement =
                  AstUtils.getConstructorInvocationStatement(method);

              // Special treat anonymous class creation when declared in a parameter of a
              // super/this constructor invocation.
              boolean inSuperOrThisCall =
                  getParent(Predicate.isEqual(constructorInvocationStatement)) != null;
              if (inSuperOrThisCall
                  // Restrict the handling to JsConstructor super/this calls. These are the only
                  // cases where the generated code is not correct for JS.
                  && AstUtils.getConstructorInvocation(method).getTarget().isJsConstructor()) {

                return NewInstance.Builder.from(newInstance)
                    .setQualifier(
                        currentMember.getDescriptor().getEnclosingTypeDescriptor().getNullValue())
                    .build();
              }
            }
            return rewriteMemberReference(newInstance);
          }
        });
  }
}
