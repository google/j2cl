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

import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_DISABLED_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_IN_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_NAME_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_OBJECTIVE_C_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_OUT_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_PROPERTY_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_PUBLIC_NATIVE_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.J2KT_THROWS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.common.FrontendConstants.SUPPRESS_WARNINGS_ANNOTATION_NAME;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.findAnnotationByName;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationName;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationParameterString;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;

/** Utility methods to get information about Kotlin Interop annotations. */
public class J2ktInteropAnnotationUtils {
  private J2ktInteropAnnotationUtils() {}

  public static AnnotationMirror getJ2ktNameAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_NAME_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktNativeAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_NATIVE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktPropertyAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_PROPERTY_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktDisabledAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_DISABLED_ANNOTATION_NAME);
  }

  public static String getJ2ktObjectiveCName(AnnotationMirror annotation) {
    return getAnnotationParameterString(annotation, "value");
  }

  public static String getJ2ktObjectiveCName(AnnotatedConstruct annotatedConstruct) {
    return getJ2ktObjectiveCName(
        getAnnotation(annotatedConstruct, J2ktInteropAnnotationUtils::isJ2ktObjectiveCName));
  }

  public static boolean isJ2ktObjectiveCName(AnnotationMirror annotation) {
    return getAnnotationName(annotation).equals(J2KT_OBJECTIVE_C_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktObjectiveCNameAnnotation(
      AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_OBJECTIVE_C_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktInAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_IN_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktOutAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_OUT_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktThrowsAnnotation(AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_THROWS_ANNOTATION_NAME);
  }

  public static AnnotationMirror getJ2ktPublicNativeAnnotation(
      AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, J2KT_PUBLIC_NATIVE_ANNOTATION_NAME);
  }

  public static AnnotationMirror getSuppressWarningsAnnotation(
      AnnotatedConstruct annotatedConstruct) {
    return findAnnotationByName(annotatedConstruct, SUPPRESS_WARNINGS_ANNOTATION_NAME);
  }
}
