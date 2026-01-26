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
package com.google.j2cl.transpiler.frontend.jdt;

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_DISABLED_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_IN_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_NAME_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_OBJECTIVE_C_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_OUT_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_PROPERTY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_PUBLIC_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_THROWS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.SWIFT_NAME_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.findAnnotationBindingByName;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getAnnotationBinding;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getStringAttribute;

import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;

/** Utility methods to get information about Kotlin Interop annotations. */
public class J2ktInteropAnnotationUtils {
  private J2ktInteropAnnotationUtils() {}

  @Nullable
  public static IAnnotationBinding getJ2ktNameAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_NAME_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktNativeAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_NATIVE_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktPropertyAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_PROPERTY_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktDisabledAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_DISABLED_ANNOTATION_NAME);
  }

  /** The namespace specified on a package, type, method or field. */
  // TODO(b/444296932): Remove when no longer needed.
  @Nullable
  public static String getJ2ktObjectiveCName(ITypeBinding typeBinding) {
    return getJ2ktObjectiveCName(getJ2ktObjectiveCNameAnnotation(typeBinding));
  }

  // TODO(b/444296932): Remove when no longer needed.
  @Nullable
  public static String getJ2ktObjectiveCName(IAnnotationBinding annotationBinding) {
    return getStringAttribute(annotationBinding, "value");
  }

  // TODO(b/444296932): Remove when no longer needed.
  @Nullable
  public static String getJ2ktObjectiveCName(PackageDeclaration packageDeclaration) {
    return getJ2ktObjectiveCName(
        getAnnotationBinding(packageDeclaration, J2ktInteropAnnotationUtils::isJ2ktObjectiveCName));
  }

  // TODO(b/444296932): Remove when no longer needed.
  public static boolean isJ2ktObjectiveCName(IAnnotationBinding annotationBinding) {
    return annotationBinding
        .getAnnotationType()
        .getQualifiedName()
        .equals(J2KT_OBJECTIVE_C_ANNOTATION_NAME);
  }

  public static boolean isJ2ktSwiftName(IAnnotationBinding annotationBinding) {
    return annotationBinding
        .getAnnotationType()
        .getQualifiedName()
        .equals(SWIFT_NAME_ANNOTATION_NAME);
  }

  @Nullable
  public static String getJ2ktSwiftName(PackageDeclaration packageDeclaration) {
    IAnnotationBinding annotation =
        getAnnotationBinding(packageDeclaration, J2ktInteropAnnotationUtils::isJ2ktSwiftName);
    if (annotation == null) {
      return null;
    }
    String swiftName = getJ2ktSwiftName(annotation);
    return swiftName == null ? "" : swiftName;
  }

  @Nullable
  public static String getJ2ktSwiftName(IAnnotationBinding annotationBinding) {
    return getStringAttribute(annotationBinding, "value");
  }

  @Nullable
  public static IAnnotationBinding getJ2ktObjectiveCNameAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_OBJECTIVE_C_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktInAnnotation(ITypeBinding typeVariableBinding) {
    // TODO(b/399932181): Double check the logic and comment here why we're using
    // ITypeBinding.getTypeAnnotations() instead of IBinding.getAnnotations().
    return findAnnotationBindingByName(
        typeVariableBinding.getTypeAnnotations(), J2KT_IN_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktOutAnnotation(ITypeBinding typeVariableBinding) {
    // TODO(b/399932181): Double check the logic and comment here why we're using
    // ITypeBinding.getTypeAnnotations() instead of IBinding.getAnnotations().
    return findAnnotationBindingByName(
        typeVariableBinding.getTypeAnnotations(), J2KT_OUT_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktThrowsAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_THROWS_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktPublicNativeAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_PUBLIC_NATIVE_ANNOTATION_NAME);
  }
}
