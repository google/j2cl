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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.common.MoreElements;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import junit.framework.TestCase;

class JUnit3Validator extends BaseValidator {

  public JUnit3Validator(ErrorReporter errorReporter) {
    super(errorReporter);
  }

  public boolean isJUnit3Suite(TypeElement typeElement) {
    return ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        .filter(MoreElements.hasModifiers(Sets.newHashSet(Modifier.STATIC, Modifier.PUBLIC)))
        .anyMatch(
            method -> {
              if (!method.getSimpleName().contentEquals("suite")) {
                return false;
              }

              if (!method.getParameters().isEmpty()) {
                return false;
              }

              TypeElement returnElement = MoreApt.asTypeElement(method.getReturnType());
              if (returnElement == null) {
                return false;
              }

              return isTestClass(returnElement) || isJunit3TestCase(returnElement);
            });
  }

  public boolean isJunit3TestCase(TypeElement typeElement) {
    return hasTestCaseAsParentClass(typeElement);
  }

  public boolean validateJunit3Test(TypeElement typeElement) {
    if (!validateNotInnerClass(typeElement)) {
      return false;
    }

    boolean isValid = true;
    for (TypeElement type : MoreApt.getClassHierarchy(typeElement)) {
      isValid &= validateMethods(getAllJUnit3TestMethods(type));
    }
    return isValid;
  }

  private final boolean validateMethods(List<ExecutableElement> methods) {
    boolean isValid = true;
    for (ExecutableElement executableElement : methods) {
      isValid &= validateMemberIsPublic(executableElement);
      isValid &= validateMethodNoArguments(executableElement);
      isValid &= validateMethodVoidReturn(executableElement);
    }
    return isValid;
  }

  private final boolean validateMethodVoidReturn(ExecutableElement executableElement) {
    if (!TestingPredicates.RETURN_TYPE_VOID_PREDICATE.apply(executableElement)) {
      errorReporter.report(ErrorMessage.NON_VOID_RETURN, executableElement);
      return false;
    }
    return true;
  }

  private boolean hasTestCaseAsParentClass(TypeElement typeElement) {
    return MoreApt.getClassHierarchy(typeElement).stream()
        .map(input -> input.getQualifiedName().toString())
        .anyMatch(Predicates.equalTo(TestCase.class.getName()));
  }

  private List<ExecutableElement> getAllJUnit3TestMethods(TypeElement typeElement) {
    return ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        .filter(TestingPredicates.METHOD_NAME_STARTS_WITH_TEST_PREDICATE)
        .filter(TestingPredicates.ZERO_PARAMETER_PREDICATE)
        .collect(toImmutableList());
  }

  private boolean isTestClass(TypeElement typeElement) {
    return typeElement.getQualifiedName().contentEquals(TestCase.class.getName());
  }
}
