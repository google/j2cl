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
  // TODO(simionato): Verify that it is always correct to add the same parameters to the method
  // descriptor and its declaration.
  public static MethodDescriptor createModifiedCopy(
      MethodDescriptor methodDescriptor, List<TypeDescriptor> addedParameters) {
    // Add the provided parameters to the end of the existing parameters list.
    List<TypeDescriptor> parameters =
        new ArrayList<>(methodDescriptor.getParameterTypeDescriptors());
    parameters.addAll(addedParameters);

    MethodDescriptor.Builder methodBuilder = MethodDescriptor.Builder.from(methodDescriptor)
        .parameterTypeDescriptors(parameters);

    if (methodDescriptor != methodDescriptor.getDeclarationMethodDescriptor()) {
      methodBuilder.declarationMethodDescriptor(
          createModifiedCopy(methodDescriptor.getDeclarationMethodDescriptor(), addedParameters));
    }

    return methodBuilder.build();
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
    TypeDescriptor enclosingClassTypeDescriptor =
        methodDescriptor.getEnclosingClassTypeDescriptor();
    List<TypeDescriptor> parameterTypeDescriptors = new ArrayList<>();
    parameterTypeDescriptors.add(enclosingClassTypeDescriptor);
    parameterTypeDescriptors.addAll(methodDescriptor.getParameterTypeDescriptors());

    List<TypeDescriptor> typeParameterTypeDescriptors = new ArrayList<>();
    typeParameterTypeDescriptors.addAll(methodDescriptor.getTypeParameterTypeDescriptors());
    // as the method is static, it has to copy the enclosing class's type parameters to its own.
    typeParameterTypeDescriptors.addAll(
        methodDescriptor.getEnclosingClassTypeDescriptor().getTypeArgumentDescriptors());

    MethodDescriptor.Builder methodBuilder =
        MethodDescriptor.Builder.from(methodDescriptor)
            .parameterTypeDescriptors(parameterTypeDescriptors)
            .typeParameterTypeDescriptors(typeParameterTypeDescriptors)
            .isStatic(true);

    if (methodDescriptor != methodDescriptor.getDeclarationMethodDescriptor()) {
      MethodDescriptor declarationMethodDescriptor =
          methodDescriptor.getDeclarationMethodDescriptor();

      List<TypeDescriptor> methodDeclarationParameterTypeDescriptors = new ArrayList<>();
      methodDeclarationParameterTypeDescriptors.add(enclosingClassTypeDescriptor);
      methodDeclarationParameterTypeDescriptors.addAll(
          declarationMethodDescriptor.getParameterTypeDescriptors());

      MethodDescriptor newDeclarationMethodDescriptor =
          MethodDescriptor.Builder.from(makeStaticMethodDescriptor(declarationMethodDescriptor))
              .parameterTypeDescriptors(methodDeclarationParameterTypeDescriptors)
              .build();
      methodBuilder.declarationMethodDescriptor(
          makeStaticMethodDescriptor(newDeclarationMethodDescriptor));
    }
    return methodBuilder.build();
  }
}
