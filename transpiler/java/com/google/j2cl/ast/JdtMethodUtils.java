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
package com.google.j2cl.ast;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility functions to transpile JDT MethodBinding to J2cl MethodDescriptor.
 */
public class JdtMethodUtils {
  public static MethodDescriptor createMethodDescriptor(IMethodBinding methodBinding) {
    int modifiers = methodBinding.getModifiers();
    boolean isStatic = Modifier.isStatic(modifiers);
    Visibility visibility = TypeProxyUtils.getVisibility(modifiers);
    boolean isNative = Modifier.isNative(modifiers);
    TypeDescriptor enclosingClassTypeDescriptor =
        TypeProxyUtils.createTypeDescriptor(methodBinding.getDeclaringClass());
    boolean isConstructor = methodBinding.isConstructor();
    String methodName =
        isConstructor
            ? TypeProxyUtils.createTypeDescriptor(methodBinding.getDeclaringClass()).getClassName()
            : methodBinding.getName();

    JsInfo jsInfo = computeJsInfo(methodBinding);
    boolean isRaw = isOrOverridesJsMember(methodBinding);

    TypeDescriptor returnTypeDescriptor =
        TypeProxyUtils.createTypeDescriptor(methodBinding.getReturnType());

    // generate parameters type descriptors.
    Iterable<TypeDescriptor> parameterTypeDescriptors =
        FluentIterable.from(Arrays.asList(methodBinding.getMethodDeclaration().getParameterTypes()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    // Whenever we create the parameter types of a method,
                    // we use the rawTypeDescriptor.
                    return TypeProxyUtils.createTypeDescriptor(typeBinding).getRawTypeDescriptor();
                  }
                });
    // generate type parameters declared in the method.
    Iterable<TypeDescriptor> typeParameterDescriptors =
        FluentIterable.from(Arrays.asList(methodBinding.getTypeParameters()))
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return TypeProxyUtils.createTypeDescriptor(typeBinding);
                  }
                });

    return MethodDescriptor.create(
        isStatic,
        isRaw,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        returnTypeDescriptor,
        parameterTypeDescriptors,
        typeParameterDescriptors,
        jsInfo);
  }

  public static boolean isOrOverridesJsMember(IMethodBinding methodBinding) {
    return JsInteropUtils.isJsMember(methodBinding)
        || !getOverriddenJsMembers(methodBinding).isEmpty();
  }

  /**
   * Checks overriding chain to compute JsInfo.
   */
  static JsInfo computeJsInfo(IMethodBinding methodBinding) {
    // The direct JsInfo.
    JsInfo jsInfo = JsInteropUtils.getJsInfo(methodBinding);
    if (!jsInfo.isNone()) {
      return jsInfo;
    }
    // Checks overriding chain.
    for (IMethodBinding overriddenMethod : getOverriddenMethods(methodBinding)) {
      JsInfo inheritedJsInfo = JsInteropUtils.getJsInfo(overriddenMethod);
      if (!inheritedJsInfo.isNone()) {
        return inheritedJsInfo;
      }
    }
    return JsInfo.NONE;
  }

  public static Set<IMethodBinding> getOverriddenJsMembers(IMethodBinding methodBinding) {
    return Sets.filter(
        getOverriddenMethods(methodBinding),
        new Predicate<IMethodBinding>() {
          @Override
          public boolean apply(IMethodBinding overriddenMethod) {
            return JsInteropUtils.isJsMember(overriddenMethod);
          }
        });
  }

  public static Set<IMethodBinding> getOverriddenMethods(IMethodBinding methodBinding) {
    Set<IMethodBinding> overriddenMethods = new HashSet<>();
    ITypeBinding enclosingClass = methodBinding.getDeclaringClass();
    ITypeBinding superClass = enclosingClass.getSuperclass();
    if (superClass != null) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, superClass));
    }
    for (ITypeBinding superInterface : enclosingClass.getInterfaces()) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, superInterface));
    }
    return overriddenMethods;
  }

  static Set<IMethodBinding> getOverriddenMethodsInType(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    Set<IMethodBinding> overriddenMethods = new HashSet<>();
    for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
      if (methodBinding.overrides(declaredMethod)) {
        overriddenMethods.add(declaredMethod);
      }
    }
    // Recurse into immediate super class and interfaces for overridden method.
    if (typeBinding.getSuperclass() != null) {
      overriddenMethods.addAll(
          getOverriddenMethodsInType(methodBinding, typeBinding.getSuperclass()));
    }
    for (ITypeBinding interfaceBinding : typeBinding.getInterfaces()) {
      overriddenMethods.addAll(getOverriddenMethodsInType(methodBinding, interfaceBinding));
    }
    return overriddenMethods;
  }
}
