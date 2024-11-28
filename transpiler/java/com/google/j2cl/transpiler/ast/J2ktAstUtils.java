/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.transpiler.ast;

import static com.google.j2cl.transpiler.ast.TypeDescriptors.isJavaLangObject;

/** J2KT AST utilities. */
public class J2ktAstUtils {
  /** Returns whether given type declaration implicitly extends J2ktMonitor. */
  public static boolean implicitlyExtendsJ2ktMonitor(TypeDeclaration typeDeclaration) {
    if (!typeDeclaration.isClass()) {
      return false;
    }

    DeclaredTypeDescriptor superTypeDescriptor = typeDeclaration.getSuperTypeDescriptor();
    if (superTypeDescriptor == null || !isJavaLangObject(superTypeDescriptor)) {
      return false;
    }

    if (typeDeclaration.getDeclaredMethodDescriptors().stream()
        .noneMatch(MethodDescriptor::isSynchronized)) {
      return false;
    }

    return true;
  }

  /** Returns whether given type descriptor is subtype of J2ktMonitor, explicitly or implicitly. */
  public static boolean isSubtypeOfJ2ktMonitor(DeclaredTypeDescriptor typeDescriptor) {
    if (typeDescriptor.isSubtypeOf(TypeDescriptors.get().javaemulLangJ2ktMonitor)) {
      return true;
    }

    if (typeDescriptor.getAllSuperTypesIncludingSelf().stream()
        .anyMatch(it -> implicitlyExtendsJ2ktMonitor(it.getTypeDeclaration()))) {
      return true;
    }

    return false;
  }

  /** Returns whether given type descriptor is allowed in synchronized statement expression. */
  public static boolean isValidSynchronizedStatementExpressionTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    if (!(typeDescriptor instanceof DeclaredTypeDescriptor)) {
      return false;
    }

    DeclaredTypeDescriptor declaredTypeDescriptor = (DeclaredTypeDescriptor) typeDescriptor;

    if (declaredTypeDescriptor.isSubtypeOf(TypeDescriptors.get().javaLangClass)) {
      return true;
    }

    if (isSubtypeOfJ2ktMonitor(declaredTypeDescriptor)) {
      return true;
    }

    return false;
  }

  private J2ktAstUtils() {}
}
