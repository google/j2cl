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
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtDisabledAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtInAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtNameAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtNativeAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtObjectiveCNameAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtOutAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtPropertyAnnotation;
import static com.google.j2cl.transpiler.frontend.javac.KtInteropAnnotationUtils.getKtThrowsAnnotation;

import com.google.j2cl.transpiler.ast.KtInfo;
import com.google.j2cl.transpiler.ast.KtObjcInfo;
import com.google.j2cl.transpiler.ast.KtTypeInfo;
import com.google.j2cl.transpiler.ast.KtVariance;
import javax.annotation.Nullable;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/** Utility functions for Kotlin Interop properties. */
public class KtInteropUtils {
  private KtInteropUtils() {}

  @Nullable
  public static KtObjcInfo getKtObjcInfo(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror annotation = getKtObjectiveCNameAnnotation(annotatedConstruct);
    if (annotation == null) {
      return null;
    }
    return KtObjcInfo.newBuilder()
        .setObjectiveCName(getAnnotationParameterString(annotation, "value"))
        .build();
  }

  @Nullable
  public static KtTypeInfo getKtTypeInfo(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror annotation = getKtNativeAnnotation(annotatedConstruct);
    if (annotation == null) {
      return null;
    }

    String qualifiedName = getAnnotationParameterString(annotation, "name");
    String bridgeQualifiedName = getAnnotationParameterString(annotation, "bridgeName");
    String companionObject = getAnnotationParameterString(annotation, "companionName");
    return KtTypeInfo.newBuilder()
        .setQualifiedName(qualifiedName)
        .setBridgeQualifiedName(bridgeQualifiedName)
        .setCompanionQualifiedName(companionObject)
        .build();
  }

  public static KtInfo getKtInfo(Element element) {
    // Checking for both property annotations and enclosing class annotations for uninitialized
    // warning suppressions.
    boolean isUninitializedWarningSuppressed = isUninitializedWarningSuppressed(element);
    @Nullable TypeElement declaringClass = (TypeElement) element.getEnclosingElement();
    while (declaringClass != null && !isUninitializedWarningSuppressed) {
      isUninitializedWarningSuppressed = isUninitializedWarningSuppressed(declaringClass);
      declaringClass =
          declaringClass.getEnclosingElement() instanceof TypeElement
              ? (TypeElement) declaringClass.getEnclosingElement()
              : null;
    }
    return getKtInfo(element, isUninitializedWarningSuppressed);
  }

  private static KtInfo getKtInfo(
      AnnotatedConstruct annotatedConstruct, boolean isUninitializedWarningSuppressed) {
    return KtInfo.newBuilder()
        .setProperty(isKtProperty(annotatedConstruct))
        .setName(getKtName(annotatedConstruct))
        .setDisabled(isKtDisabled(annotatedConstruct))
        .setUninitializedWarningSuppressed(isUninitializedWarningSuppressed)
        .setThrows(isThrows(annotatedConstruct))
        .build();
  }

  @Nullable
  private static String getKtName(AnnotatedConstruct annotatedConstruct) {
    AnnotationMirror annotation = getKtNameAnnotation(annotatedConstruct);
    return annotation != null ? getAnnotationParameterString(annotation, "value") : null;
  }

  private static boolean isKtProperty(AnnotatedConstruct annotatedConstruct) {
    return getKtPropertyAnnotation(annotatedConstruct) != null;
  }

  public static boolean isKtDisabled(AnnotatedConstruct annotatedConstruct) {
    return getKtDisabledAnnotation(annotatedConstruct) != null;
  }

  private static boolean isThrows(AnnotatedConstruct annotatedConstruct) {
    return getKtThrowsAnnotation(annotatedConstruct) != null;
  }

  public static boolean isUninitializedWarningSuppressed(AnnotatedConstruct annotatedConstruct) {
    return isWarningSuppressed(annotatedConstruct, "nullness:initialization.field.uninitialized");
  }

  @Nullable
  public static KtVariance getKtVariance(AnnotatedConstruct annotatedConstruct) {
    if (getKtInAnnotation(annotatedConstruct) != null) {
      return KtVariance.IN;
    } else if (getKtOutAnnotation(annotatedConstruct) != null) {
      return KtVariance.OUT;
    } else {
      return null;
    }
  }
}
