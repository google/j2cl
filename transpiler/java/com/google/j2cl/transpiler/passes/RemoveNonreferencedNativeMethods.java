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
import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Invocation;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Removes certain private native methods that are not referenced. This avoids import generation for
 * implicit constructors, for example, if they are not intended to be used.
 */
public class RemoveNonreferencedNativeMethods extends NormalizationPass {

  private final Set<MethodDescriptor> referencedPrivateMethods = new HashSet<>();

  @Override
  public final void applyTo(CompilationUnit compilationUnit) {
    collectPrivateNativeMethodReferences(compilationUnit);
    removeNonreferencedPrivateNativeMethods(compilationUnit);
  }

  private void collectPrivateNativeMethodReferences(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitInvocation(Invocation invocation) {
            recordReference(invocation.getTarget());
          }
        });
  }

  public void removeNonreferencedPrivateNativeMethods(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractRewriter() {
          @Override
          @Nullable
          public Method rewriteMethod(Method method) {
            if (isPrivateNativeMethod(method.getDescriptor())
                && !isReferenced(method.getDescriptor())) {
              return null;
            }
            return method;
          }
        });
  }

  private void recordReference(MethodDescriptor targetMember) {
    if (!isPrivateNativeMethod(targetMember)) {
      return;
    }
    referencedPrivateMethods.add(targetMember.getDeclarationDescriptor());
  }

  private boolean isReferenced(MethodDescriptor methodDescriptor) {
    return referencedPrivateMethods.contains(methodDescriptor.getDeclarationDescriptor());
  }

  /** Returns {@code true} if the specified method is effectively private and native. */
  private static boolean isPrivateNativeMethod(MethodDescriptor methodDescriptor) {
    boolean isEffectivelyNative =
        methodDescriptor.isNative()
            // TODO(b/264676817): Consider refactoring to have MethodDescriptor.isNative return
            // true for native constructors, or exposing isNativeConstructor from MethodDescriptor.
            || (methodDescriptor.getEnclosingTypeDescriptor().isNative()
                && methodDescriptor.isConstructor());
    boolean isEffectivelyPrivate =
        // Exclude polymorphic methods because they may be used even if not directly referenced; for
        // example, through interfaces.
        !methodDescriptor.isPolymorphic()
            && (methodDescriptor.getVisibility().isPrivate()
                || methodDescriptor
                    .getEnclosingTypeDescriptor()
                    .getTypeDeclaration()
                    .getVisibility()
                    .isPrivate());
    return isEffectivelyNative && isEffectivelyPrivate;
  }
}
