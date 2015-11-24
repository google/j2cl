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
import com.google.j2cl.ast.JsInteropUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;

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
 * Creates bridge methods for instance JsMethods.
 */
public class JsBridgeMethodsCreator {
  /**
   * Returns generated bridge methods.
   */
  public static List<Method> create(ITypeBinding typeBinding) {
    return new JsBridgeMethodsCreator(typeBinding).createBridgeMethods();
  }

  private ITypeBinding typeBinding;

  private JsBridgeMethodsCreator(ITypeBinding typeBinding) {
    this.typeBinding = typeBinding;
  }

  /**
   * Bridge method should be generated in current type in the following two cases:
   *
   * <p>1. A method in current type is or overrides a JsMethod, and exposes a non-JsMethod. The
   * bridge method has the un-mangled name and delegates to the non-JsMethod.
   *
   * <p>2. Current type does not declare an overriding method for its interfaces' method but it is
   * accidentally overridden by its super classes. In this case:
   *
   * <p>2(a). If interface method is a JsMethod, and accidental overriding method is a non-JsMethod,
   * a bridge method is needed from JsMethod delegating to non-JsMethod.
   *
   * <p>2(b). If interface method is a non-JsMethod, and accidental overridding method is JsMehtod,
   * a bridge method is needed from non-JsMethod delegating to JsMethod.
   */
  private List<Method> createBridgeMethods() {
    List<Method> generatedBridgeMethods = new ArrayList<>();
    Set<MethodDescriptor> generatedBridgeMethodDescriptors = new HashSet<>();
    for (Entry<IMethodBinding, IMethodBinding> entry :
        getBridgeDelegateMethodsMapping().entrySet()) {
      Method bridgeMethod = createBridgeMethod(entry.getKey(), entry.getValue());
      if (generatedBridgeMethodDescriptors.contains(bridgeMethod.getDescriptor())) {
        continue;
      }
      generatedBridgeMethods.add(bridgeMethod);
      generatedBridgeMethodDescriptors.add(bridgeMethod.getDescriptor());
    }
    return generatedBridgeMethods;
  }

  /**
   * Returns the mapping from the bridge method to the delegating method.
   */
  private Map<IMethodBinding, IMethodBinding> getBridgeDelegateMethodsMapping() {
    Map<IMethodBinding, IMethodBinding> delegateByBridgeMethods = new LinkedHashMap<>();

    // case 1. exposed non-JsMethod to the exposing JsMethod.
    for (IMethodBinding declaredMethod : typeBinding.getDeclaredMethods()) {
      IMethodBinding exposedNonJsMethod = getExposedNonJsMethod(declaredMethod);
      if (exposedNonJsMethod != null) {
        delegateByBridgeMethods.put(exposedNonJsMethod, declaredMethod);
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
      // if for the overridden and overriding methods, one is JsMethod, and the other is not,
      // generate a bridge method from the overridden method to the overriding method.
      boolean isJsMethodOne = JdtUtils.isOrOverridesJsMethod(overridingMethod);
      boolean isJsMethodOther = JdtUtils.isOrOverridesJsMethod(accidentalOverriddenMethod);
      if (isJsMethodOne != isJsMethodOther) {
        delegateByBridgeMethods.put(accidentalOverriddenMethod, overridingMethod);
      }
    }

    return delegateByBridgeMethods;
  }

  /**
   * If this method is the first JsMethod in the method hierarchy that exposes an existing
   * non-JsMethod, returns the non-JsMethod it exposes, otherwise, returns null.
   */
  private static IMethodBinding getExposedNonJsMethod(IMethodBinding methodBinding) {
    if (!JdtUtils.isOrOverridesJsMethod(methodBinding)
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
    IMethodBinding overriddenNonJsMethod = null;
    for (IMethodBinding overriddenMethod : JdtUtils.getOverriddenMethods(methodBinding)) {
      if (!JdtUtils.isOrOverridesJsMethod(overriddenMethod)) {
        overriddenNonJsMethod = overriddenMethod;
      }
      if (getExposedNonJsMethod(overriddenMethod) != null) {
        return null; // the overridden method has already exposed the method.
      }
    }
    return overriddenNonJsMethod;
  }

  private Method createBridgeMethod(
      IMethodBinding bridgeMethodBinding, IMethodBinding forwardingMethodBinding) {
    MethodDescriptor bridgeMethodDescriptor =
        MethodDescriptorBuilder.from(JdtUtils.createMethodDescriptor(bridgeMethodBinding))
            .enclosingClassTypeDescriptor(JdtUtils.createTypeDescriptor(typeBinding))
            .build();
    MethodDescriptor forwardingMethodDescriptor =
        JdtUtils.createMethodDescriptor(forwardingMethodBinding);
    return AstUtils.createForwardingMethod(bridgeMethodDescriptor, forwardingMethodDescriptor);
  }
}
