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
package com.google.j2cl.frontend.javac;

import java.util.List;
import java.util.Map;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/** Utility functions to process annotations. */
public class AnnotationUtils {

  static AnnotationMirror findAnnotationBindingByName(
      List<? extends AnnotationMirror> annotations, String name) {
    if (annotations == null) {
      return null;
    }
    for (AnnotationMirror annotationBinding : annotations) {
      if (((TypeElement) annotationBinding.getAnnotationType().asElement())
          .getQualifiedName()
          .contentEquals(name)) {
        return annotationBinding;
      }
    }
    return null;
  }

  static String getAnnotationParameterString(AnnotationMirror annotationMirror, String paramName) {
    if (annotationMirror == null) {
      return null;
    }
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> member :
        annotationMirror.getElementValues().entrySet()) {
      if (member.getKey().getSimpleName().contentEquals(paramName)) {
        return (String) member.getValue().getValue();
      }
    }
    return null;
  }

  static List<?> getAnnotationParameterArray(AnnotationMirror annotationMirror, String paramName) {
    if (annotationMirror == null) {
      return null;
    }
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> member :
        annotationMirror.getElementValues().entrySet()) {
      if (member.getKey().getSimpleName().contentEquals(paramName)) {
        if (member.getValue().getValue() instanceof List) {
          return (List<?>) member.getValue().getValue();
        }
      }
    }
    return null;
  }

  static boolean getAnnotationParameterBoolean(
      AnnotationMirror annotationMirror, String paramName, boolean defaultValue) {
    if (annotationMirror == null) {
      return defaultValue;
    }
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> member :
        annotationMirror.getElementValues().entrySet()) {
      if (member.getKey().getSimpleName().contentEquals(paramName)) {
        return (Boolean) member.getValue().getValue();
      }
    }
    return defaultValue;
  }

  /** Returns true if the construct is annotated with {@code annotationSourceName}. */
  static boolean hasAnnotation(AnnotatedConstruct construct, String annotationSourceName) {
    return findAnnotationBindingByName(construct.getAnnotationMirrors(), annotationSourceName)
        != null;
  }
}
