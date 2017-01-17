/*
 * Copyright 2017 Google Inc.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.Streams;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.ManglingNameUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import com.google.j2cl.ast.TypeDescriptor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/** Creates bridge methods for instance JsMembers. */
public class JsBridgeMethodsCreator extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    for (Type type : compilationUnit.getTypes()) {
      type.addMethods(createBridgeMethods(type.getDescriptor(), type.getMethods()));
    }
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
  private static List<Method> createBridgeMethods(
      TypeDescriptor typeDescriptor, Iterable<Method> existingMethods) {
    List<Method> generatedBridgeMethods = new ArrayList<>();
    Set<String> generatedBridgeMethodMangledNames = new HashSet<>();
    Set<String> existingMethodMangledNames =
        Streams.stream(existingMethods)
            .map(method -> ManglingNameUtils.getMangledName(method.getDescriptor()))
            .collect(toImmutableSet());
    for (Entry<MethodDescriptor, MethodDescriptor> entry :
        delegatedMethodDescriptorsByBridgeMethodDescriptor(typeDescriptor).entrySet()) {
      Method bridgeMethod = createBridgeMethod(typeDescriptor, entry.getKey(), entry.getValue());
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

  /** Returns the mapping from the bridge method to the delegating method. */
  private static Map<MethodDescriptor, MethodDescriptor>
      delegatedMethodDescriptorsByBridgeMethodDescriptor(TypeDescriptor typeDescriptor) {
    Map<MethodDescriptor, MethodDescriptor> delegateMethodDescriptorsByBridgeMethodDescriptor =
        new LinkedHashMap<>();

    // case 1. exposed non-JsMember to the exposing JsMethod.
    for (MethodDescriptor declaredMethod : typeDescriptor.getDeclaredMethodDescriptors()) {
      MethodDescriptor exposedNonJsMember = getExposedNonJsMember(declaredMethod);
      if (exposedNonJsMember != null) {
        delegateMethodDescriptorsByBridgeMethodDescriptor.put(exposedNonJsMember, declaredMethod);
      }
    }

    // case 2. accidental overridden methods.
    for (MethodDescriptor accidentalOverriddenMethod :
        typeDescriptor.getAccidentallyOverriddenMethodDescriptors()) {
      MethodDescriptor overridingMethod =
          typeDescriptor.getOverridingMethodDescriptorInSuperclasses(accidentalOverriddenMethod);
      if (overridingMethod == null) {
        continue;
      }
      // if for the overridden and overriding methods, one is JsMember, and the other is not,
      // generate a bridge method from the overridden method to the overriding method.
      boolean isJsMemberOne = overridingMethod.isOrOverridesJsMember();
      boolean isJsMemberOther = accidentalOverriddenMethod.isOrOverridesJsMember();
      if (isJsMemberOne != isJsMemberOther) {
        delegateMethodDescriptorsByBridgeMethodDescriptor.put(
            accidentalOverriddenMethod, overridingMethod);
      }
    }

    return delegateMethodDescriptorsByBridgeMethodDescriptor;
  }

  /**
   * If this method is the first JsMember in the method hierarchy that exposes an existing
   * non-JsMember, returns the non-JsMember it exposes, otherwise, returns null.
   */
  private static MethodDescriptor getExposedNonJsMember(MethodDescriptor methodDescriptor) {
    if (!methodDescriptor.isOrOverridesJsMember()
        || methodDescriptor.getEnclosingClassTypeDescriptor().isInterface()
        || methodDescriptor.isStatic()
        || methodDescriptor.isConstructor()) {
      return null;
    }
    // native js type is not generated, thus it does not expose any non-js methods.
    if (methodDescriptor.getEnclosingClassTypeDescriptor().isNative()) {
      return null;
    }

    MethodDescriptor overriddenNonJsMember = null;
    for (MethodDescriptor overriddenMethod : methodDescriptor.getOverriddenMethodDescriptors()) {
      if (!overriddenMethod.isOrOverridesJsMember()) {
        overriddenNonJsMember = overriddenMethod;
      }
      if (getExposedNonJsMember(overriddenMethod) != null) {
        return null; // the overridden method has already exposed the method.
      }
    }
    return overriddenNonJsMember;
  }

  private static Method createBridgeMethod(
      TypeDescriptor targetTypeDescriptor,
      MethodDescriptor bridgeMethodDescriptor,
      MethodDescriptor forwardToMethodDescriptor) {
    return AstUtils.createForwardingMethod(
        null,
        MethodDescriptor.Builder.from(bridgeMethodDescriptor)
            .setEnclosingClassTypeDescriptor(targetTypeDescriptor)
            .build(),
        forwardToMethodDescriptor,
        "Bridge method for exposing non-JsMethod.",
        true);
  }
}
