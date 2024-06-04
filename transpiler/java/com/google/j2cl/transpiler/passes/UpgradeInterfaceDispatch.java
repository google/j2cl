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
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import javax.annotation.Nullable;

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
            if (!methodCall.isPolymorphic()
                || methodCall.getTarget().isClassDynamicDispatch()
                || methodCall.getTarget().isNative()) {
              // This is not an interface dispatch.
              return methodCall;
            }

            DeclaredTypeDescriptor qualifierTypeDescriptor =
                findConcreteQualifierTypeDescriptor(methodCall);
            if (qualifierTypeDescriptor == null) {
              // Couldn't find a concrete type to upgrade the method call.
              return methodCall;
            }

            MethodDescriptor newMethodDescriptor =
                findTargetMethodDescriptor(qualifierTypeDescriptor, methodCall.getTarget());

            return MethodCall.Builder.from(methodCall).setTarget(newMethodDescriptor).build();
          }
        });
  }

  @Nullable
  private static DeclaredTypeDescriptor findConcreteQualifierTypeDescriptor(MethodCall methodCall) {
    TypeDescriptor qualifierTypeDescriptor = methodCall.getQualifier().getTypeDescriptor();
    if (!(qualifierTypeDescriptor instanceof DeclaredTypeDescriptor)) {
      // In the case of type variables and intersection types.
      // `toRawTypeDescriptor` here returns a DeclaredTypeDescriptor. At this point, arrays have
      // been converted to WasmArray types, and primitives never qualify for dynamic dispatch.
      qualifierTypeDescriptor = qualifierTypeDescriptor.toRawTypeDescriptor();
    }

    if (qualifierTypeDescriptor.isInterface()) {
      if (!methodCall.getTarget().isOrOverridesJavaLangObjectMethod()) {
        // If the qualifier is an interface and the method is not java.lang.Object method, this is
        // an interface dispatch and cannot be upgraded.
        return null;
      }
      // Use java.lang.Object since the target is a java.lang.Object override and
      // all interfaces types extend java.lang.Object.
      return TypeDescriptors.get().javaLangObject;
    }

    return (DeclaredTypeDescriptor) qualifierTypeDescriptor;
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
