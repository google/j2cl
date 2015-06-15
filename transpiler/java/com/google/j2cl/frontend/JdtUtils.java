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

import com.google.j2cl.ast.TypeReference;
import com.google.j2cl.ast.Visibility;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility functions to manipulate JDT internal representations.
 */
public class JdtUtils {
  // JdtUtil members are all package private. Code outside frontend should not be aware of the
  // dependency on JDT.

  static String getCompilationUnitPackageName(CompilationUnit compilationUnit) {
    return compilationUnit.getPackage() == null
        ? ""
        : compilationUnit.getPackage().getName().getFullyQualifiedName();
  }

  static TypeReference createTypeReference(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    List<String> nameComponents = new LinkedList<>();
    ITypeBinding currentType = typeBinding;
    while (currentType != null) {
      nameComponents.add(0, currentType.getName());
      currentType = currentType.getDeclaringClass();
    }
    String compilationUnitSourceName = getCompilationUnitSourceName(typeBinding);

    List<String> packageComponents = new LinkedList<>();
    if (typeBinding.getPackage() != null) {
      packageComponents = Arrays.asList(typeBinding.getPackage().getNameComponents());
    }
    return TypeReference.create(packageComponents, nameComponents, compilationUnitSourceName);
  }

  static String getCompilationUnitSourceName(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return null;
    }

    // TODO: handle non-main root types.
    if (typeBinding.isMember()) {
      return getCompilationUnitSourceName(typeBinding.getDeclaringClass());
    } else {
      return typeBinding.getName();
    }
  }

  static Visibility getVisibility(int modifier) {
    if (Modifier.isPublic(modifier)) {
      return Visibility.PUBLIC;
    } else if (Modifier.isProtected(modifier)) {
      return Visibility.PROTECTED;
    } else if (Modifier.isPrivate(modifier)) {
      return Visibility.PRIVATE;
    } else {
      return Visibility.PACKAGE_PRIVATE;
    }
  }

  static boolean isFinal(int modifier) {
    return Modifier.isFinal(modifier);
  }

  private JdtUtils() {}
}
