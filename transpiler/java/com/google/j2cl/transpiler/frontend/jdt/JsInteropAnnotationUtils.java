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

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_ASYNC_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_CONSTRUCTOR_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_ENUM_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_FUNCTION_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_IGNORE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_METHOD_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OPTIONAL_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_OVERLAY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PACKAGE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_PROPERTY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.JS_TYPE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.jdt.JdtAnnotationUtils.getAnnotationBinding;

import java.util.Optional;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;

/** Utility methods to get information about Js Interop annotations. */
public class JsInteropAnnotationUtils {
  private JsInteropAnnotationUtils() {}

  @Nullable
  public static IAnnotationBinding getJsAsyncAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(methodBinding, JS_ASYNC_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsConstructorAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding, JS_CONSTRUCTOR_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsEnumAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(typeBinding, JS_ENUM_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsFunctionAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(typeBinding, JS_FUNCTION_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsIgnoreAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(methodBinding, JS_IGNORE_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsTypeAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(typeBinding, JS_TYPE_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsMethodAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(methodBinding, JS_METHOD_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsPropertyAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding, JS_PROPERTY_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsOptionalAnnotation(
      IMethodBinding methodBinding, int parameterIndex) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getParameterAnnotations(parameterIndex), JS_OPTIONAL_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsOverlayAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding, JS_OVERLAY_ANNOTATION_NAME);
  }

  @Nullable
  public static IAnnotationBinding getJsPackageAnnotation(ITypeBinding packageBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        packageBinding, JS_PACKAGE_ANNOTATION_NAME);
  }

  public static boolean isJsPackageAnnotation(IAnnotationBinding annotation) {
    return annotation.getAnnotationType().getQualifiedName().equals(JS_PACKAGE_ANNOTATION_NAME);
  }

  public static boolean isJsNative(ITypeBinding typeBinding) {
    return isJsNative(getJsTypeOrJsEnumAnnotation(typeBinding));
  }

  private static boolean isJsNative(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getBooleanAttribute(annotationBinding, "isNative", false);
  }

  public static String getJsNamespace(PackageDeclaration packageDeclaration) {
    IAnnotationBinding annotationBinding =
        getAnnotationBinding(packageDeclaration, JsInteropAnnotationUtils::isJsPackageAnnotation);
    return JsInteropAnnotationUtils.getJsNamespace(annotationBinding);
  }

  /** The namespace specified on a package, type, method or field. */
  public static String getJsNamespace(ITypeBinding typeBinding) {
    return getJsNamespace(getJsTypeOrJsEnumAnnotation(typeBinding));
  }

  public static String getJsNamespace(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getStringAttribute(annotationBinding, "namespace");
  }

  public static String getJsName(ITypeBinding typeBinding) {
    return getJsName(getJsTypeOrJsEnumAnnotation(typeBinding));
  }

  public static String getJsName(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getStringAttribute(annotationBinding, "name");
  }

  @Nullable
  private static IAnnotationBinding getJsTypeOrJsEnumAnnotation(ITypeBinding typeBinding) {
    return Optional.ofNullable(getJsTypeAnnotation(typeBinding))
        .orElse(getJsEnumAnnotation(typeBinding));
  }

  public static boolean hasCustomValue(ITypeBinding typeBinding) {
    return hasCustomValue(getJsEnumAnnotation(typeBinding));
  }

  private static boolean hasCustomValue(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getBooleanAttribute(annotationBinding, "hasCustomValue", false);
  }
}
