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
package com.google.j2cl.junit.apt;

import static com.google.auto.common.MoreElements.isAnnotationPresent;

import com.google.common.base.Predicate;
import java.lang.annotation.Annotation;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

class TestingPredicates {
  static final Predicate<ExecutableElement> ZERO_PARAMETER_PREDICATE =
      input -> input.getParameters().isEmpty();

  static final Predicate<ExecutableElement> RETURN_TYPE_VOID_PREDICATE =
      input -> input.getReturnType().getKind() == TypeKind.VOID;

  static final Predicate<ExecutableElement> METHOD_NAME_STARTS_WITH_TEST_PREDICATE =
      input -> input.getSimpleName().toString().startsWith("test");

  private static final boolean isPromise(TypeElement type) {
    if (type == null) {
      return false;
    }

    return MoreApt.getTypeHierarchy(type).stream()
        .filter(t -> isAnnotationPresent(t, JsType.class))
        .flatMap(t -> ElementFilter.methodsIn(t.getEnclosedElements()).stream())
        .filter(m -> m.getSimpleName().contentEquals("then"))
        .filter(m -> m.getParameters().size() == 1 || m.getParameters().size() == 2)
        .anyMatch(
            m ->
                m.getParameters().stream()
                    .map(ve -> MoreApt.asTypeElement(ve.asType()))
                    .allMatch(t -> t != null && isAnnotationPresent(t, JsFunction.class)));
  }

  static final Predicate<ExecutableElement> IS_RETURNTYPE_A_PROMISE =
      input -> isPromise(MoreApt.asTypeElement(input.getReturnType()));

  static Predicate<? super Element> hasAnnotation(final Class<? extends Annotation> annotation) {
    return element -> isAnnotationPresent(element, annotation);
  }
}
