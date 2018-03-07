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
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.List;
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
        .testMethods(getAnnotatedMethods(typeHierarchy, Test.class))
        .beforeMethods(getAnnotatedMethods(typeHierarchy.reverse(), Before.class))
        .afterMethods(getAnnotatedMethods(typeHierarchy, After.class))
        .beforeClassMethods(getAnnotatedMethods(typeHierarchy.reverse(), BeforeClass.class))
        .afterClassMethods(getAnnotatedMethods(typeHierarchy, AfterClass.class))
        .build();
  }

  private ImmutableList<TestMethod> getAnnotatedMethods(
      List<TypeElement> typeHierarchy, Class<? extends Annotation> annotation) {
    return typeHierarchy
        .stream()
        .flatMap(t -> getAnnotatedMethods(t, annotation).stream())
        .distinct()
        .collect(toImmutableList());
  }

  private ImmutableList<TestMethod> getAnnotatedMethods(
      TypeElement typeElement, Class<? extends Annotation> annotation) {

    Stream<ExecutableElement> stream =
        ElementFilter.methodsIn(typeElement.getEnclosedElements())
            .stream()
            .filter(TestingPredicates.hasAnnotation(annotation))
            .filter(Predicates.not(TestingPredicates.hasAnnotation(Ignore.class)));
    if (annotation.equals(Test.class)) {
      stream = stream.sorted(MethodSorter.getSorter(typeElement));
    }

    return stream
        .map(
            input ->
                TestMethod.builder()
                    .javaMethodName(input.getSimpleName().toString())
                    .isStatic(input.getModifiers().contains(Modifier.STATIC))
                    .expectedExceptionQualifiedName(getExpectedException(input))
                    .timeout(getTimeout(input))
                    .isAsync(TestingPredicates.IS_RETURNTYPE_A_PROMISE.apply(input))
                    .build())
        .collect(toImmutableList());
  }

  private static String getExpectedException(Element method) {
    return MoreApt.getClassNameFromAnnotation(method, Test.class, "expected").orNull();
  }

  private static long getTimeout(Element method) {
    Test test = method.getAnnotation(Test.class);
    return test == null ? 0L : test.timeout();
  }
}
