/*
 * Copyright 2022 Google Inc.
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

import static com.google.common.collect.MoreCollectors.onlyElement;

import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDescriptor;

/**
 * Upgrades the dispatch if the instance is not an interface type. This may happen if the type at
 * the call-site is an abstract class or if the implementation is a default method.
 */
public final class UpgradeInterfaceDispatch extends NormalizationPass {

  @Override
  public void applyTo(Type type) {

    type.accept(
        new AbstractRewriter() {
          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.isPolymorphic() || methodCall.getTarget().isClassDynamicDispatch()) {
              // This is not an interface dispatch.
              return methodCall;
            }

            TypeDescriptor typeDescriptor = methodCall.getQualifier().getTypeDescriptor();

            if (typeDescriptor.isTypeVariable()) {
              typeDescriptor = typeDescriptor.toRawTypeDescriptor();
            }

            if (!typeDescriptor.isClass()) {
              // If we haven't inferred a class type, then we cannot upgrade the call.
              return methodCall;
            }

            DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
            MethodDescriptor newMethodDescriptor =
                findTargetMethodDescriptor(declaredTypeDescriptor, methodCall.getTarget());

            return MethodCall.Builder.from(methodCall).setTarget(newMethodDescriptor).build();
          }
        });
  }

  /** Creates a MethodDescriptor to target a polymorphic method on a particular class. */
  private static MethodDescriptor findTargetMethodDescriptor(
      DeclaredTypeDescriptor typeDescriptor, MethodDescriptor descriptor) {
    MethodDescriptor targetMethod =
        typeDescriptor.getPolymorphicMethods().stream()
            .filter(m -> m.getMangledName().equals(descriptor.getMangledName()))
            .collect(onlyElement());
    if (targetMethod.getEnclosingTypeDescriptor().isInterface()) {
      // If the new target method is an interface method, this means the qualifier is an abstract
      // class that does not implement the method. In this case, synthesize a method descriptor
      // which will become a vtable dispatch.
      return MethodDescriptor.Builder.from(descriptor.getDeclarationDescriptor())
          .makeDeclaration()
          .setDefaultMethod(false)
          .setAbstract(typeDescriptor.getTypeDeclaration().isAbstract())
          .setEnclosingTypeDescriptor(typeDescriptor.toNullable())
          .build();
    }
    return targetMethod;
  }
}
