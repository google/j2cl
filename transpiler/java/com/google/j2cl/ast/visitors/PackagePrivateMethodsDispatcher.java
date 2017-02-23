/*
 * Copyright 2015 Google Inc.
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

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDeclaration;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks package private method that is exposed by its public or protected overriding methods, and
 * returns the generated dispatch methods.
 */
public class PackagePrivateMethodsDispatcher extends NormalizationPass {
  /** Creates dispatch methods for package private methods and adds them to the java type. */
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      List<Method> dispatchMethods =
          findExposedOverriddenMethods(type.getDeclaration())
              .entrySet()
              .stream()
              .map(
                  entry -> {
                    return AstUtils.createForwardingMethod(
                        null,
                        MethodDescriptor.Builder.from(entry.getValue())
                            .setEnclosingClassTypeDescriptor(
                                type.getDeclaration().getUnsafeTypeDescriptor())
                            .setSynthetic(true)
                            .setBridge(true)
                            .setAbstract(false)
                            .build(),
                        entry.getKey(),
                        "Forwarding method for package private method.",
                        true);
                  })
              .collect(Collectors.toList());
      type.addMethods(dispatchMethods);
    }
  }

  /** Returns the mapping from public/protected method to its package private overridden method. */
  private static Map<MethodDescriptor, MethodDescriptor> findExposedOverriddenMethods(
      TypeDeclaration typeDeclaration) {
    Map<MethodDescriptor, MethodDescriptor> exposedOverriddenMethodsByOverridingMethod =
        new LinkedHashMap<>();
    for (MethodDescriptor method : typeDeclaration.getDeclaredMethodDescriptors()) {
      if (!(method.getVisibility().isPublic() || method.getVisibility().isProtected())
          || method.isStatic()
          || method.isConstructor()
          || method.isSynthetic()) {
        // ITypeBinding.getDeclaredMethods() may or may not include Synthetic methods and
        // constructors, and these methods are not what we care about.
        continue;
      }
      MethodDescriptor overriddenMethod = findDirectExposedOverriddenMethod(method);
      if (overriddenMethod == null) {
        continue;
      }
      exposedOverriddenMethodsByOverridingMethod.put(method, overriddenMethod);
    }
    return exposedOverriddenMethodsByOverridingMethod;
  }

  /**
   * Returns directly exposed overridden method by {@code method}. 'directly' means there is no
   * other method implementations between the inheritance chain. For example,
   *
   * <pre>
   * class SuperParent {
   *   void f1() {}
   *   void f2() {}
   *   void f3()
   * }
   * class Parent extends SuperParent {
   *   void f1() {}
   *   public void f3()
   * }
   * class Child extends Parent {
   *   public void f1() {}
   *   public void f2() {}
   *   public void f3() {}
   * }
   * </pre>
   *
   * <p>"Child.f1()" directly exposes "Parent.f1()", but does not directly expose
   * "SuperParent.f1();". "Child.f2()" directly exposes "SuperParent.f2();". "Parent.f3()" directly
   * exposes "SuperParent.f3()", but "Child.f3()" does not.
   */
  private static MethodDescriptor findDirectExposedOverriddenMethod(
      MethodDescriptor overridingMethod) {
    MethodDescriptor directOverriddenMethod = findDirectOverriddenMethod(overridingMethod);
    if (directOverriddenMethod == null) {
      return null;
    }
    boolean upgradesPackagePrivateVisibility =
        directOverriddenMethod.getVisibility().isPackagePrivate()
            && (overridingMethod.getVisibility().isPublic()
                || overridingMethod.getVisibility().isProtected());
    return upgradesPackagePrivateVisibility ? directOverriddenMethod : null;
  }

  /**
   * Returns directly overridden method in the super classes chain. We don't care about its super
   * interfaces since interface methods are always public.
   */
  private static MethodDescriptor findDirectOverriddenMethod(MethodDescriptor overridingMethod) {
    TypeDescriptor superclass =
        overridingMethod.getEnclosingClassTypeDescriptor().getSuperTypeDescriptor();
    while (superclass != null) {
      for (MethodDescriptor method : superclass.getDeclaredMethodDescriptors()) {
        if (overridingMethod.isOverride(method)) {
          return method;
        }
      }
      superclass = superclass.getSuperTypeDescriptor();
    }
    return null;
  }
}
