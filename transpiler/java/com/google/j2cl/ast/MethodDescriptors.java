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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class holding method descriptor creation utilities.
 */
public class MethodDescriptors {

  public static boolean isToStringMethodDescriptor(MethodDescriptor methodDescriptor) {
    return methodDescriptor.getMethodName().equals("toString")
        && methodDescriptor.getParameterTypeDescriptors().isEmpty();
  }

  /**
   * Creates a copy of the given method descriptor by adding the provided parameters to its end.
   * 
   * <p>Takes care to correctly mirror the update to any contained erased method descriptor version.
   */
  public static MethodDescriptor createModifiedCopy(
      MethodDescriptor methodDescriptor, List<TypeDescriptor> addedParameters) {
    // Add the provided parameters to the end of the existing parameters list.
    List<TypeDescriptor> parameters =
        new ArrayList<>(methodDescriptor.getParameterTypeDescriptors());
    parameters.addAll(addedParameters);
    return MethodDescriptorBuilder.from(methodDescriptor)
        .parameterTypeDescriptors(parameters)
        .build();
  }

  /**
   * Creates a static MethodDescriptor from an instance MethodDescriptor.
   *
   * <p>The static MethodDescriptor has an extra parameter as its first parameter whose type is
   * the enclosing class of {@code methodDescriptor}.
   */
  public static MethodDescriptor makeStaticMethodDescriptor(MethodDescriptor methodDescriptor) {
    if (methodDescriptor.isStatic() || methodDescriptor.isConstructor()) {
      return methodDescriptor;
    }
    List<TypeDescriptor> parameterTypeDescriptors = new ArrayList<>();
    parameterTypeDescriptors.add(methodDescriptor.getEnclosingClassTypeDescriptor());
    parameterTypeDescriptors.addAll(methodDescriptor.getParameterTypeDescriptors());
    List<TypeDescriptor> typeParameterTypeDescriptors = new ArrayList<>();
    typeParameterTypeDescriptors.addAll(methodDescriptor.getTypeParameterTypeDescriptors());
    // as the method is static, it has to copy the enclosing class's type parameters to its own.
    typeParameterTypeDescriptors.addAll(
        methodDescriptor.getEnclosingClassTypeDescriptor().getTypeArgumentDescriptors());
    return MethodDescriptorBuilder.from(methodDescriptor)
        .parameterTypeDescriptors(parameterTypeDescriptors)
        .typeParameterDescriptors(typeParameterTypeDescriptors)
        .isStatic(true)
        .build();
  }
}
