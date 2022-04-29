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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.j2cl.transpiler.ast.AbstractRewriter;
import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Library;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeVariable;
import java.util.Set;

/**
 * Upgrades the dispatch if the instance is not an interface type. This may happen if the type at
 * the call-site is an abstract class or if the implementation is a default method.
 */
public final class UpgradeInterfaceDispatch extends LibraryNormalizationPass {

  @Override
  public void applyTo(Library library) {

    SetMultimap<TypeDeclaration, MethodDescriptor> newMethods = LinkedHashMultimap.create();

    library.accept(
        new AbstractRewriter() {
          @Override
          public MethodCall rewriteMethodCall(MethodCall methodCall) {
            if (!methodCall.isPolymorphic() || methodCall.getTarget().isClassDynamicDispatch()) {
              // This is not an interface dispatch.
              return methodCall;
            }

            TypeDescriptor typeDescriptor = methodCall.getQualifier().getTypeDescriptor();

            if (typeDescriptor.isTypeVariable()) {
              typeDescriptor = ((TypeVariable) typeDescriptor).toRawTypeDescriptor();
            }

            if (!typeDescriptor.isClass()) {
              // If we haven't inferred a class type, then we cannot upgrade the call.
              return methodCall;
            }

            DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;
            MethodDescriptor newMethodDescriptor =
                createStubMethodDescriptor(declaredTypeDescriptor, methodCall.getTarget());

            newMethods.put(declaredTypeDescriptor.getTypeDeclaration(), newMethodDescriptor);

            return MethodCall.Builder.from(methodCall).setTarget(newMethodDescriptor).build();
          }
        });

    library.streamTypes().forEach(t -> addStubMethods(t, newMethods.get(t.getDeclaration())));
  }

  private static MethodDescriptor createStubMethodDescriptor(
      DeclaredTypeDescriptor typeDescriptor, MethodDescriptor descriptor) {
    return MethodDescriptor.Builder.from(descriptor.getDeclarationDescriptor())
        .setDeclarationDescriptor(null)
        .setDefaultMethod(false)
        .setAbstract(true)
        .setEnclosingTypeDescriptor(typeDescriptor.toNullable())
        .build();
  }

  private static void addStubMethods(Type type, Set<MethodDescriptor> newMethodDescriptors) {
    if (newMethodDescriptors.isEmpty()) {
      return;
    }

    // Some interface dispatches already have targets method due to bridges that are added later.
    ImmutableSet<String> bridgeMethodsMangledNames =
        type.getTypeDescriptor().getPolymorphicMethods().stream()
            .filter(m -> m.isBridge())
            .map(MethodDescriptor::getMangledName)
            .collect(toImmutableSet());

    Set<String> existingMangledNames = Sets.newHashSet(bridgeMethodsMangledNames);

    newMethodDescriptors.stream()
        .filter(m -> existingMangledNames.add(m.getMangledName()))
        .forEach(
            m ->
                type.addMember(
                    Method.newBuilder()
                        .setMethodDescriptor(m)
                        .setParameters(
                            AstUtils.createParameterVariables(m.getParameterTypeDescriptors()))
                        .setSourcePosition(type.getSourcePosition())
                        .build()));
  }
}
