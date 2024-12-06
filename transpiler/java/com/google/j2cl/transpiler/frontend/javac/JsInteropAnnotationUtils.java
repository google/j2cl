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
package com.google.j2cl.transpiler.frontend.javac;

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
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.findAnnotationByName;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationParameterBoolean;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationParameterString;

import java.util.List;
import java.util.Optional;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

/** Utility methods to get information about Js Interop annotations. */
public class JsInteropAnnotationUtils {

  private JsInteropAnnotationUtils() {}

  public static AnnotationMirror getJsAsyncAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_ASYNC_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsConstructorAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_CONSTRUCTOR_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsEnumAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_ENUM_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsFunctionAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_FUNCTION_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsIgnoreAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_IGNORE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsTypeAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_TYPE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsMethodAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_METHOD_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsPackageAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_PACKAGE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsPropertyAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_PROPERTY_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsOptionalAnnotation(
      ExecutableElement method, int parameterIndex) {
    return findAnnotationByName(
        method.getParameters().get(parameterIndex), JS_OPTIONAL_ANNOTATION_NAME);
  }

  public static AnnotationMirror getDoNotAutoboxAnnotation(
      ExecutableElement method, int parameterIndex) {
    return findAnnotationByName(
        method.getParameters().get(parameterIndex), DO_NOT_AUTOBOX_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJsOverlayAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, JS_OVERLAY_ANNOTATION_NAME);
  }

  public static boolean isJsNative(AnnotatedConstruct annotatedConstruct) {
    return isJsNative(getJsTypeOrJsEnumAnnotation(annotatedConstruct));
  }

  private static boolean isJsNative(AnnotationMirror annotation) {
    return getAnnotationParameterBoolean(annotation, "isNative", false);
  }

  public static boolean isUnusableByJsSuppressed(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror suppressWarningsAnnotation =
        findAnnotationByName(annotatedConstruct, SUPPRESS_WARNINGS_ANNOTATION_NAME);
    if (suppressWarningsAnnotation == null) {
      return false;
    }
    List<?> suppressions =
        AnnotationUtils.getAnnotationParameterArray(suppressWarningsAnnotation, "value");
    return suppressions.stream().map(Object::toString).anyMatch("\"unusable-by-js\""::equals);
  }

  /** The namespace specified on a package, type, method or field. */
  public static String getJsNamespace(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror annotation = getJsTypeAnnotation(annotatedConstruct);
    if (annotation == null) {
      annotation = getJsEnumAnnotation(annotatedConstruct);
    }
    if (annotation == null) {
      annotation = getJsPackageAnnotation(annotatedConstruct);
    }
    return getJsNamespace(annotation);
  }

  public static String getJsNamespace(AnnotationMirror annotation) {
    return getAnnotationParameterString(annotation, "namespace");
  }

  public static String getJsName(AnnotatedConstruct annotatedConstruct) {
    return getJsName(getJsTypeOrJsEnumAnnotation(annotatedConstruct));
  }

  public static String getJsName(AnnotationMirror annotation) {
    return getAnnotationParameterString(annotation, "name");
  }

  private static AnnotationMirror getJsTypeOrJsEnumAnnotation(
      AnnotatedConstruct annotatedConstruct) {
    return Optional.ofNullable(getJsTypeAnnotation(annotatedConstruct))
        .orElse(getJsEnumAnnotation(annotatedConstruct));
  }

  public static boolean hasCustomValue(AnnotatedConstruct annotatedConstruct) {
    return hasCustomValue(getJsEnumAnnotation(annotatedConstruct));
  }

  private static boolean hasCustomValue(AnnotationMirror annotation) {
    return getAnnotationParameterBoolean(annotation, "hasCustomValue", false);
  }
}
