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

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.DO_NOT_AUTOBOX_ANNOTATION_NAME;
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
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.SUPPRESS_WARNINGS_ANNOTATION_NAME;
import static java.util.Arrays.stream;

import java.util.Optional;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/** Utility methods to get information about Js Interop annotations. */
public class JsInteropAnnotationUtils {
  private JsInteropAnnotationUtils() {}

  public static IAnnotationBinding getJsAsyncAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_ASYNC_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsConstructorAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_CONSTRUCTOR_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsEnumAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotations(), JS_ENUM_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsFunctionAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotations(), JS_FUNCTION_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsIgnoreAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_IGNORE_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsTypeAnnotation(ITypeBinding typeBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        typeBinding.getAnnotations(), JS_TYPE_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsMethodAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_METHOD_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsPropertyAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_PROPERTY_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsOptionalAnnotation(
      IMethodBinding methodBinding, int parameterIndex) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getParameterAnnotations(parameterIndex), JS_OPTIONAL_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getDoNotAutoboxAnnotation(
      IMethodBinding methodBinding, int parameterIndex) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getParameterAnnotations(parameterIndex), DO_NOT_AUTOBOX_ANNOTATION_NAME);
  }

  public static IAnnotationBinding getJsOverlayAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_OVERLAY_ANNOTATION_NAME);
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

  public static boolean isUnusableByJsSuppressed(IBinding binding) {
    IAnnotationBinding suppressWarningsBinding =
        JdtAnnotationUtils.findAnnotationBindingByName(
            binding.getAnnotations(), SUPPRESS_WARNINGS_ANNOTATION_NAME);
    if (suppressWarningsBinding == null) {
      return false;
    }
    Object[] suppressions = JdtAnnotationUtils.getArrayAttribute(suppressWarningsBinding, "value");
    return stream(suppressions).anyMatch("unusable-by-js"::equals);
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
