/*
 * Copyright 2023 Google Inc.
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

import com.google.j2cl.transpiler.ast.AstUtils;
import com.google.j2cl.transpiler.ast.DeclaredTypeDescriptor;
import com.google.j2cl.transpiler.ast.Method;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.SuperReference;
import com.google.j2cl.transpiler.ast.Type;
import com.google.j2cl.transpiler.ast.TypeDeclaration;
import com.google.j2cl.transpiler.ast.TypeDescriptors;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Add forwarding methods that implement interfaces that declare {@code java.lang.Object} methods.
 * Kotlin requires explicit re-declaration of such methods vs. inheriting them from {@code Any}.
 */
public class AddJavaLangObjectForwardingMethods extends NormalizationPass {

  // TODO(b/322906767): Remove when the bug is fixed.
  private static final boolean PRESERVE_EQUALS_FOR_JSTYPE_INTERFACE =
      "true"
          .equals(
              System.getProperty(
                  "com.google.j2cl.transpiler.backend.kotlin.preserveEqualsForJsTypeInterface"));

  @Override
  public void applyTo(Type type) {
    if (type.isInterface()) {
      return;
    }

    // Note that we are also handling abstract classes. Even they do not require the redeclarations,
    // implementing such interface nullifies inherited methods from abstract class. So the class
    // that extend from them can no longer do super.equals etc. unless they are redeclared.

    Map<String, MethodDescriptor> requiredJavaLangObjectMethods =
        type.getDeclaration().getInterfaceTypeDescriptors().stream()
            .flatMap(t -> t.getTypeDeclaration().getAllSuperTypesIncludingSelf().stream())
            .filter(TypeDeclaration::isInterface)
            // TODO(b/317299672): Remove JsType special casing since should preserve all of them for
            // migration purposes.
            .filter(t -> PRESERVE_EQUALS_FOR_JSTYPE_INTERFACE && t.isJsType())
            .flatMap(t -> t.getDeclaredMethodDescriptors().stream())
            .filter(MethodDescriptor::isOrOverridesJavaLangObjectMethod)
            .collect(toMangledNameMap());

    if (requiredJavaLangObjectMethods.isEmpty()) {
      return;
    }

    // Remove method that are already overridden from required ones.
    for (DeclaredTypeDescriptor current = type.getTypeDescriptor();
        !TypeDescriptors.isJavaLangObject(current);
        current = current.getSuperTypeDescriptor()) {
      for (MethodDescriptor m : current.getDeclaredMethodDescriptors()) {
        // Method is already declared so we don't need to redefine.
        requiredJavaLangObjectMethods.remove(m.getMangledName());
        if (requiredJavaLangObjectMethods.isEmpty()) {
          // Exit early since there is no work to be done.
          return;
        }
      }
    }

    // Define all missing methods as forwarding methods to parent one.
    for (MethodDescriptor m : requiredJavaLangObjectMethods.values()) {
      // Target the declaration from j.l.Object instead of the interface so super is qualified
      // correctly.
      MethodDescriptor targetMethod =
          TypeDescriptors.get().javaLangObject.getMethodDescriptorByName(m.getName());
      type.addMember(createSuperForwardingMethod(type, targetMethod));
    }
  }

  // TODO(goktug): Refactor to reuse with other similar super forwarding helpers.
  private static Method createSuperForwardingMethod(Type type, MethodDescriptor targetMethod) {
    MethodDescriptor forwardingMethod =
        MethodDescriptor.Builder.from(targetMethod)
            .setEnclosingTypeDescriptor(type.getTypeDescriptor())
            .makeDeclaration()
            .setNative(false)
            .build();
    return AstUtils.createForwardingMethod(
        type.getSourcePosition(),
        new SuperReference(type.getTypeDescriptor()),
        forwardingMethod,
        targetMethod,
        null);
  }

  private static Collector<MethodDescriptor, ?, LinkedHashMap<String, MethodDescriptor>>
      toMangledNameMap() {
    return Collectors.toMap(
        MethodDescriptor::getMangledName, Function.identity(), (a, b) -> a, LinkedHashMap::new);
  }
}
