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
import java.util.List;

/**
 * Creates bridge methods for instance JsMethods.
 *
 * <p>The bridge method for an instance JsMethod has the regular mangled name and delegates to the
 * JsMethod.
 */
public class JsBridgeMethodsCreator {
  /**
   * Returns generated bridge methods.
   */
  public static List<Method> create(ITypeBinding typeBinding) {
    List<Method> generatedBridgeMethods = new ArrayList<>();
    for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
      if (!exposesNonJsMethod(methodBinding)) {
        continue;
      }
      generatedBridgeMethods.add(createBridgeMethod(methodBinding));
    }
    return generatedBridgeMethods;
  }

  /**
   * Returns {@code true} if this method is the first JsMethod in the method hierarchy that exposes
   * an existing non-JsMethod inside a class.
   */
  private static boolean exposesNonJsMethod(IMethodBinding methodBinding) {
    if (!JsInteropUtils.isJsMethod(methodBinding)
        || JdtUtils.isStatic(methodBinding.getModifiers())
        || methodBinding.isConstructor()) {
      return false;
    }
    // native js type is not generated, thus it does not expose any non-js methods.
    if (JsInteropUtils.isNative(
        JsInteropUtils.getJsTypeAnnotation(methodBinding.getDeclaringClass()))) {
      return false;
    }
    boolean overridesNonJsMethod = false;
    for (IMethodBinding overriddenMethod : JdtUtils.getOverriddenMethods(methodBinding)) {
      if (!JsInteropUtils.isJsMethod(overriddenMethod)) {
        overridesNonJsMethod = true;
      }
      if (exposesNonJsMethod(overriddenMethod)) {
        return false; // the overridden method has already exposed the method.
      }
    }
    return overridesNonJsMethod;
  }

  private static Method createBridgeMethod(IMethodBinding methodBinding) {
    MethodDescriptor jsMethodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
    MethodDescriptor originalMethodDescriptor =
        MethodDescriptorBuilder.from(jsMethodDescriptor)
            .isRaw(false)
            .jsMethodName(null)
            .jsMethodNamespace(null)
            .build();
    return AstUtils.createForwardingMethod(originalMethodDescriptor, jsMethodDescriptor);
  }
}
