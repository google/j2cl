/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.AbstractVisitor;
import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/** Fixes mismatch between Java and Kotlin method overrides. */
public class FixJavaKotlinMethodOverrideMismatch extends NormalizationPass {

  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(
        new AbstractVisitor() {
          @Override
          public void exitMethod(Method method) {
            if (directlyOverridesJavaObjectClone(method.getDescriptor())) {
              // In Java clone() is magical: it is not declared in Cloneable but declared in Object
              // and only really implemented if the interface Cloneable is implemented. In Kotlin it
              // is explicitly declared in the Cloneable interface but not in Any.
              method.setForcedJavaOverride(false);
            } else if (directlyOverridesJavaUtilCollectionToArray(method.getDescriptor())) {
              // In Kotlin JVM, (Mutable)Collection#toArray are not visible, so overrides cannot
              // have the override modifier. On Kotlin Native, the emulated method is a regular
              // method and overrides need the modifier. Mark the method for special handling in
              // the renderer.
              method.setHasSuppressNothingToOverrideAnnotation(true);
            }
          }
        });
  }

  private static boolean directlyOverridesJavaObjectClone(MethodDescriptor methodDescriptor) {
    return methodDescriptor.getSignature().equals("clone()")
        && !methodDescriptor
            .getEnclosingTypeDescriptor()
            .isSubtypeOf(TypeDescriptors.get().javaLangCloneable)
        && methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
            .allMatch(
                it ->
                    it.getDeclarationDescriptor().isMemberOf(TypeDescriptors.get().javaLangObject));
  }

  private static boolean directlyOverridesJavaUtilCollectionToArray(
      MethodDescriptor methodDescriptor) {
    return methodDescriptor.getName().equals("toArray")
        && methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
            .allMatch(
                it ->
                    it.getDeclarationDescriptor()
                        .isMemberOf(TypeDescriptors.get().javaUtilCollection));
  }
}
