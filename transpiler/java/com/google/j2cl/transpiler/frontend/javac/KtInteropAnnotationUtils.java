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
package com.google.j2cl.transpiler.frontend.javac;

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_DISABLED_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_IN_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_NAME_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_OBJECTIVE_C_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_OUT_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_PROPERTY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.KT_THROWS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.SUPPRESS_WARNINGS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.findAnnotationByName;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationName;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationParameterString;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;

/** Utility methods to get information about Kotlin Interop annotations. */
public class KtInteropAnnotationUtils {
  private KtInteropAnnotationUtils() {}

  public static AnnotationMirror getKtNameAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_NAME_ANNOTATION_NAME);
  }

  public static AnnotationMirror getKtNativeAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_NATIVE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getKtPropertyAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_PROPERTY_ANNOTATION_NAME);
  }

  public static AnnotationMirror getKtDisabledAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_DISABLED_ANNOTATION_NAME);
  }

  public static String getKtObjectiveCName(AnnotationMirror annotation) {
    return getAnnotationParameterString(annotation, "value");
  }

  public static String getKtObjectiveCName(AnnotatedConstruct annotatedConstruct) {
    return getKtObjectiveCName(
        getAnnotation(annotatedConstruct, KtInteropAnnotationUtils::isKtObjectiveCName));
  }

  public static boolean isKtObjectiveCName(AnnotationMirror annotation) {
    return getAnnotationName(annotation).equals(KT_OBJECTIVE_C_NAME);
  }

  public static AnnotationMirror getKtObjectiveCNameAnnotation(
      AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_OBJECTIVE_C_NAME);
  }

  public static AnnotationMirror getKtInAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_IN_ANNOTATION_NAME);
  }

  public static AnnotationMirror getKtOutAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_OUT_ANNOTATION_NAME);
  }

  public static AnnotationMirror getKtThrowsAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, KT_THROWS_ANNOTATION_NAME);
  }

  public static AnnotationMirror getSuppressWarningsAnnotation(
      AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, SUPPRESS_WARNINGS_ANNOTATION_NAME);
  }
}
