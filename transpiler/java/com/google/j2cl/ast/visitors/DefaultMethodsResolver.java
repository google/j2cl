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
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implements inherited default methods as concrete forwarding methods. */
public class DefaultMethodsResolver extends NormalizationPass {
  /** Creates forwarding stubs in classes that 'inherit' a default method implementation. */
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      if (type.isInterface() || type.isAbstract()) {
        // Only concrete classes inherit default methods. Nothing to do.
        continue;
      }

      // Collect all super interfaces with more specific interfaces first since default methods in
      // more specific interfaces take precedence over default methods in less specific interfaces.
      List<TypeDescriptor> declarationSuperInterfaceTypeDescriptors =
          collectDeclarationSuperInterfaces(type.getDeclaration());

      // Gather all (most specific) default methods declared through the interfaces of this class.
      Map<String, MethodDescriptor> defaultMethodDescriptorsBySignature =
          getDefaultMethodDescriptorsBySignature(
              type.getDeclaration(), declarationSuperInterfaceTypeDescriptors);

      // Remove methods that are already implemented in the class or any of its superclasses
      TypeDescriptor declaringClassTypeDescriptor =
          type.isInterface() ? null : type.getDeclaration().getUnsafeTypeDescriptor();
      while (declaringClassTypeDescriptor != null) {
        for (MethodDescriptor declaredMethodDescriptor :
            declaringClassTypeDescriptor.getDeclaredMethodDescriptors()) {
          defaultMethodDescriptorsBySignature.remove(
              declaredMethodDescriptor.getOverrideSignature());
        }
        declaringClassTypeDescriptor = declaringClassTypeDescriptor.getSuperTypeDescriptor();
      }

      // Finally implement a forwarding method to the actual interface method.
      implementDefaultMethods(type, defaultMethodDescriptorsBySignature);
    }
  }

  private static Map<String, MethodDescriptor> getDefaultMethodDescriptorsBySignature(
      TypeDeclaration typeDeclaration, List<TypeDescriptor> superInterfaceTypeDescriptors) {
    // Gather all (most specific) default methods declared through the interfaces of this class.
    Map<String, MethodDescriptor> defaultMethodDescriptorsBySignature = new LinkedHashMap<>();
    for (TypeDescriptor superInterfaceTypeDescriptor : superInterfaceTypeDescriptors) {
      for (MethodDescriptor declaredMethodDescriptor :
          superInterfaceTypeDescriptor.getDeclaredMethodDescriptors()) {
        if (!declaredMethodDescriptor.isDefaultMethod()) {
          continue;
        }

        MethodDescriptor specializedMethodDescriptor =
            declaredMethodDescriptor.specializeTypeVariables(
                typeDeclaration
                    .getUnsafeTypeDescriptor()
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

  private static List<TypeDescriptor> collectDeclarationSuperInterfaces(
      TypeDeclaration typeDeclaration) {
    List<TypeDescriptor> typeDescriptors =
        Lists.newArrayList(typeDeclaration.getTransitiveInterfaceTypeDescriptors());
    // Make sure the interface descriptors are declaration versions. This improves cache hits in
    // some later analysis.
    for (int i = 0; i < typeDescriptors.size(); i++) {
      TypeDescriptor superInterfaceTypeDescriptor = typeDescriptors.get(i);
      typeDescriptors.set(
          i, superInterfaceTypeDescriptor.getTypeDeclaration().getUnsafeTypeDescriptor());
    }
    Collections.sort(typeDescriptors, TypeDescriptor.MORE_SPECIFIC_INTERFACES_FIRST);
    return typeDescriptors;
  }

  private static void implementDefaultMethods(
      Type type, Map<String, MethodDescriptor> defaultMethodDescriptorsBySignature) {
    // Finally implement the methods by as forwarding stubs to the actual interface method.
    for (MethodDescriptor targetMethodDescriptor : defaultMethodDescriptorsBySignature.values()) {
      Method defaultForwardingMethod =
          AstUtils.createStaticForwardingMethod(
              targetMethodDescriptor,
              type.getDeclaration().getUnsafeTypeDescriptor(),
              "Default method forwarding stub.");
      defaultForwardingMethod.setSourcePosition(type.getSourcePosition());
      type.addMethod(defaultForwardingMethod);

      if (targetMethodDescriptor.isOrOverridesJsMember()) {
        // Depending on the class hierarchy these methods might or might not be an override
        // e.g.
        //   interface I {        interface J {
        //     m();                 @JsMethod default m() {}
        //   }                      @JsMethod default n() {}
        //                        }
        //
        //   class A implements I, J { }
        //
        // class A in Js will be emitted as follows
        //
        //   class A {
        //     m()   { J.m(); }     // Overrides J.m
        //     m_m() { this.m(); }  // Overrides I.m
        //     n()   { J.n(); }     // Overrides J.n
        //     m_n() { this.n(); }  // does not override anything
        //
        // We could not omit generating m_n() due to accidental overrides in subclasses
        // but we could skip the override flag in just this case.
        //
        // See b/31312257.
        type.addMethod(
            AstUtils.createForwardingMethod(
                null,
                MethodDescriptor.Builder.from(defaultForwardingMethod.getDescriptor())
                    .setJsInfo(JsInfo.NONE)
                    .setSynthetic(true)
                    .setBridge(true)
                    .setAbstract(false)
                    .setNative(false)
                    .build(),
                defaultForwardingMethod.getDescriptor(),
                "Bridge to JsMethod.",
                false));
      }
    }
  }
}
