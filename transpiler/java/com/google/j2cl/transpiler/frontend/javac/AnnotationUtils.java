/*
 * Copyright 2015 Google Inc.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getSuppressWarningsAnnotation;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.transpiler.frontend.common.Nullability;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;

/** Utility functions to process annotations. */
public final class AnnotationUtils {

  @Nullable
  static AnnotationMirror findAnnotationByName(AnnotatedConstruct annotatedConstruct, String name) {
    return getAnnotation(annotatedConstruct, a -> getAnnotationName(a).equals(name));
  }

  static String getAnnotationName(AnnotationMirror annotation) {
    return ((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().toString();
  }

  @Nullable
  static String getAnnotationParameterString(AnnotationMirror annotation, String paramName) {
    var parameterValue = getAnnotationParameterValue(annotation, paramName);
    return parameterValue != null ? (String) parameterValue : null;
  }

  static boolean getAnnotationParameterBoolean(
      AnnotationMirror annotation, String paramName, boolean defaultValue) {
    var parameterValue = getAnnotationParameterValue(annotation, paramName);
    return parameterValue != null ? (Boolean) parameterValue : defaultValue;
  }

  @Nullable
  static ImmutableList<?> getAnnotationParameterArray(
      AnnotationMirror annotation, String paramName) {
    var parameterValue = getAnnotationParameterValue(annotation, paramName);

    return parameterValue instanceof List<?> list
        ? list.stream()
            .map(AnnotationValue.class::cast)
            .map(AnnotationValue::getValue)
            .collect(toImmutableList())
        : null;
  }

  @Nullable
  static Object getAnnotationParameterValue(AnnotationMirror annotation, String paramName) {
    if (annotation == null) {
      return null;
    }
    return annotation.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().contentEquals(paramName))
        .map(Entry::getValue)
        .map(AnnotationValue::getValue)
        .collect(toOptional())
        .orElse(null);
  }

  /** Returns true if the construct is annotated with {@code annotationSourceName}. */
  static boolean hasAnnotation(AnnotatedConstruct construct, String annotationSourceName) {
    return findAnnotationByName(construct, annotationSourceName) != null;
  }

  public static boolean isWarningSuppressed(AnnotatedConstruct annotatedConstruct, String warning) {
    var annotation = getSuppressWarningsAnnotation(annotatedConstruct);
    if (annotation == null) {
      return false;
    }

    var suppressions = getAnnotationParameterArray(annotation, "value");
    return suppressions.contains(warning);
  }

  @Nullable
  public static AnnotationMirror getAnnotation(
      AnnotatedConstruct annotatedConstruct, Predicate<? super AnnotationMirror> whichAnnotation) {

    return annotatedConstruct.getAnnotationMirrors().stream()
        .filter(whichAnnotation)
        .findFirst()
        .orElse(null);
  }

  public static boolean hasNullMarkedAnnotation(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror annotation =
        getAnnotation(
            annotatedConstruct, a -> Nullability.isNullMarkedAnnotation(getAnnotationName(a)));
    return annotation != null;
  }

  private AnnotationUtils() {}
}
