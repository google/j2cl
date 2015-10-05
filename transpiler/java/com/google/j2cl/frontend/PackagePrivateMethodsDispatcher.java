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
package com.google.j2cl.frontend;

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks package private method that is exposed by its public or protected overriding methods,
 * and returns the generated dispatch methods.
 */
public class PackagePrivateMethodsDispatcher {
  /**
   * Returns generated dispatch methods.
   */
  public static List<Method> create(ITypeBinding typeBinding) {
    List<Method> dispatchMethods = new ArrayList<>();
    for (Map.Entry<MethodDescriptor, MethodDescriptor> entry :
        findExposedOverriddenMethods(typeBinding).entrySet()) {
      dispatchMethods.add(AstUtils.createForwardingMethod(entry.getValue(), entry.getKey()));
    }
    return dispatchMethods;
  }

  /**
   * Returns the mapping from public/protected method to its package private overridden method.
   */
  public static Map<MethodDescriptor, MethodDescriptor> findExposedOverriddenMethods(
      ITypeBinding type) {
    Map<MethodDescriptor, MethodDescriptor> exposedOverriddenMethodsByOverridingMethod =
        new LinkedHashMap<>();
    for (IMethodBinding method : type.getDeclaredMethods()) {
      int modifiers = method.getModifiers();
      if (!(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) // public/protected.
          || Modifier.isStatic(modifiers) // non-static
          || method.isConstructor()
          || method.isSynthetic()) {
        // ITypeBinding.getDeclaredMethods() may or may not include Synthetic methods and
        // constructors, and these methods are not what we care about.
        continue;
      }
      IMethodBinding overriddenMethod = findDirectExposedOverriddenMethod(method);
      if (overriddenMethod == null) {
        continue;
      }
      exposedOverriddenMethodsByOverridingMethod.put(
          JdtUtils.createMethodDescriptor(method),
          JdtUtils.createMethodDescriptor(overriddenMethod));
    }
    return exposedOverriddenMethodsByOverridingMethod;
  }

  /**
   * Returns directly exposed overridden method by {@code method}.
   * 'directly' means there is no other method implementations between the inheritance chain.
   * For example,
   * class SuperParent {void f1(){} void f2(){} void f3()}
   * class Parent extends SuperParent {void f1()} public void f3()}
   * class Child extends Parent {public void f1(){} public void f2(){} public void f3()}
   * Child.f1() directly expose Parent.f1(), but not directly expose SuperParent.f1();
   * Child.f2() directly expose SuperParent.f2();
   * Parent.f3() directly expose SuperParent.f3(), but Child.f3() does not.
   */
  private static IMethodBinding findDirectExposedOverriddenMethod(IMethodBinding overridingMethod) {
    IMethodBinding directOverriddenMethod = findDirectOverriddenMethod(overridingMethod);
    if (directOverriddenMethod == null) {
      return null;
    }
    return JdtUtils.upgradesPackagePrivateVisibility(overridingMethod, directOverriddenMethod)
        ? directOverriddenMethod
        : null;
  }

  /**
   * Returns directly overridden method in the super classes chain. We don't care about its
   * super interfaces since interface methods are always public.
   */
  private static IMethodBinding findDirectOverriddenMethod(IMethodBinding overridingMethod) {
    ITypeBinding superclass = overridingMethod.getDeclaringClass().getSuperclass();
    while (superclass != null) {
      for (IMethodBinding method : superclass.getDeclaredMethods()) {
        if (overridingMethod.overrides(method)) {
          return method;
        }
      }
      superclass = superclass.getSuperclass();
    }
    return null;
  }
}
