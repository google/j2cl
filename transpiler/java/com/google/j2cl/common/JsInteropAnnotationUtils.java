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
package com.google.j2cl.common;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * Utility methods to get information about Js Interop annotations.
 */
public class JsInteropAnnotationUtils {
  private static final String JS_CONSTRUCTOR_ANNOTATION_NAME =
      "jsinterop.annotations.JsConstructor";
  private static final String JS_FUNCTION_ANNOTATION_NAME = "jsinterop.annotations.JsFunction";
  private static final String JS_IGNORE_ANNOTATION_NAME = "jsinterop.annotations.JsIgnore";
  private static final String JS_METHOD_ANNOTATION_NAME = "jsinterop.annotations.JsMethod";
  private static final String JS_OPTIONAL_ANNOTATION_NAME = "jsinterop.annotations.JsOptional";
  private static final String JS_OVERLAY_ANNOTATION_NAME = "jsinterop.annotations.JsOverlay";
  private static final String JS_PACKAGE_ANNOTATION_NAME = "jsinterop.annotations.JsPackage";
  private static final String JS_PROPERTY_ANNOTATION_NAME = "jsinterop.annotations.JsProperty";
  private static final String JS_TYPE_ANNOTATION_NAME = "jsinterop.annotations.JsType";

  private JsInteropAnnotationUtils() {}

  public static IAnnotationBinding getJsConstructorAnnotation(IMethodBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_CONSTRUCTOR_ANNOTATION_NAME);
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

  public static IAnnotationBinding getJsOverlayAnnotation(IBinding methodBinding) {
    return JdtAnnotationUtils.findAnnotationBindingByName(
        methodBinding.getAnnotations(), JS_OVERLAY_ANNOTATION_NAME);
  }

  public static boolean isJsPackageAnnotation(IAnnotationBinding annotation) {
    return annotation.getAnnotationType().getQualifiedName().equals(JS_PACKAGE_ANNOTATION_NAME);
  }

  public static boolean isNative(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getAnnotationParameterBoolean(annotationBinding, "isNative", false);
  }

  /**
   * The namespace specified on a package, type, method or field.
   */
  public static String getJsNamespace(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getAnnotationParameterString(annotationBinding, "namespace");
  }

  public static String getJsName(IAnnotationBinding annotationBinding) {
    return JdtAnnotationUtils.getAnnotationParameterString(annotationBinding, "name");
  }
}
