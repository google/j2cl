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
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

class JUnit3TestDataExtractor {

  public TestClass extractTestData(TypeElement typeElement) {
    // JUnit3 only has one setUp/tearDown method, but it is protected
    // so we added "hidden" setup and tearDown methods in our version of TestCase
    return TestClass.builder()
        .isJUnit3(true)
        .packageName(MoreElements.getPackage(typeElement).getQualifiedName().toString())
        .simpleName(typeElement.getSimpleName().toString())
        .qualifiedName(typeElement.getQualifiedName().toString())
        .testConstructor(TestConstructor.builder().numberOfParameters(0).build())
        .testMethods(getJUnit3TestMethods(MoreApt.getClassHierarchy(typeElement)))
        .beforeMethods(ImmutableList.of())
        .afterMethods(ImmutableList.of())
        .beforeClassMethods(ImmutableList.<TestMethod>of())
        .afterClassMethods(ImmutableList.<TestMethod>of())
        .beforeParamMethods(ImmutableList.<TestMethod>of())
        .afterParamMethods(ImmutableList.<TestMethod>of())
        .parameterizedDataMethod(Optional.empty())
        .parameterizedFields(ImmutableList.<ParameterizedTestField>of())
        .build();
  }

  private ImmutableList<TestMethod> getJUnit3TestMethods(List<TypeElement> typeHierarchy) {
    return typeHierarchy.stream()
        .flatMap(t -> getJUnit3TestMethods(t).stream())
        .distinct()
        .collect(toImmutableList());
  }

  private ImmutableList<TestMethod> getJUnit3TestMethods(TypeElement typeElement) {
    return ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        .filter(TestingPredicates.METHOD_NAME_STARTS_WITH_TEST_PREDICATE)
        .filter(TestingPredicates.ZERO_PARAMETER_PREDICATE)
        .map(
            input ->
                TestMethod.builder()
                    .javaMethodName(input.getSimpleName().toString())
                    .isStatic(input.getModifiers().contains(Modifier.STATIC))
                    .build())
        .sorted(MethodSorter.getTestSorter(typeElement))
        .collect(toImmutableList());
  }
}
