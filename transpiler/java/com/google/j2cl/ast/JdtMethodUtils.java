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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.j2cl.ast.TypeProxyUtils.Nullability;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility functions to transpile JDT MethodBinding to J2cl MethodDescriptor.
 */
public class JdtMethodUtils {

  /**
   * Creates a MethodDescriptor directly based on the given JDT method binding.
   */
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
            ? TypeProxyUtils.createTypeDescriptor(methodBinding.getDeclaringClass())
                .getBinaryClassName()
            : methodBinding.getName();
    final Nullability defaultNullabilityForPackage =
        TypeProxyUtils.getPackageDefaultNullability(methodBinding.getDeclaringClass().getPackage());

    JsInfo jsInfo = computeJsInfo(methodBinding);

    TypeDescriptor returnTypeDescriptor =
        TypeProxyUtils.createTypeDescriptorWithNullability(
            methodBinding.getReturnType(),
            methodBinding.getAnnotations(),
            defaultNullabilityForPackage);

    // generate parameters type descriptors.
    List<TypeDescriptor> parameterTypeDescriptors = new ArrayList<>();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      TypeDescriptor descriptor = TypeProxyUtils.createTypeDescriptorWithNullability(
          methodBinding.getParameterTypes()[i],
          methodBinding.getParameterAnnotations(i),
          defaultNullabilityForPackage);
      parameterTypeDescriptors.add(descriptor);
    }

    MethodDescriptor declarationMethodDescriptor = null;
    if (methodBinding.getMethodDeclaration() != methodBinding) {
      declarationMethodDescriptor = createMethodDescriptor(methodBinding.getMethodDeclaration());
    }

    // generate type parameters declared in the method.
    Iterable<TypeDescriptor> typeParameterDescriptors =
        FluentIterable.from(methodBinding.getTypeParameters())
            .transform(
                new Function<ITypeBinding, TypeDescriptor>() {
                  @Override
                  public TypeDescriptor apply(ITypeBinding typeBinding) {
                    return TypeProxyUtils.createTypeDescriptor(typeBinding);
                  }
                });

    return MethodDescriptor.create(
        isStatic,
        visibility,
        enclosingClassTypeDescriptor,
        methodName,
        isConstructor,
        isNative,
        methodBinding.isVarargs(),
        declarationMethodDescriptor,
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
    List<JsInfo> jsInfoList = new ArrayList<>();
    // Add the JsInfo of the method and all the overridden methods to the list.
    JsInfo jsInfo = JsInteropUtils.getJsInfo(methodBinding);
    if (!jsInfo.isNone()) {
      jsInfoList.add(jsInfo);
    }
    for (IMethodBinding overriddenMethod : getOverriddenMethods(methodBinding)) {
      JsInfo inheritedJsInfo = JsInteropUtils.getJsInfo(overriddenMethod);
      if (!inheritedJsInfo.isNone()) {
        jsInfoList.add(inheritedJsInfo);
      }
    }

    if (jsInfoList.isEmpty()) {
      return JsInfo.NONE;
    }

    // TODO: Do the same for JsProperty?
    if (jsInfoList.get(0).getJsMemberType() == JsMemberType.METHOD) {
      // Return the first JsInfo with a Js name specified.
      for (JsInfo jsInfoElement : jsInfoList) {
        if (jsInfoElement.getJsName() != null) {
          return jsInfoElement;
        }
      }
    }
    return jsInfoList.get(0);
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
      if (methodBinding.overrides(declaredMethod) && !methodBinding.isConstructor()) {
        checkArgument(!Modifier.isStatic(methodBinding.getModifiers()));
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
