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
    // We consider a type to be working under the following conditions:
    //
    //  - It is a JsType
    //  - It has a then method with either one or two parameters
    //  - All the parameters of the then methods are annotated with @JsFunction
    //
    // For now we do not take a look at the hierarchy of the type, so we
    // could be missing then methods in parent classes or interfaces. Since in tests
    // one can always use the base type this is probably fine.
    if (type == null || !isAnnotationPresent(type, JsType.class)) {
      return false;
    }

    return ElementFilter.methodsIn(type.getEnclosedElements())
        .stream()
        .filter(m -> m.getSimpleName().contentEquals("then"))
        .filter(m -> m.getParameters().size() == 1 || m.getParameters().size() == 2)
        .anyMatch(
            m ->
                m.getParameters()
                    .stream()
                    .map(ve -> MoreApt.asTypeElement(ve.asType()))
                    .allMatch(t -> t != null && isAnnotationPresent(t, JsFunction.class)));
  }

  static final Predicate<ExecutableElement> IS_RETURNTYPE_A_PROMISE =
      input -> isPromise(MoreApt.asTypeElement(input.getReturnType()));

  static Predicate<Element> hasAnnotation(final Class<? extends Annotation> annotation) {
    return input -> isAnnotationPresent(input, annotation);
  }
}
