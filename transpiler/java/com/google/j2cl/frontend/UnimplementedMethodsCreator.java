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

import com.google.j2cl.ast.Block;
import com.google.j2cl.ast.Method;
import com.google.j2cl.ast.MethodDescriptor;
import com.google.j2cl.ast.MethodDescriptorBuilder;
import com.google.j2cl.ast.TypeDescriptor;
import com.google.j2cl.ast.Variable;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
  public static List<Method> create(ITypeBinding typeBinding) {
    List<Method> unimplementedMethods = new ArrayList<>();
    // used to avoid generating duplicate methods.
    Set<MethodDescriptor> unimplementedMethodDescriptors = new LinkedHashSet<>();

    for (IMethodBinding methodBinding : JdtUtils.getUnimplementedMethodBindings(typeBinding)) {
      MethodDescriptor unimplementedMethodDescriptor =
          createMethodDescriptorInType(methodBinding, typeBinding);
      if (unimplementedMethodDescriptors.contains(unimplementedMethodDescriptor)) {
        continue;
      }
      unimplementedMethodDescriptors.add(unimplementedMethodDescriptor);
      unimplementedMethods.add(createEmptyMethod(methodBinding, typeBinding));
    }
    return unimplementedMethods;
  }

  /**
   * Creates a MethodDescriptor in type {@code typeBinding} with the same signature of
   * the original declaration of {@code methodBinding}.
   */
  private static MethodDescriptor createMethodDescriptorInType(
      IMethodBinding methodBinding, ITypeBinding typeBinding) {
    TypeDescriptor enclosingClassTypeDescriptor = JdtUtils.createTypeDescriptor(typeBinding);

    MethodDescriptor originalMethodDescriptor = JdtUtils.createMethodDescriptor(methodBinding);
    return MethodDescriptorBuilder.from(originalMethodDescriptor)
        .enclosingClassTypeDescriptor(enclosingClassTypeDescriptor)
        .build();
  }

  /**
   * Returns an empty method in type {@code typeBinding} that has the same signature of
   * {@code methodBinding}.
   */
  private static Method createEmptyMethod(IMethodBinding methodBinding, ITypeBinding typeBinding) {
    MethodDescriptor methodDescriptor = createMethodDescriptorInType(methodBinding, typeBinding);
    List<Variable> parameters = new ArrayList<>();
    int i = 0;
    for (ITypeBinding parameterTypeBinding : methodBinding.getParameterTypes()) {
      Variable parameter =
          new Variable(
              "arg" + i++, JdtUtils.createTypeDescriptor(parameterTypeBinding), false, true);
      parameters.add(parameter);
    }
    Block body = new Block();
    return new Method(
        methodDescriptor,
        parameters,
        body,
        true,
        false,
        false,
        JdtUtils.isFinal(methodBinding.getModifiers()));
  }
}
