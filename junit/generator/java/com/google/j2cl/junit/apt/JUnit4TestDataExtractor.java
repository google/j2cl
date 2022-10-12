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
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.common.MoreElements;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.j2cl.junit.async.Timeout;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
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

class JUnit4TestDataExtractor {

  public TestClass extractJUnit4Test(TypeElement typeElement) {
    ImmutableList<TypeElement> typeHierarchy = MoreApt.getClassHierarchy(typeElement);
    return TestClass.builder()
        .isJUnit3(false)
        .packageName(MoreElements.getPackage(typeElement).getQualifiedName().toString())
        .simpleName(typeElement.getSimpleName().toString())
        .qualifiedName(typeElement.getQualifiedName().toString())
        .testConstructor(getConstructor(typeElement))
        .testMethods(
            getAnnotatedMethods(
                typeHierarchy,
                Test.class,
                not(TestMethod::isIgnored),
                JUnit4TestDataExtractor::toTestMethod))
        .beforeMethods(getAnnotatedMethods(typeHierarchy, Before.class).reverse())
        .afterMethods(getAnnotatedMethods(typeHierarchy, After.class))
        .beforeClassMethods(getAnnotatedMethods(typeHierarchy, BeforeClass.class).reverse())
        .afterClassMethods(getAnnotatedMethods(typeHierarchy, AfterClass.class))
        .beforeParamMethods(getAnnotatedMethods(typeHierarchy, BeforeParam.class))
        .afterParamMethods(getAnnotatedMethods(typeHierarchy, AfterParam.class))
        .parameterizedDataMethod(getAnnotatedDataMethod(typeHierarchy))
        .parameterizedFields(getAnnotatedFields(typeHierarchy, Parameter.class))
        .build();
  }

  public Optional<ParameterizedDataMethod> getAnnotatedDataMethod(List<TypeElement> typeHierarchy) {
    return getAnnotatedMethods(
            typeHierarchy,
            Parameters.class,
            Predicates.alwaysTrue(),
            JUnit4TestDataExtractor::toParameterizedDataMethod)
        .stream()
        .findFirst();
  }

  private ImmutableList<TestMethod> getAnnotatedMethods(
      List<TypeElement> typeHierarchy, Class<? extends Annotation> annotation) {
    return getAnnotatedMethods(
        typeHierarchy, annotation, Predicates.alwaysTrue(), JUnit4TestDataExtractor::toTestMethod);
  }

  private <T extends Method> ImmutableList<T> getAnnotatedMethods(
      List<TypeElement> typeHierarchy,
      Class<? extends Annotation> annotation,
      Predicate<T> filter,
      Function<ExecutableElement, T> transformer) {
    return typeHierarchy.stream()
        .flatMap(t -> getAnnotatedMethodStream(t, annotation, transformer))
        .filter(distinctByName())
        .filter(filter)
        .collect(toImmutableList());
  }

  private static <T extends Method> Predicate<T> distinctByName() {
    Set<String> seen = new HashSet<>();
    return t -> seen.add(t.javaMethodName());
  }

  private static <T extends Method> Stream<T> getAnnotatedMethodStream(
      TypeElement type,
      Class<? extends Annotation> annotation,
      Function<ExecutableElement, T> transformer) {
    return getAnnotatedElementsStream(type, annotation, ElementFilter::methodsIn)
        .map(transformer)
        .sorted(MethodSorter.getTestSorter(type));
  }

  private static <T extends Element> Stream<T> getAnnotatedElementsStream(
      TypeElement type,
      Class<? extends Annotation> annotation,
      Function<List<? extends Element>, List<T>> getElements) {
    return getElements.apply(type.getEnclosedElements()).stream()
        .filter(TestingPredicates.hasAnnotation(annotation));
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

  public static ParameterizedDataMethod toParameterizedDataMethod(ExecutableElement element) {
    // No parameters allowed in JUnit4 methods.
    checkState(element.getParameters().isEmpty());
    return ParameterizedDataMethod.builder()
        .javaMethodName(element.getSimpleName().toString())
        .isStatic(element.getModifiers().contains(Modifier.STATIC))
        .parameterizedNameTemplate(getNameTemplate(element))
        .build();
  }

  private static String getExpectedException(Element method) {
    return MoreApt.getClassNameFromAnnotation(method, Test.class, "expected").orNull();
  }

  private ImmutableList<ParameterizedTestField> getAnnotatedFields(
      List<TypeElement> typeHierarchy, Class<? extends Annotation> annotation) {
    return typeHierarchy.stream()
        .flatMap(t -> getAnnotatedFieldStream(t, annotation))
        .collect(toImmutableList());
  }

  private static Stream<ParameterizedTestField> getAnnotatedFieldStream(
      TypeElement type, Class<? extends Annotation> annotation) {
    return getAnnotatedElementsStream(type, annotation, ElementFilter::fieldsIn)
        .map(JUnit4TestDataExtractor::toParameterizedTestField);
  }

  public static ParameterizedTestField toParameterizedTestField(VariableElement element) {
    return ParameterizedTestField.builder()
        .fieldName(element.getSimpleName().toString())
        .index(element.getAnnotation(Parameter.class).value())
        .build();
  }

  private static long getTimeout(Element method) {
    Test testAnnotation = method.getAnnotation(Test.class);
    Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);
    return testAnnotation != null
        ? testAnnotation.timeout()
        : timeoutAnnotation != null ? timeoutAnnotation.value() : 0;
  }

  private static TestConstructor getConstructor(TypeElement typeElement) {
    ImmutableList<TestConstructor> customizedConstructors =
        ElementFilter.constructorsIn(typeElement.getEnclosedElements()).stream()
            .map(JUnit4TestDataExtractor::toTestConstructor)
            .collect(toImmutableList());
    checkArgument(customizedConstructors.size() <= 1, "Test class can only have one constructor");

    return Iterables.getOnlyElement(customizedConstructors, implicitTestConstructor());
  }

  public static TestConstructor toTestConstructor(ExecutableElement element) {
    return TestConstructor.builder().numberOfParameters(element.getParameters().size()).build();
  }

  private static TestConstructor implicitTestConstructor() {
    return TestConstructor.builder().numberOfParameters(0).build();
  }

  private static String getNameTemplate(Element method) {
    return Optional.ofNullable(method.getAnnotation(Parameters.class))
        .map(Parameters::name)
        .orElse("");
  }
}
