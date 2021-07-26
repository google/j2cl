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
package com.google.j2cl.junit.apt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.common.AnnotationValues;
import com.google.auto.common.MoreTypes;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Helper methods for APT processing.
 */
public class MoreApt {

  /** Returns the list of the type and its super types without a specific order. */
  public static ImmutableSet<TypeElement> getTypeHierarchy(TypeElement typeElement) {
    return ImmutableSet.copyOf(getHierarchyImpl(typeElement, true));
  }

  /** Returns the list of the class and its super classes from most to least specific order. */
  public static ImmutableList<TypeElement> getClassHierarchy(TypeElement typeElement) {
    return getHierarchyImpl(typeElement, false);
  }

  public static ImmutableList<TypeElement> getHierarchyImpl(
      TypeElement typeElement, boolean includeInterfaces) {
    ImmutableList.Builder<TypeElement> classHierarchyBuilder = new ImmutableList.Builder<>();
    for (TypeElement t = typeElement; t != null; t = asTypeElement(t.getSuperclass())) {
      classHierarchyBuilder.add(t);
      if (includeInterfaces) {
        for (TypeMirror i : t.getInterfaces()) {
          classHierarchyBuilder.addAll(getHierarchyImpl(asTypeElement(i), includeInterfaces));
        }
      }
    }
    return classHierarchyBuilder.build();
  }

  public static TypeElement asTypeElement(TypeMirror type) {
    return type.getKind() != TypeKind.DECLARED ? null : MoreTypes.asTypeElement(type);
  }

  /**
   * Returns a list of fully qualified name of a class found in an annotation.
   *
   * <p>Reading values for type Class in annotation processors causes issues, see {@link
   * Element#getAnnotation(Class)}. To work around these issues we treat the annotation value as a
   * String.
   *
   * <p>Note: If this method is invoked with an annotation and method that does not contain a
   * Collection / Array of class files it will throw an {@link IllegalArgumentException}.
   */
  public static ImmutableList<String> getClassNamesFromAnnotation(
      Element element, final Class<? extends Annotation> annotationClass, String field) {
    Optional<AnnotationValue> annotationValue = getAnnotationValue(element, annotationClass, field);
    if (!annotationValue.isPresent()) {
      return ImmutableList.<String>of();
    }
    Object value = annotationValue.get().getValue();
    checkArgument(value instanceof List, "The annotation value does not represent a list");
    @SuppressWarnings("unchecked")
    List<AnnotationValue> values = (List<AnnotationValue>) value;
    return values.stream()
        .map(input -> extractClassName(AnnotationValues.toString(input)))
        .collect(toImmutableList());
  }

  /**
   * Returns the fully qualified name of a class found in an annotation.
   *
   * <p>Reading values for type Class in annotation processors causes issues, see
   * {@link Element#getAnnotation(Class)}. To work around these issues we treat the annotation value
   * as a String.
   */
  public static Optional<String> getClassNameFromAnnotation(
      Element value, final Class<? extends Annotation> annotationClass, String field) {
    Optional<AnnotationValue> annotationValue = getAnnotationValue(value, annotationClass, field);
    if (annotationValue.isPresent()) {
      return Optional.of(extractClassName(AnnotationValues.toString(annotationValue.get())));
    }
    return Optional.absent();
  }

  private static String extractClassName(String className) {
    checkArgument(className.endsWith(".class"), className);
    return className.substring(0, className.length() - ".class".length());
  }

  /**
   * Reading annotation values that are a class causes issues, so we use this workaround to do it..
   */
  private static Optional<AnnotationValue> getAnnotationValue(
      Element element, final Class<? extends Annotation> annotationClass, String method) {
    // Quoting from Element.getAnnotation's javadoc:
    // "The annotation returned by this method could contain an element whose value is of
    // type Class. This value cannot be returned directly: information necessary to locate
    // and load a class (such as the class loader to use) is not available, and the class
    // might not be loadable at all. Attempting to read a Class object by invoking the
    // relevant method on the returned annotation will result in a MirroredTypeException, from
    // which the corresponding TypeMirror may be extracted. Similarly, attempting to read a
    // Class[]-valued element will result in a MirroredTypesException."

    for (AnnotationMirror am : element.getAnnotationMirrors()) {
      if (annotationClass.getCanonicalName().equals(am.getAnnotationType().toString())) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
            am.getElementValues().entrySet()) {
          if (method.contentEquals(entry.getKey().getSimpleName())) {
            return Optional.<AnnotationValue>of(entry.getValue());
          }
        }
      }
    }
    return Optional.absent();
  }
}
