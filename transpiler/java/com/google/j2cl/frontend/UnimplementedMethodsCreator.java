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

import com.google.j2cl.ast.JavaType;
import com.google.j2cl.ast.JdtBindingUtils;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates unimplemented methods in abstract class.
 *
 * <p>An abstract class in Java may have unimplemented methods that are declared by its super
 * interfaces. However, in JavaScript, it requires a class tagged with 'implements' to implement all
 * of the methods defined by the interface. This class synthesizes the unimplemented methods in an
 * abstract class.
 */
public class UnimplementedMethodsCreator {
  /**
   * Returns the synthesized unimplemented methods in {@code typeBinding}.
   */
  public static void create(ITypeBinding typeBinding, JavaType javaType) {
    // Used to avoid generating duplicate methods.
    Map<String, IMethodBinding> unimplementedMethodBindingBySignature = new LinkedHashMap<>();

    for (IMethodBinding methodBinding : JdtUtils.getUnimplementedMethodBindings(typeBinding)) {
      unimplementedMethodBindingBySignature.put(
          JdtBindingUtils.getMethodSignature(methodBinding), methodBinding);
    }

    if (typeBinding.getSuperclass() != null) {
      for (IMethodBinding methodBinding :
          JdtUtils.getUnimplementedMethodBindings(typeBinding.getSuperclass())) {
        // Do not stub methods that would be stubbed anyway in the supertypes.
        unimplementedMethodBindingBySignature.remove(
            JdtBindingUtils.getMethodSignature(methodBinding));
      }
    }

    for (IMethodBinding unimplementedMethodBinding :
        unimplementedMethodBindingBySignature.values()) {
      javaType.addMethod(createEmptyMethod(unimplementedMethodBinding, typeBinding));
    }
  }

  /**
   * Creates a MethodDescriptor in type {@code typeBinding} with the same signature of
   * the original declaration of {@code methodBinding}.
   */
  private static MethodDescriptor createMethodDescriptorInType(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    TypeDescriptor enclosingClassTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);

    MethodDescriptor originalMethodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);

    MethodDescriptor newDeclarationMethodDescriptor =
        MethodDescriptor.Builder.from(originalMethodDescriptor.getDeclarationMethodDescriptor())
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .build();

    return MethodDescriptor.Builder.from(originalMethodDescriptor)
        .setDeclarationMethodDescriptor(newDeclarationMethodDescriptor)
        .setEnclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .build();
  }

  /**
   * Returns an empty method in type {@code typeBinding} that has the same signature of
   * {@code methodBinding}.
   */
  private static Method createEmptyMethod(IMethodBinding methodBinding, ITypeBinding typeBinding) {
    MethodDescriptor methodDescriptor = createMethodDescriptorInType(methodBinding, typeBinding);
    List<Variable> parameters = new ArrayList<>();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      Variable parameter =
          new Variable(
              "arg" + i,
              JdtBindingUtils.createTypeDescriptorWithNullability(
                  methodBinding.getParameterTypes()[i],
                  methodBinding.getParameterAnnotations(i),
                  JdtBindingUtils.getTypeDefaultNullability(methodBinding.getDeclaringClass())),
              false,
              true);
      parameters.add(parameter);
    }
    return Method.Builder.fromDefault()
        .setMethodDescriptor(methodDescriptor)
        .setParameters(parameters)
        .setIsAbstract(true)
        .setIsOverride(true)
        .setIsFinal(JdtBindingUtils.isFinal(methodBinding))
        .build();
  }
}
