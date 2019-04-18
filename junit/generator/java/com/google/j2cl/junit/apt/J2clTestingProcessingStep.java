/*
 * Copyright 2015 Google Inc.
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

import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.junit.runners.Suite;

/**
 * J2ClTestingProcessingStep emits a JavaScript file containing a JavaScript test suite for a given
 * JUnit Testcase.
 */
public class J2clTestingProcessingStep implements ProcessingStep {
  private final LinkedHashSet<TestClass> testClasses = Sets.newLinkedHashSet();
  private final JUnit3TestDataExtractor junit3Extractor = new JUnit3TestDataExtractor();
  private final JUnit4TestDataExtractor junit4Extractor = new JUnit4TestDataExtractor();
  private final ProcessingEnvironment processingEnv;
  private final ErrorReporter errorReporter;
  private final JUnit3Validator junit3Validator;
  private final JUnit4Validator junit4Validator;
  private final TemplateWriter writer;

  public J2clTestingProcessingStep(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.errorReporter = new ErrorReporter(processingEnv.getMessager());
    this.junit3Validator = new JUnit3Validator(errorReporter);
    this.junit4Validator = new JUnit4Validator(errorReporter);
    this.writer = new TemplateWriter(errorReporter, processingEnv.getFiler());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return Sets.newHashSet(J2clTestInput.class);
  }

  @Override
  public Set<Element> process(
      SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    Element value = Iterables.getOnlyElement(elementsByAnnotation.get(J2clTestInput.class));
    String className =
        MoreApt.getClassNameFromAnnotation(value, J2clTestInput.class, "value").get();
    handleClass(className);
    return ImmutableSet.of();
  }

  @VisibleForTesting
  void handleClass(String className) {
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(className);
    handleClass(typeElement);
  }

  private void handleClass(TypeElement typeElement) {
    if (isJunit3TestCase(typeElement)) {
      handleJUnit3TestCase(typeElement);
    } else if (isJUnit4TestCase(typeElement)) {
      handleJUnit4TestCase(typeElement);
    } else if (isJUnit4Suite(typeElement)) {
      handleJUnit4Suite(typeElement);
    } else if (isJUnit3Suite(typeElement)) {
      errorReporter.report(ErrorMessage.JUNIT3_SUITE, typeElement);
    } else {
      errorReporter.report(ErrorMessage.SKIPPED_TYPE, typeElement);
    }
  }

  private boolean isJunit3TestCase(TypeElement typeElement) {
    return junit3Validator.isJunit3TestCase(typeElement);
  }

  private void handleJUnit3TestCase(TypeElement typeElement) {
    if (!junit3Validator.validateJunit3Test(typeElement)) {
      return;
    }
    TestClass testClass = junit3Extractor.extractTestData(typeElement);
    writer.writeTestClass(testClass);
    testClasses.add(testClass);
  }

  private boolean isJUnit4TestCase(TypeElement typeElement) {
    // Making sure we do not find test methods from a JUnit3 test case
    if (isJunit3TestCase(typeElement)) {
      return false;
    }
    return junit4Validator.hasAnyMethodsAnnotatedWithTest(typeElement);
  }

  private void handleJUnit4TestCase(TypeElement typeElement) {
    if (!junit4Validator.validateJUnit4MethodAndClass(typeElement)) {
      return;
    }
    TestClass testClass = junit4Extractor.extractJUnit4Test(typeElement);
    writer.writeTestClass(testClass);
    testClasses.add(testClass);
  }

  private boolean isJUnit3Suite(TypeElement typeElement) {
    return junit3Validator.isJUnit3Suite(typeElement);
  }

  private boolean isJUnit4Suite(TypeElement typeElement) {
    return TestingPredicates.hasAnnotation(Suite.SuiteClasses.class).apply(typeElement);
  }

  private void handleJUnit4Suite(TypeElement typeElement) {
    List<String> classes =
        MoreApt.getClassNamesFromAnnotation(typeElement, Suite.SuiteClasses.class, "value");
    if (classes.isEmpty()) {
      errorReporter.report(ErrorMessage.EMPTY_SUITE, typeElement);
    }
    for (String clazzName : classes) {
      TypeElement t = processingEnv.getElementUtils().getTypeElement(clazzName);
      handleClass(t);
    }
  }

  @VisibleForTesting
  ImmutableList<TestClass> getTestClasses() {
    return ImmutableList.copyOf(testClasses);
  }

  public void writeSummary() {
    writer.writeSummary();
  }
}
