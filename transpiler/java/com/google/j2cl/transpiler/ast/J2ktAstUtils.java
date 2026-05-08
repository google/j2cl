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

import java.util.List;
import javax.annotation.Nullable;

/** J2KT AST utilities. */
public class J2ktAstUtils {
  /** Construct KtInfo from the given annotations. */
  public static KtInfo getKtInfo(List<Annotation> annotations) {
    if (annotations.isEmpty()) {
      return KtInfo.NONE;
    }

    String name = null;
    boolean isProperty = false;
    boolean isDisabled = false;
    boolean isThrows = false;

    for (Annotation annotation : annotations) {
      switch (annotation.getTypeDescriptor().getQualifiedSourceName()) {
        case "javaemul.internal.annotations.KtName" -> name = annotation.getStringValue("value");
        case "javaemul.internal.annotations.KtProperty" -> isProperty = true;
        case "javaemul.internal.annotations.KtDisabled" -> isDisabled = true;
        case "com.google.j2kt.annotations.Throws" -> isThrows = true;
        default -> {}
      }
    }

    return KtInfo.newBuilder()
        .setProperty(isProperty)
        .setName(name)
        .setDisabled(isDisabled)
        .setThrows(isThrows)
        .build();
  }

  /** Returns whether given type declaration implicitly extends J2ktMonitor. */
  public static boolean implicitlyExtendsJ2ktMonitor(TypeDeclaration typeDeclaration) {
    if (!typeDeclaration.isClass()) {
      return false;
    }

    DeclaredTypeDescriptor superTypeDescriptor = typeDeclaration.getSuperTypeDescriptor();
    if (superTypeDescriptor == null || !isJavaLangObject(superTypeDescriptor)) {
      return false;
    }

    return typeDeclaration.getDeclaredMethodDescriptors().stream()
        .anyMatch(MethodDescriptor::isSynchronized);
  }

  /** Returns whether given type descriptor is subtype of J2ktMonitor, explicitly or implicitly. */
  public static boolean isSubtypeOfJ2ktMonitor(DeclaredTypeDescriptor typeDescriptor) {
    if (typeDescriptor.isSubtypeOf(TypeDescriptors.get().javaemulLangJ2ktMonitor)) {
      return true;
    }

    return typeDescriptor.getAllSuperTypesIncludingSelf().stream()
        .map(DeclaredTypeDescriptor::getTypeDeclaration)
        .anyMatch(J2ktAstUtils::implicitlyExtendsJ2ktMonitor);
  }

  /** Returns whether given type descriptor is allowed in synchronized statement expression. */
  public static boolean isValidSynchronizedStatementExpressionTypeDescriptor(
      TypeDescriptor typeDescriptor) {
    return typeDescriptor instanceof DeclaredTypeDescriptor descriptor
        && (descriptor.isSubtypeOf(TypeDescriptors.get().javaLangClass)
            || isSubtypeOfJ2ktMonitor(descriptor));
  }

  @Nullable
  public static String getObjectiveCName(HasAnnotations hasAnnotations) {
    return getAnnotationValueString(hasAnnotations, "com.google.j2objc.annotations.ObjectiveCName");
  }

  @Nullable
  public static String getSwiftName(HasAnnotations hasAnnotations) {
    return getAnnotationValueString(hasAnnotations, "com.google.j2objc.annotations.SwiftName");
  }

  @Nullable
  private static String getAnnotationValueString(
      HasAnnotations hasAnnotations, String qualifiedName) {
    Annotation annotation = hasAnnotations.getAnnotation(qualifiedName);
    return annotation == null ? null : annotation.getStringValue("value");
  }

  private J2ktAstUtils() {}
}
