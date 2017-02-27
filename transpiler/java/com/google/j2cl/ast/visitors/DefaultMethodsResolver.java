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
      Map<String, MethodDescriptor> applicableDefaultMethodsBySignature =
          getApplicableDefaultMethodsBySignature(
              type.getDeclaration(), declarationSuperInterfaceTypeDescriptors);

      // Remove methods that are already implemented in the class or any of its superclasses
      TypeDescriptor implementingClass =
          type.isInterface() ? null : type.getDeclaration().getUnsafeTypeDescriptor();
      while (implementingClass != null) {
        for (MethodDescriptor implementedMethod :
            implementingClass.getDeclaredMethodDescriptors()) {
          applicableDefaultMethodsBySignature.remove(implementedMethod.getOverrideSignature());
        }
        implementingClass = implementingClass.getSuperTypeDescriptor();
      }

      // Finally implement a forwarding method to the actual interface method.
      implementDefaultMethods(type, applicableDefaultMethodsBySignature);
    }
  }

  private static Map<String, MethodDescriptor> getApplicableDefaultMethodsBySignature(
      TypeDeclaration typeDeclaration, List<TypeDescriptor> superInterfaces) {
    // Gather all (most specific) default methods declared through the interfaces of this class.
    Map<String, MethodDescriptor> applicableDefaultMethodsBySignature = new LinkedHashMap<>();
    for (TypeDescriptor interfaceBinding : superInterfaces) {
      for (MethodDescriptor methodBinding : interfaceBinding.getDeclaredMethodDescriptors()) {
        if (!methodBinding.isDefaultMethod()) {
          continue;
        }

        MethodDescriptor specializedMethodDescriptor =
            methodBinding.specializeTypeVariables(
                typeDeclaration
                    .getUnsafeTypeDescriptor()
                    .getSpecializedTypeArgumentByTypeParameters());

        String specializedSignature = specializedMethodDescriptor.getOverrideSignature();
        if (applicableDefaultMethodsBySignature.containsKey(specializedSignature)) {
          continue;
        }
        applicableDefaultMethodsBySignature.put(specializedSignature, methodBinding);
      }
    }
    return applicableDefaultMethodsBySignature;
  }

  private static List<TypeDescriptor> collectDeclarationSuperInterfaces(TypeDeclaration type) {
    List<TypeDescriptor> superInterfaces =
        Lists.newArrayList(type.getTransitiveInterfaceTypeDescriptors());
    // Make sure the interface descriptors are declaration versions. This improves cache hits in
    // some later analysis.
    for (int i = 0; i < superInterfaces.size(); i++) {
      TypeDescriptor superInterface = superInterfaces.get(i);
      superInterfaces.set(i, superInterface.getTypeDeclaration().getUnsafeTypeDescriptor());
    }
    Collections.sort(superInterfaces, TypeDescriptor.MORE_SPECIFIC_INTERFACES_FIRST);
    return superInterfaces;
  }

  private static void implementDefaultMethods(
      Type type, Map<String, MethodDescriptor> applicableDefaultMethodsBySignature) {
    // Finally implement the methods by as forwarding stubs to the actual interface method.
    for (MethodDescriptor method : applicableDefaultMethodsBySignature.values()) {
      MethodDescriptor targetMethod = method;
      Method defaultForwardingMethod =
          AstUtils.createStaticForwardingMethod(
              targetMethod,
              type.getDeclaration().getUnsafeTypeDescriptor(),
              "Default method forwarding stub.");
      defaultForwardingMethod.setSourcePosition(type.getSourcePosition());
      type.addMethod(defaultForwardingMethod);
      if (method.isOrOverridesJsMember()) {
        type.addMethod(
            AstUtils.createForwardingMethod(
                null,
                MethodDescriptor.Builder.from(defaultForwardingMethod.getDescriptor())
                    .setJsInfo(JsInfo.NONE)
                    .setSynthetic(true)
                    .setBridge(true)
                    .setAbstract(false)
                    .build(),
                defaultForwardingMethod.getDescriptor(),
                "Bridge to JsMethod.",
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
                // The we could not omit generating m_n() due to accidental overrides in subclasses
                // but we could skip the override flag in just this case.
                //
                // See b/31312257.
                false));
      }
    }
  }
}
