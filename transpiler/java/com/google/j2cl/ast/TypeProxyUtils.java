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

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility functions to transpile JDT TypeBinding to J2cl TypeDescriptor.
 */
public class TypeProxyUtils {
  /**
   * Creates a TypeDescriptor from a JDT TypeBinding.
   */
  public static TypeDescriptor createTypeDescriptor(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    if (typeBinding.isArray()) {
      TypeDescriptor leafTypeDescriptor = createTypeDescriptor(typeBinding.getElementType());
      return leafTypeDescriptor.getForArray(typeBinding.getDimensions());
    }

    return TypeDescriptor.create(typeBinding);
  }

  static List<String> getPackageComponents(ITypeBinding typeBinding) {
    List<String> packageComponents = new ArrayList<>();
    if (typeBinding.getPackage() != null) {
      packageComponents = Arrays.asList(typeBinding.getPackage().getNameComponents());
    }
    return packageComponents;
  }

  static List<String> getClassComponents(ITypeBinding typeBinding) {
    List<String> classComponents = new LinkedList<>();
    if (typeBinding.isWildcardType() || typeBinding.isCapture()) {
      return Arrays.asList("?");
    }
    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      String simpleName;
      if (currentType.isLocal()) {
        // JDT binary name for local class is like package.components.EnclosingClass$1SimpleName
        // Extract the name after the last '$' as the class component here.
        String binaryName = currentType.getErasure().getBinaryName();
        simpleName = binaryName.substring(binaryName.lastIndexOf('$') + 1);
      } else if (currentType.isTypeVariable()) {
        if (currentType.getDeclaringClass() != null) {
          // If it is a class-level type variable, use the simple name (with prefix "C_") as the
          // current name component.
          simpleName = AstUtils.TYPE_VARIABLE_IN_TYPE_PREFIX + currentType.getName();
        } else {
          // If it is a method-level type variable, use the simple name (with prefix "M_") as the
          // current name component, and add declaringClass_declaringMethod as the next name
          // component, and set currentType to declaringClass for the next iteration.
          classComponents.add(0, AstUtils.TYPE_VARIABLE_IN_METHOD_PREFIX + currentType.getName());
          simpleName =
              currentType.getDeclaringMethod().getDeclaringClass().getName()
                  + "_"
                  + currentType.getDeclaringMethod().getName();
          currentType = currentType.getDeclaringMethod().getDeclaringClass();
        }
      } else {
        simpleName = currentType.getErasure().getName();
      }
      classComponents.add(0, simpleName);
      currentType = currentType.getDeclaringClass();
    }
    return classComponents;
  }

  static List<TypeDescriptor> getTypeArgumentDescriptors(ITypeBinding typeBinding) {
    List<TypeDescriptor> typeArgumentDescriptors = new ArrayList<>();
    if (typeBinding.isParameterizedType()) {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeArguments()));
    } else {
      typeArgumentDescriptors.addAll(createTypeDescriptors(typeBinding.getTypeParameters()));
      boolean isInstanceNestedClass =
          typeBinding.isNested() && !Modifier.isStatic(typeBinding.getModifiers());
      if (isInstanceNestedClass) {
        if (typeBinding.getDeclaringMethod() != null) {
          typeArgumentDescriptors.addAll(
              createTypeDescriptors(typeBinding.getDeclaringMethod().getTypeParameters()));
        }
        typeArgumentDescriptors.addAll(
            createTypeDescriptor(typeBinding.getDeclaringClass()).getTypeArgumentDescriptors());
      }
    }
    return typeArgumentDescriptors;
  }

  private static List<TypeDescriptor> createTypeDescriptors(ITypeBinding[] typeBindings) {
    return Lists.transform(
        Arrays.asList(typeBindings),
        new Function<ITypeBinding, TypeDescriptor>() {
          @Override
          public TypeDescriptor apply(ITypeBinding typeBinding) {
            return createTypeDescriptor(typeBinding);
          }
        });
  }
}
