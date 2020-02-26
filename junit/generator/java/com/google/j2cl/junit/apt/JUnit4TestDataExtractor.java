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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.common.MoreElements;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

class JUnit4TestDataExtractor {

  public TestClass extractJUnit4Test(TypeElement typeElement) {
    ImmutableList<TypeElement> typeHierarchy = MoreApt.getClassHierarchy(typeElement);
    return TestClass.builder()
        .packageName(MoreElements.getPackage(typeElement).getQualifiedName().toString())
        .simpleName(typeElement.getSimpleName().toString())
        .qualifiedName(typeElement.getQualifiedName().toString())
        .testMethods(getAnnotatedMethods(typeHierarchy, Test.class, not(TestMethod::isIgnored)))
        .beforeMethods(getAnnotatedMethods(typeHierarchy, Before.class).reverse())
        .afterMethods(getAnnotatedMethods(typeHierarchy, After.class))
        .beforeClassMethods(getAnnotatedMethods(typeHierarchy, BeforeClass.class).reverse())
        .afterClassMethods(getAnnotatedMethods(typeHierarchy, AfterClass.class))
        .build();
  }

  private ImmutableList<TestMethod> getAnnotatedMethods(
      List<TypeElement> typeHierarchy, Class<? extends Annotation> annotation) {
    return getAnnotatedMethods(typeHierarchy, annotation, Predicates.alwaysTrue());
  }

  private ImmutableList<TestMethod> getAnnotatedMethods(
      List<TypeElement> typeHierarchy,
      Class<? extends Annotation> annotation,
      Predicate<TestMethod> filter) {
    return typeHierarchy.stream()
        .flatMap(t -> getAnnotatedMethodStream(t, annotation))
        .filter(distinctByName())
        .filter(filter)
        .collect(toImmutableList());
  }

  private static Predicate<TestMethod> distinctByName() {
    Set<String> seen = new HashSet<>();
    return t -> seen.add(t.javaMethodName());
  }

  private static Stream<TestMethod> getAnnotatedMethodStream(
      TypeElement type, Class<? extends Annotation> annotation) {
    return ElementFilter.methodsIn(type.getEnclosedElements()).stream()
        .filter(TestingPredicates.hasAnnotation(annotation))
        .map(JUnit4TestDataExtractor::toTestMethod)
        .sorted(MethodSorter.getTestSorter(type));
  }

  public static TestMethod toTestMethod(ExecutableElement element) {
    // No parameters allowed in JUnit4 methods.
    checkState(element.getParameters().isEmpty());
    return TestMethod.builder()
        .javaMethodName(element.getSimpleName().toString())
        .isStatic(element.getModifiers().contains(Modifier.STATIC))
        .expectedExceptionQualifiedName(getExpectedException(element))
        .timeout(getTimeout(element))
        .isAsync(TestingPredicates.IS_RETURNTYPE_A_THENABLE.test(element))
        .isIgnored(isAnnotationPresent(element, Ignore.class))
        .build();
  }

  private static String getExpectedException(Element method) {
    return MoreApt.getClassNameFromAnnotation(method, Test.class, "expected").orNull();
  }

  private static long getTimeout(Element method) {
    Test test = method.getAnnotation(Test.class);
    return test == null ? 0L : test.timeout();
  }
}
