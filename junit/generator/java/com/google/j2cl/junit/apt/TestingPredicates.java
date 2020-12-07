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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

class TestingPredicates {
  static final Predicate<ExecutableElement> ZERO_PARAMETER_PREDICATE =
      input -> input.getParameters().isEmpty();

  static final Predicate<ExecutableElement> RETURN_TYPE_VOID_PREDICATE =
      input -> input.getReturnType().getKind() == TypeKind.VOID;

  static final Predicate<ExecutableElement> METHOD_NAME_STARTS_WITH_TEST_PREDICATE =
      input -> input.getSimpleName().toString().startsWith("test");

  // TODO(b/140131081): Code like the following should be using shared J2CL infrastructure so
  //  that the code is consistent and not duplicated.
  private static final boolean isThenable(TypeElement type) {
    if (type == null) {
      return false;
    }

    // Listenable futures are implicitly thenable in JS
    if (type.getQualifiedName()
        .contentEquals("com.google.common.util.concurrent.ListenableFuture")) {
      return true;
    }

    Predicate<ExecutableElement> isJsMethod =
        method ->
            (isAnnotationPresent(method.getEnclosingElement(), JsType.class)
                    && !isAnnotationPresent(method, JsIgnore.class))
                || isAnnotationPresent(method, JsMethod.class);

    return MoreApt.getTypeHierarchy(type).stream()
        .flatMap(t -> ElementFilter.methodsIn(t.getEnclosedElements()).stream())
        .filter(m -> !m.getModifiers().contains(Modifier.STATIC))
        .filter(TestingPredicates::isThenMethod)
        .filter(m -> m.getParameters().size() == 1 || m.getParameters().size() == 2)
        .filter(isJsMethod)
        .anyMatch(
            m ->
                m.getParameters().stream()
                    .map(ve -> MoreApt.asTypeElement(ve.asType()))
                    .allMatch(t -> t != null && isAnnotationPresent(t, JsFunction.class)));
  }

  // TODO(b/140131081): Due to having two separate implementations to decide what is a Promise
  // one for running under Jvm and the other for the running on the web, we require the method
  // to be named 'then' and not renamed with the @JsMethod annotation.
  private static boolean isThenMethod(ExecutableElement method) {
    return method.getSimpleName().contentEquals("then")
        && (!isAnnotationPresent(method, JsMethod.class)
            || method.getAnnotation(JsMethod.class).name().equals("<auto>")
            || method.getAnnotation(JsMethod.class).name().equals("then"));
  }

  static final Predicate<ExecutableElement> IS_RETURNTYPE_A_THENABLE =
      input -> isThenable(MoreApt.asTypeElement(input.getReturnType()));

  static Predicate<? super Element> hasAnnotation(final Class<? extends Annotation> annotation) {
    return element -> isAnnotationPresent(element, annotation);
  }
}
