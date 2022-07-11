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

import com.google.auto.common.MoreTypes;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.j2cl.junit.async.Timeout;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.AfterParam;
import org.junit.runners.Parameterized.BeforeParam;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

class JUnit4Validator extends BaseValidator {

  private final Types typeUtils;
  private final Elements elementUtils;

  public JUnit4Validator(ErrorReporter errorReporter, Types typeUtils, Elements elementUtils) {
    super(errorReporter);
    this.typeUtils = typeUtils;
    this.elementUtils = elementUtils;
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
    return MoreApt.getClassHierarchy(typeElement).stream()
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
      isValid &= validateMemberIsPublic(executableElement);
      isValid &= validateMemberInstanceOrStatic(executableElement);
      isValid &= validateMethodNoArguments(executableElement);
      isValid &= validateMethodReturnType(executableElement);
    }
    ImmutableList<VariableElement> variableElements = getAllTestParameters(type);
    isValid &= validateParameters(variableElements);
    return isValid;
  }

  private boolean validateParameters(ImmutableList<VariableElement> variableElements) {
    boolean isValid = true;
    int[] usedAnnotatedIndices = new int[variableElements.size()];
    for (VariableElement each : variableElements) {
      int index = each.getAnnotation(Parameter.class).value();
      if (index < 0 || index > variableElements.size() - 1) {
        errorReporter.report(
            ErrorMessage.INVALID_PARAMETER_VALUE,
            index,
            variableElements.size(),
            variableElements.size() - 1);
        isValid = false;
      } else {
        usedAnnotatedIndices[index]++;
        isValid &= validateMemberIsPublic(each);
        isValid &= validateMemberIsInstance(each);
        isValid &= validateMemberIsNonFinal(each);
      }
    }
    for (int index = 0; index < usedAnnotatedIndices.length; index++) {
      int numberOfUse = usedAnnotatedIndices[index];
      if (numberOfUse == 0) {
        errorReporter.report(ErrorMessage.MISSING_PARAMETER, index);
        isValid = false;
      } else if (numberOfUse > 1) {
        errorReporter.report(ErrorMessage.DUPLICATE_PARAMETER, index, numberOfUse);
        isValid = false;
      }
    }
    return isValid;
  }

  private boolean validateMemberInstanceOrStatic(Element element) {
    boolean isStatic = element.getModifiers().contains(Modifier.STATIC);

    if (needsToBeStaticMethod(element) != isStatic) {
      errorReporter.report(isStatic ? ErrorMessage.IS_STATIC : ErrorMessage.NON_STATIC, element);
      return false;
    }
    return true;
  }

  private boolean needsToBeStaticMethod(Element element) {
    return hasClassSetupAnnotation(element)
        || isAnnotationPresent(element, Parameters.class)
        || isAnnotationPresent(element, BeforeParam.class)
        || isAnnotationPresent(element, AfterParam.class);
  }

  // TODO(b/235234450): clean up validation for different types of methods
  private boolean validateMethodReturnType(ExecutableElement executableElement) {
    if (isAnnotationPresent(executableElement, Parameters.class)) {
      if (!isReturnTypeIterableOrArray(executableElement)) {
        errorReporter.report(ErrorMessage.NON_ITERABLE_OR_ARRAY_RETURN, executableElement);
        return false;
      }
      return true;
    }

    boolean isValid = true;
    Test testAnnotation = executableElement.getAnnotation(Test.class);
    Timeout timeoutAnnotation = executableElement.getAnnotation(Timeout.class);
    long timeout =
        testAnnotation != null
            ? testAnnotation.timeout()
            : timeoutAnnotation != null ? timeoutAnnotation.value() : 0;

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

  private boolean isReturnTypeIterableOrArray(ExecutableElement executableElement) {

    return isArrayOfObject(executableElement)
        || isIterable(MoreApt.asTypeElement(executableElement.getReturnType()));
  }

  private final boolean isArrayOfObject(ExecutableElement executableElement) {
    TypeMirror typeMirror = executableElement.getReturnType();
    return typeMirror.getKind() == TypeKind.ARRAY
        && !MoreTypes.asArray(typeMirror).getComponentType().getKind().isPrimitive();
  }

  private final boolean isIterable(TypeElement type) {
    if (type == null) {
      return false;
    }

    return typeUtils.isSubtype(
        typeUtils.erasure(type.asType()),
        typeUtils.erasure(elementUtils.getTypeElement(Iterable.class.getName()).asType()));
  }

  private boolean validateMemberIsInstance(Element element) {
    if (element.getModifiers().contains(Modifier.STATIC)) {
      errorReporter.report(ErrorMessage.IS_STATIC, element);
      return false;
    }
    return true;
  }

  private static ImmutableList<ExecutableElement> getAllTestMethods(TypeElement typeElement) {
    return ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        .filter(
            Predicates.or(
                TestingPredicates.hasAnnotation(BeforeClass.class),
                TestingPredicates.hasAnnotation(AfterClass.class),
                TestingPredicates.hasAnnotation(Parameters.class),
                TestingPredicates.hasAnnotation(Test.class),
                TestingPredicates.hasAnnotation(Before.class),
                TestingPredicates.hasAnnotation(After.class)))
        .collect(toImmutableList());
  }

  private static boolean hasClassSetupAnnotation(Element element) {
    return isAnnotationPresent(element, BeforeClass.class)
        || isAnnotationPresent(element, AfterClass.class);
  }

  private static ImmutableList<VariableElement> getAllTestParameters(TypeElement typeElement) {
    return ElementFilter.fieldsIn(typeElement.getEnclosedElements()).stream()
        .filter(TestingPredicates.hasAnnotation(Parameter.class))
        .collect(toImmutableList());
  }
}
