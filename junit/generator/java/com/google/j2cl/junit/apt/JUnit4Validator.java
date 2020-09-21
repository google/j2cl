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
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.junit.async.Timeout;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

class JUnit4Validator extends BaseValidator {

  public JUnit4Validator(ErrorReporter errorReporter) {
    super(errorReporter);
  }

  public boolean validateJUnit4MethodAndClass(TypeElement typeElement) {
    if (!validateNotInnerClass(typeElement)) {
      return false;
    }

    boolean isValid = true;
    for (TypeElement type : MoreApt.getClassHierarchy(typeElement)) {
      isValid &= validateType(type);
    }

    return isValid;
  }

  public boolean hasAnyMethodsAnnotatedWithTest(TypeElement typeElement) {
    return MoreApt.getClassHierarchy(typeElement)
        .stream()
        .flatMap(input -> ElementFilter.methodsIn(input.getEnclosedElements()).stream())
        .anyMatch(TestingPredicates.hasAnnotation(Test.class));
  }

  private final boolean validateType(TypeElement type) {
    boolean isValid = true;
    if (isAnnotationPresent(type, Ignore.class)) {
      errorReporter.report(ErrorMessage.IGNORE_ON_TYPE, type);
      isValid = false;
    }
    for (ExecutableElement executableElement : getAllTestMethods(type)) {
      isValid &= validateMethodPublic(executableElement);
      isValid &= validateMethodNotStatic(executableElement);
      isValid &= validateMethodNoArguments(executableElement);
      isValid &= validateAsync(executableElement);
    }
    return isValid;
  }

  private boolean validateMethodNotStatic(ExecutableElement executableElement) {
    if (executableElement.getModifiers().contains(Modifier.STATIC)) {
      errorReporter.report(ErrorMessage.IS_STATIC, executableElement);
      return false;
    }
    return true;
  }

  private boolean validateAsync(ExecutableElement executableElement) {
    Test testAnnotation = executableElement.getAnnotation(Test.class);
    Timeout timeoutAnnotation = executableElement.getAnnotation(Timeout.class);
    long timeout =
        testAnnotation != null
            ? testAnnotation.timeout()
            : timeoutAnnotation != null ? timeoutAnnotation.value() : 0;

    boolean isValid = true;

    if (testAnnotation != null && timeoutAnnotation != null) {
      errorReporter.report(ErrorMessage.TEST_HAS_TIMEOUT_ANNOTATION, executableElement);
      isValid = false;
    }

    if (TestingPredicates.IS_RETURNTYPE_A_THENABLE.test(executableElement)) {
      // if we are an async test, we need the timeout attribute
      if (timeout <= 0L) {
        errorReporter.report(ErrorMessage.ASYNC_HAS_NO_TIMEOUT, executableElement);
        isValid = false;
      }
      // block usage of expected exception with async tests
      if (MoreApt.getClassNameFromAnnotation(executableElement, Test.class, "expected")
          .isPresent()) {
        errorReporter.report(ErrorMessage.ASYNC_HAS_EXPECTED_EXCEPTION, executableElement);
        isValid = false;
      }
    } else {
      // block usage of timeout with non async tests
      if (timeout != 0L) {
        errorReporter.report(ErrorMessage.NON_ASYNC_HAS_TIMEOUT, executableElement);
        isValid = false;
      }
      if (!TestingPredicates.RETURN_TYPE_VOID_PREDICATE.test(executableElement)) {
        errorReporter.report(ErrorMessage.NON_PROMISE_RETURN, executableElement);
        isValid = false;
      }
    }
    return isValid;
  }

  private static ImmutableList<ExecutableElement> getAllTestMethods(TypeElement typeElement) {
    return ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        .filter(
            Predicates.or(
                TestingPredicates.hasAnnotation(Test.class),
                TestingPredicates.hasAnnotation(Before.class),
                TestingPredicates.hasAnnotation(After.class)))
        .collect(toImmutableList());
  }
}
