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

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_THROWS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_DISABLED_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_IN_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_NAME_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_OBJECTIVE_C_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_OUT_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_PROPERTY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.findAnnotationBindingByName;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getAnnotationBinding;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getStringAttribute;

import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;

/** Utility methods to get information about Kotlin Interop annotations. */
public class KtInteropAnnotationUtils {
  private KtInteropAnnotationUtils() {}

  @Nullable
  public static IAnnotationBinding getKtNameAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, KT_NAME_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getKtNativeAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, KT_NATIVE_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getKtPropertyAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, KT_PROPERTY_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getKtDisabledAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, KT_DISABLED_ANNOTATION_NAME);
  }

  /** The namespace specified on a package, type, method or field. */
  @Nullable
  public static String getKtObjectiveCName(ITypeBinding typeBinding) {
    return getKtObjectiveCName(getKtObjectiveCNameAnnotation(typeBinding));
  }

  @Nullable
  public static String getKtObjectiveCName(IAnnotationBinding annotationBinding) {
    return getStringAttribute(annotationBinding, "value");
  }

  @Nullable
  public static String getKtObjectiveCName(PackageDeclaration packageDeclaration) {
    return getKtObjectiveCName(
        getAnnotationBinding(packageDeclaration, KtInteropAnnotationUtils::isKtObjectiveCName));
  }

  public static boolean isKtObjectiveCName(IAnnotationBinding annotationBinding) {
    return annotationBinding.getAnnotationType().getQualifiedName().equals(KT_OBJECTIVE_C_NAME);
  }

  @Nullable
  public static IAnnotationBinding getKtObjectiveCNameAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, KT_OBJECTIVE_C_NAME);
  }

  @Nullable
  public static IAnnotationBinding getKtInAnnotation(ITypeBinding typeVariableBinding) {
    // TODO(b/399932181): Double check the logic and comment here why we're using
    // ITypeBinding.getTypeAnnotations() instead of IBinding.getAnnotations().
    return findAnnotationBindingByName(
        typeVariableBinding.getTypeAnnotations(), KT_IN_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getKtOutAnnotation(ITypeBinding typeVariableBinding) {
    // TODO(b/399932181): Double check the logic and comment here why we're using
    // ITypeBinding.getTypeAnnotations() instead of IBinding.getAnnotations().
    return findAnnotationBindingByName(
        typeVariableBinding.getTypeAnnotations(), KT_OUT_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktThrowsAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_THROWS_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJ2ktNativeAnnotation(IBinding binding) {
    return findAnnotationBindingByName(binding, J2KT_NATIVE_ANNOTATION_NAME);
  }
}
