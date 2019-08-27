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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.j2cl.junit.apt.MoreApt.asTypeElement;

import com.google.auto.common.MoreElements;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
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
        .testMethods(getTestMethods(typeElement))
        .beforeMethods(getAnnotatedMethods(typeHierarchy.reverse(), Before.class))
        .afterMethods(getAnnotatedMethods(typeHierarchy, After.class))
        .beforeClassMethods(getAnnotatedMethods(typeHierarchy.reverse(), BeforeClass.class))
        .afterClassMethods(getAnnotatedMethods(typeHierarchy, AfterClass.class))
        .build();
  }

  private static ImmutableList<TestMethod> getTestMethods(TypeElement typeElement) {

    // Ignore tests in this class and supertypes.
    if (isAnnotationPresent(typeElement, Ignore.class)) {
      return ImmutableList.of();
    }

    // Collect inherited methods
    Set<TestMethod> methods = new LinkedHashSet<>();
    for (TypeMirror ifce : typeElement.getInterfaces()) {
      methods.addAll(getTestMethods(asTypeElement(ifce)));
    }
    if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
      methods.addAll(getTestMethods(asTypeElement(typeElement.getSuperclass())));
    }

    Predicate<? super Element> isIgnored = TestingPredicates.hasAnnotation(Ignore.class);

    // Remove ignored methods.
    ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        // JUnit4 as of 8/2019 requires both @Test and @Ignore for an overridden test to be ignored,
        // although looking at the documentation and following open bug annotations like @Test, etc
        // should be inherited; so in our implementation we honour @Ignore whether @Test is also
        // defined in the method.
        // (See: https://github.com/junit-team/junit4/issues/695
        .filter(isIgnored)
        .map(JUnit4TestDataExtractor::toTestMethod)
        .forEach(methods::remove);

    // Add declared methods.
    ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
        .filter(TestingPredicates.hasAnnotation(Test.class))
        .filter(isIgnored.negate())
        .map(JUnit4TestDataExtractor::toTestMethod)
        .forEach(methods::add);

    return ImmutableList.sortedCopyOf(MethodSorter.getTestSorter(typeElement), methods);
  }

  private ImmutableList<TestMethod> getAnnotatedMethods(
      List<TypeElement> typeHierarchy, Class<? extends Annotation> annotation) {
    return typeHierarchy.stream()
        .flatMap(
            t ->
                ElementFilter.methodsIn(t.getEnclosedElements()).stream()
                    .filter(TestingPredicates.hasAnnotation(annotation))
                    .map(JUnit4TestDataExtractor::toTestMethod))
        .distinct()
        .collect(toImmutableList());
  }

  public static TestMethod toTestMethod(ExecutableElement element) {
    // No parameters allowed in JUnit4 methods.
    checkState(element.getParameters().isEmpty());
    return TestMethod.builder()
        .javaMethodName(element.getSimpleName().toString())
        .isStatic(element.getModifiers().contains(Modifier.STATIC))
        .expectedExceptionQualifiedName(getExpectedException(element))
        .timeout(getTimeout(element))
        .isAsync(TestingPredicates.IS_RETURNTYPE_A_PROMISE.test(element))
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
