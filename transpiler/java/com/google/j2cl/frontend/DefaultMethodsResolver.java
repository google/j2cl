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
import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JdtMethodUtils;
import com.google.j2cl.ast.MethodDescriptor;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implements inherited default methods as concrete forwarding methods.
 */
public class DefaultMethodsResolver {
  /**
   * Creates forwarding stubs in classes that 'inherit' a default method implementation.
   */
  public static void resolve(ITypeBinding typeBinding, JavaType javaType) {
    if (javaType.isInterface() || javaType.isAbstract()) {
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
        applicableDefaultMethodsBySignature.remove(getMethodSignature(implementedMethod));
      }
    }

    // Finally implement the methods by as forwarding stubs to the actual interface method.
    implementDefaultMethods(javaType, applicableDefaultMethodsBySignature);
  }

  private static Map<String, IMethodBinding> getApplicableDefaultMethodsBySignature(
      Set<ITypeBinding> superInterfaces) {
    // Gather all (most specific) default methods declared through the interfaces of this class.
    Map<String, IMethodBinding> applicableDefaultMethodsBySignature = new LinkedHashMap<>();
    for (ITypeBinding interfaceBinding : superInterfaces) {
      for (IMethodBinding methodBinding : interfaceBinding.getDeclaredMethods()) {
        if (!JdtUtils.isDefault(methodBinding.getModifiers())) {
          continue;
        }
        String signature = getMethodSignature(methodBinding);
        if (applicableDefaultMethodsBySignature.containsKey(signature)) {
          continue;
        }
        applicableDefaultMethodsBySignature.put(signature, methodBinding);
      }
    }
    return applicableDefaultMethodsBySignature;
  }

  private static String getMethodSignature(IMethodBinding methodBinding) {
    String key = methodBinding.getName() + "(";
    String separator = "";
    for (ITypeBinding parameterType : methodBinding.getParameterTypes()) {
      key += separator + parameterType.getErasure().getBinaryName();
      separator = ";";
    }
    key += ")";
    return key;
  }

  private static Set<ITypeBinding> collectSuperInterfaces(ITypeBinding type) {
    // Collect all super interfaces, in topological order with respect the subtype relationship;
    // i.e. subtypes always appear before supertypes.
    Set<ITypeBinding> superInterfaces =
        new TreeSet<>(
            new Comparator<ITypeBinding>() {
              @Override
              public int compare(ITypeBinding thisTypeBinding, ITypeBinding thatTypeBinding) {
                return thisTypeBinding == thatTypeBinding
                    ? 0
                    : (thisTypeBinding.isSubTypeCompatible(thatTypeBinding) ? -1 : 1);
              }
            });

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
      JavaType lambdaJavaType, Map<String, IMethodBinding> applicableDefaultMethodsBySignature) {
    // Finally implement the methods by as forwarding stubs to the actual interface method.
    for (IMethodBinding method : applicableDefaultMethodsBySignature.values()) {
      MethodDescriptor targetMethod = JdtMethodUtils.createMethodDescriptor(method);
      lambdaJavaType.addMethod(
          AstUtils.createStaticForwardingMethod(
              targetMethod, lambdaJavaType.getDescriptor(), "Default method forwarding stub."));
    }
  }

}
