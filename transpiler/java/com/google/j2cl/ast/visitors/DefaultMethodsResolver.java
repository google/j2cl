/*
 * Copyright 2016 Google Inc.
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
package com.google.j2cl.ast.visitors;

import com.google.common.collect.Lists;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.DeclaredTypeDescriptor;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implements inherited default methods as concrete forwarding methods. */
public class DefaultMethodsResolver extends NormalizationPass {
  /** Creates forwarding stubs in classes that 'inherit' a default method implementation. */
  @Override
  public void applyTo(Type type) {
    if (type.isInterface()) {
      // Only classes inherit default methods.
      return;
    }

    // Collect all super interfaces with more specific interfaces first since default methods in
    // more specific interfaces take precedence over default methods in less specific interfaces.
    List<DeclaredTypeDescriptor> declarationSuperInterfaceTypeDescriptors =
        collectDeclarationSuperInterfaces(type.getDeclaration());

    // Gather all (most specific) default methods declared through the interfaces of this class.
    Map<String, MethodDescriptor> defaultMethodDescriptorsBySignature =
        getDefaultMethodDescriptorsBySignature(
            type.getDeclaration(), declarationSuperInterfaceTypeDescriptors);

    // Remove methods that are already implemented in the class or any of its superclasses
    DeclaredTypeDescriptor declaringClassTypeDescriptor =
        type.isInterface() ? null : type.getTypeDescriptor();
    while (declaringClassTypeDescriptor != null) {
      for (MethodDescriptor declaredMethodDescriptor :
          declaringClassTypeDescriptor.getDeclaredMethodDescriptors()) {
        defaultMethodDescriptorsBySignature.remove(declaredMethodDescriptor.getOverrideSignature());
      }
      declaringClassTypeDescriptor = declaringClassTypeDescriptor.getSuperTypeDescriptor();
    }

    // Finally implement a forwarding method to the actual interface method.
    implementDefaultMethods(type, defaultMethodDescriptorsBySignature);
  }

  private static Map<String, MethodDescriptor> getDefaultMethodDescriptorsBySignature(
      TypeDeclaration typeDeclaration, List<DeclaredTypeDescriptor> superInterfaceTypeDescriptors) {
    // Gather all (most specific) default methods declared through the interfaces of this class.
    Map<String, MethodDescriptor> defaultMethodDescriptorsBySignature = new LinkedHashMap<>();
    for (DeclaredTypeDescriptor superInterfaceTypeDescriptor : superInterfaceTypeDescriptors) {
      for (MethodDescriptor declaredMethodDescriptor :
          superInterfaceTypeDescriptor.getDeclaredMethodDescriptors()) {
        if (!declaredMethodDescriptor.isDefaultMethod() || declaredMethodDescriptor.isJsOverlay()) {
          continue;
        }

        MethodDescriptor specializedMethodDescriptor =
            declaredMethodDescriptor.specializeTypeVariables(
                typeDeclaration
                    .toUnparameterizedTypeDescriptor()
                    .getSpecializedTypeArgumentByTypeParameters());

        String specializedSignature = specializedMethodDescriptor.getOverrideSignature();
        if (defaultMethodDescriptorsBySignature.containsKey(specializedSignature)) {
          continue;
        }
        defaultMethodDescriptorsBySignature.put(specializedSignature, declaredMethodDescriptor);
      }
    }
    return defaultMethodDescriptorsBySignature;
  }

  private static List<DeclaredTypeDescriptor> collectDeclarationSuperInterfaces(
      TypeDeclaration typeDeclaration) {
    List<DeclaredTypeDescriptor> typeDescriptors =
        Lists.newArrayList(typeDeclaration.getTransitiveInterfaceTypeDescriptors());
    // Make sure the interface descriptors are declaration versions. This improves cache hits in
    // some later analysis.
    for (int i = 0; i < typeDescriptors.size(); i++) {
      DeclaredTypeDescriptor superInterfaceTypeDescriptor = typeDescriptors.get(i);
      typeDescriptors.set(i, superInterfaceTypeDescriptor.toUnparameterizedTypeDescriptor());
    }

    // Sort so most specific interfaces are first.
    Collections.sort(
        typeDescriptors,
        (t1, t2) ->
            t2.getTypeDeclaration().getMaxInterfaceDepth()
                - t1.getTypeDeclaration().getMaxInterfaceDepth());

    return typeDescriptors;
  }

  private static void implementDefaultMethods(
      Type type, Map<String, MethodDescriptor> defaultMethodDescriptorsBySignature) {
    // Finally implement the methods by as forwarding stubs to the actual interface method.
    for (MethodDescriptor targetMethodDescriptor : defaultMethodDescriptorsBySignature.values()) {
      Method defaultForwardingMethod =
          AstUtils.createStaticForwardingMethod(
              type.getSourcePosition(),
              targetMethodDescriptor,
              type.getTypeDescriptor(),
              "Default method forwarding stub.");
      type.addMethod(defaultForwardingMethod);
    }
  }
}
