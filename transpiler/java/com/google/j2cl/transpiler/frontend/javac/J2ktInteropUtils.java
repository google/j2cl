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

import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.getAnnotationParameterString;
import static com.google.j2cl.transpiler.frontend.javac.AnnotationUtils.isWarningSuppressed;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktDisabledAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktInAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktNameAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktOutAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktPropertyAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktPublicNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.J2ktInteropAnnotationUtils.getJ2ktThrowsAnnotation;

import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.KtTypeInfo;
import com.google.j2cl.transpiler.ast.KtVariance;
import javax.annotation.Nullable;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/** Utility functions for Kotlin Interop properties. */
public class J2ktInteropUtils {
  private J2ktInteropUtils() {}

  @Nullable
  public static KtTypeInfo getJ2ktTypeInfo(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror annotation = getJ2ktNativeAnnotation(annotatedConstruct);
    if (annotation != null) {
      String qualifiedName = getAnnotationParameterString(annotation, "name");
      String bridgeQualifiedName = getAnnotationParameterString(annotation, "bridgeName");
      String companionObject = getAnnotationParameterString(annotation, "companionName");
      return KtTypeInfo.newBuilder()
          .setQualifiedName(qualifiedName)
          .setBridgeQualifiedName(bridgeQualifiedName)
          .setCompanionQualifiedName(companionObject)
          .build();
    }

    annotation = getJ2ktPublicNativeAnnotation(annotatedConstruct);
    if (annotation != null) {
      return KtTypeInfo.newBuilder().build();
    }

    return null;
  }

  public static KtInfo getJ2ktInfo(Element element) {
    return getJ2ktInfo(element, isUninitializedWarningSuppressed(element));
  }

  private static KtInfo getJ2ktInfo(
      AnnotatedConstruct annotatedConstruct, boolean isUninitializedWarningSuppressed) {
    return KtInfo.newBuilder()
        .setProperty(isKtProperty(annotatedConstruct))
        .setName(getJ2ktName(annotatedConstruct))
        .setDisabled(isKtDisabled(annotatedConstruct))
        .setUninitializedWarningSuppressed(isUninitializedWarningSuppressed)
        .setThrows(isThrows(annotatedConstruct))
        .build();
  }

  @Nullable
  private static String getJ2ktName(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror annotation = getJ2ktNameAnnotation(annotatedConstruct);
    return annotation != null ? getAnnotationParameterString(annotation, "value") : null;
  }

  private static boolean isKtProperty(AnnotatedConstruct annotatedConstruct) {
    return getJ2ktPropertyAnnotation(annotatedConstruct) != null;
  }

  public static boolean isKtDisabled(AnnotatedConstruct annotatedConstruct) {
    return getJ2ktDisabledAnnotation(annotatedConstruct) != null;
  }

  private static boolean isThrows(AnnotatedConstruct annotatedConstruct) {
    return getJ2ktThrowsAnnotation(annotatedConstruct) != null;
  }

  public static boolean isUninitializedWarningSuppressed(Element element) {
    for (; element != null; element = element.getEnclosingElement()) {
      if (isWarningSuppressed(element, "nullness:initialization.field.uninitialized")) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static KtVariance getJ2ktVariance(AnnotatedConstruct annotatedConstruct) {
    if (getJ2ktInAnnotation(annotatedConstruct) != null) {
      return KtVariance.IN;
    } else if (getJ2ktOutAnnotation(annotatedConstruct) != null) {
      return KtVariance.OUT;
    } else {
      return null;
    }
  }
}
