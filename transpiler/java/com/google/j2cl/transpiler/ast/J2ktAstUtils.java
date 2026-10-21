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
  private static final String WEB_ASYNC_ANNOTATION_NAME =
      "com.google.calendar.v2a.shared.async.WebAsync";
  private static final String DB_ASYNC_ANNOTATION_NAME =
      "com.google.calendar.v2a.shared.async.DbAsync";

  /** Construct J2ktInfo from the given annotations. */
  public static J2ktInfo getJ2ktInfo(List<Annotation> annotations) {
    if (annotations.isEmpty()) {
      return J2ktInfo.NONE;
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

    return J2ktInfo.builder()
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

  /** Returns the @WebAsync or @DbAsync annotation on the given method declaration, or null. */
  @Nullable
  public static Annotation getWebAsyncAnnotation(MethodDescriptor methodDescriptor) {
    MethodDescriptor declaration = methodDescriptor.getDeclarationDescriptor();
    Annotation annotation = declaration.getAnnotation(WEB_ASYNC_ANNOTATION_NAME);
    return annotation != null ? annotation : declaration.getAnnotation(DB_ASYNC_ANNOTATION_NAME);
  }

  /** Returns whether the given method declaration is annotated with @WebAsync or @DbAsync. */
  public static boolean hasWebAsyncAnnotation(MethodDescriptor methodDescriptor) {
    return getWebAsyncAnnotation(methodDescriptor) != null;
  }

  /** Returns whether the given method overrides a @WebAsync or @DbAsync method. */
  public static boolean isWebAsyncOverride(MethodDescriptor methodDescriptor) {
    return methodDescriptor.getJavaOverriddenMethodDescriptors().stream()
        .anyMatch(J2ktAstUtils::hasWebAsyncAnnotation);
  }

  /** Returns whether the given method is or overrides a @WebAsync or @DbAsync method. */
  public static boolean isWebAsync(MethodDescriptor methodDescriptor) {
    return hasWebAsyncAnnotation(methodDescriptor) || isWebAsyncOverride(methodDescriptor);
  }

  /** Returns whether the given type declares any @WebAsync or @DbAsync method. */
  public static boolean isWebAsync(TypeDeclaration typeDeclaration) {
    return typeDeclaration.getDeclaredMethodDescriptors().stream()
        .anyMatch(J2ktAstUtils::isWebAsync);
  }

  private J2ktAstUtils() {}
}
