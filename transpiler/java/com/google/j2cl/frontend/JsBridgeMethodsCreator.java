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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JdtMethodUtils;
import com.google.j2cl.ast.JsInteropUtils;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Creates bridge methods for instance JsMembers.
 */
public class JsBridgeMethodsCreator {
  /**
   * Creates bridge methods and adds them to the java type.
   */
  public static void create(ITypeBinding typeBinding, JavaType javaType) {
    javaType.addMethods(
        new JsBridgeMethodsCreator(typeBinding).createBridgeMethods(javaType.getMethods()));
  }

  private ITypeBinding typeBinding;

  private JsBridgeMethodsCreator(ITypeBinding typeBinding) {
    this.typeBinding = typeBinding;
  }

  /**
   * Bridge method should be generated in current type in the following two cases:
   *
   * <p>1. A method in current type is or overrides a JsMember, and exposes a non-JsMember. The
   * bridge method has the un-mangled name and delegates to the non-JsMember.
   *
   * <p>2. Current type does not declare an overriding method for its interfaces' method but it is
   * accidentally overridden by its super classes. In this case:
   *
   * <p>2(a). If interface method is a JsMember, and accidental overriding method is a non-JsMember,
   * a bridge method is needed from JsMember delegating to non-JsMember.
   *
   * <p>2(b). If interface method is a non-JsMember, and accidental overridding method is JsMember,
   * a bridge method is needed from non-JsMember delegating to JsMember.
   */
  private List<Method> createBridgeMethods(List<Method> existingMethods) {
    List<Method> generatedBridgeMethods = new ArrayList<>();
    Set<String> generatedBridgeMethodMangledNames = new HashSet<>();
    List<String> existingMethodMangledNames =
        Lists.transform(
            existingMethods,
            new Function<Method, String>() {
              @Override
              public String apply(Method method) {
                return ManglingNameUtils.getMangledName(method.getDescriptor());
              }
            });
    for (Entry<IMethodBinding, IMethodBinding> entry :
        getBridgeDelegateMethodsMapping().entrySet()) {
      Method bridgeMethod = createBridgeMethod(entry.getKey(), entry.getValue());
      String manglingName = ManglingNameUtils.getMangledName(bridgeMethod.getDescriptor());
      if (generatedBridgeMethodMangledNames.contains(manglingName)
          || existingMethodMangledNames.contains(manglingName)) {
        // Do not generate duplicate methods that have the same signature of the existing methods
        // in the type.
        continue;
      }
      generatedBridgeMethods.add(bridgeMethod);
      generatedBridgeMethodMangledNames.add(manglingName);
    }
    return generatedBridgeMethods;
  }

  /**
   * Returns the mapping from the bridge method to the delegating method.
   */
  private Map<IMethodBinding, IMethodBinding> getBridgeDelegateMethodsMapping() {
    Map<IMethodBinding, IMethodBinding> delegateByBridgeMethods = new LinkedHashMap<>();

    // case 1. exposed non-JsMember to the exposing JsMethod.
    for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
      IMethodBinding exposedNonJsMember = getExposedNonJsMember(declaredMethod);
      if (exposedNonJsMember != null) {
        delegateByBridgeMethods.put(exposedNonJsMember, declaredMethod);
      }
    }

    // case 2. accidental overridden methods.
    for (IMethodBinding accidentalOverriddenMethod :
        JdtUtils.getAccidentalOverriddenMethodBindings(typeBinding)) {
      IMethodBinding overridingMethod =
          JdtUtils.getOverridingMethodInSuperclasses(accidentalOverriddenMethod, typeBinding);
      if (overridingMethod == null) {
        continue;
      }
      // if for the overridden and overriding methods, one is JsMember, and the other is not,
      // generate a bridge method from the overridden method to the overriding method.
      boolean isJsMemberOne = JdtMethodUtils.isOrOverridesJsMember(overridingMethod);
      boolean isJsMemberOther = JdtMethodUtils.isOrOverridesJsMember(accidentalOverriddenMethod);
      if (isJsMemberOne != isJsMemberOther) {
        delegateByBridgeMethods.put(accidentalOverriddenMethod, overridingMethod);
      }
    }

    return delegateByBridgeMethods;
  }

  /**
   * If this method is the first JsMember in the method hierarchy that exposes an existing
   * non-JsMember, returns the non-JsMember it exposes, otherwise, returns null.
   */
  private static IMethodBinding getExposedNonJsMember(IMethodBinding methodBinding) {
    if (!JdtMethodUtils.isOrOverridesJsMember(methodBinding)
        || methodBinding.getDeclaringClass().isInterface()
        || JdtUtils.isStatic(methodBinding.getModifiers())
        || methodBinding.isConstructor()) {
      return null;
    }
    // native js type is not generated, thus it does not expose any non-js methods.
    if (JsInteropUtils.isNative(
        JsInteropUtils.getJsTypeAnnotation(methodBinding.getDeclaringClass()))) {
      return null;
    }
    IMethodBinding overriddenNonJsMember = null;
    for (IMethodBinding overriddenMethod : JdtMethodUtils.getOverriddenMethods(methodBinding)) {
      if (!JdtMethodUtils.isOrOverridesJsMember(overriddenMethod)) {
        overriddenNonJsMember = overriddenMethod;
      }
      if (getExposedNonJsMember(overriddenMethod) != null) {
        return null; // the overridden method has already exposed the method.
      }
    }
    return overriddenNonJsMember;
  }

  private Method createBridgeMethod(
      IMethodBinding bridgeMethodBinding, IMethodBinding forwardToMethodBinding) {
    MethodDescriptor forwardToMethodDescriptor =
        JdtUtils.createMethodDescriptor(forwardToMethodBinding);
    MethodDescriptor bridgeMethodDescriptor = JdtUtils.createMethodDescriptor(bridgeMethodBinding);
    return AstUtils.createForwardingMethod(
        bridgeMethodDescriptor,
        forwardToMethodDescriptor,
        JdtUtils.createTypeDescriptor(typeBinding),
        "Bridge method for exposing non-JsMethod.");
  }
}
