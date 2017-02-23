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
package com.google.j2cl.frontend;

import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.JsInfo;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/** Implements inherited default methods as concrete forwarding methods. */
public class DefaultMethodsResolver {
  /** Creates forwarding stubs in classes that 'inherit' a default method implementation. */
  public static void resolve(ITypeBinding typeBinding, Type type) {
    if (type.isInterface() || type.isAbstract()) {
      // Only concrete classes inherit default methods. Nothing to do.
      return;
    }

    // Collect all super interfaces, in topological order with respect the subtype relationship;
    // i.e. subtypes always appear before supertypes.
    Set<ITypeBinding> superInterfaces = collectSuperInterfaces(typeBinding);

    // Gather all (most specific) default methods declared through the interfaces of this class.
    Map<String, IMethodBinding> applicableDefaultMethodsBySignature =
        getApplicableDefaultMethodsBySignature(superInterfaces);

    // Remove methods that are already implemented in the class or any of its superclasses
    for (ITypeBinding implementingClass = typeBinding.isInterface() ? null : typeBinding;
        implementingClass != null;
        implementingClass = implementingClass.getSuperclass()) {
      for (IMethodBinding implementedMethod : implementingClass.getDeclaredMethods()) {
        applicableDefaultMethodsBySignature.remove(JdtUtils.getMethodSignature(implementedMethod));
      }
    }

    // Finally implement the methods by as forwarding stubs to the actual interface method.
    implementDefaultMethods(type, applicableDefaultMethodsBySignature);
  }

  private static Map<String, IMethodBinding> getApplicableDefaultMethodsBySignature(
      Set<ITypeBinding> superInterfaces) {
    // Gather all (most specific) default methods declared through the interfaces of this class.
    Map<String, IMethodBinding> applicableDefaultMethodsBySignature = new LinkedHashMap<>();
    for (ITypeBinding interfaceBinding : superInterfaces) {
      for (IMethodBinding methodBinding : interfaceBinding.getDeclaredMethods()) {
        if (!JdtUtils.isDefaultMethod(methodBinding)) {
          continue;
        }
        String signature = JdtUtils.getMethodSignature(methodBinding);
        if (applicableDefaultMethodsBySignature.containsKey(signature)) {
          continue;
        }
        applicableDefaultMethodsBySignature.put(signature, methodBinding);
      }
    }
    return applicableDefaultMethodsBySignature;
  }

  private static Set<ITypeBinding> collectSuperInterfaces(ITypeBinding type) {
    // Collect all super interfaces, in topological order with respect the subtype relationship;
    // i.e. subtypes always appear before supertypes.
    Set<ITypeBinding> superInterfaces =
        new TreeSet<>(
            (thisTypeBinding, thatTypeBinding) ->
                thisTypeBinding == thatTypeBinding
                    ? 0
                    : (thisTypeBinding.isSubTypeCompatible(thatTypeBinding) ? -1 : 1));

    collectSuperInterfaces(superInterfaces, type);
    return superInterfaces;
  }

  private static void collectSuperInterfaces(Set<ITypeBinding> collected, ITypeBinding type) {
    if (type.isInterface()) {
      collected.add(type);
    }
    if (type.getSuperclass() != null) {
      collectSuperInterfaces(collected, type.getSuperclass());
    }
    for (ITypeBinding interfaceType : type.getInterfaces()) {
      collectSuperInterfaces(collected, interfaceType);
    }
  }

  private static void implementDefaultMethods(
      Type type, Map<String, IMethodBinding> applicableDefaultMethodsBySignature) {
    // Finally implement the methods by as forwarding stubs to the actual interface method.
    for (IMethodBinding method : applicableDefaultMethodsBySignature.values()) {
      MethodDescriptor targetMethod = JdtUtils.createMethodDescriptor(method);
      Method defaultForwardingMethod =
          AstUtils.createStaticForwardingMethod(
              targetMethod,
              type.getDeclaration().getUnsafeTypeDescriptor(),
              "Default method forwarding stub.");
      defaultForwardingMethod.setSourcePosition(type.getSourcePosition());
      type.addMethod(defaultForwardingMethod);
      if (JdtUtils.isOrOverridesJsMember(method)) {
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
                //    }                     @JsMethod default n() {}
                //                         }
                //   class A implements I, J { }
                //
                // class A in Js will be emitted as follows
                //
                //   class A {
                //    ...
                //       m()   { J.m(); }     // Overrides J.m
                //       m_m() { this.m(); }  // Overrides I.m
                //       n()   { J.n(); }     // Overrides J.n
                //       m_n() { this.n(); }  // does not override anything
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
