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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.j2cl.junit.integration.async.data.TestReturnTypeNotStructuralPromise;
import com.google.j2cl.junit.integration.async.data.TestReturnTypeNotStructuralPromiseThenNameRedefined;
import com.google.j2cl.junit.integration.async.data.TestReturnTypeNotStructuralPromiseThenParameterCount;
import com.google.j2cl.junit.integration.async.data.TestReturnTypeNotStructuralPromiseThenParameterNotJsType;
import com.google.j2cl.junit.integration.async.data.TestReturnsVoidTimeoutProvided;
import com.google.j2cl.junit.integration.async.data.TestTimeOutNotProvided;
import com.google.j2cl.junit.integration.async.data.TestWithExpectedException;
import com.google.testing.compile.CompilationRule;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link J2clTestingProcessingStep}. */
@RunWith(JUnit4.class)
public class J2clTestingProcessingStepTest {

  @Rule public CompilationRule compilation = new CompilationRule();
  Messager messager;

  @Before
  public void setup() {
    messager = mock(Messager.class);
  }

  @Test
  public void testSimpleJUnit3TestCase() {
    TestClass testClass = executeProcessorOnTest(SimpleJUnit3TestCase.class);
    assertThat(testClass.isJUnit3()).isTrue();
    assertThat(testClass.packageName()).isEqualTo("com.google.j2cl.junit.apt");
    assertThat(testClass.simpleName()).isEqualTo("SimpleJUnit3TestCase");
    assertThat(testClass.afterClassMethods()).isEmpty();
    assertThat(testClass.beforeClassMethods()).isEmpty();
    assertThat(testClass.testMethods())
        .containsExactly(method("testMethod1"), method("testMethod2"))
        .inOrder();
  }

  @Test
  public void testAdvancedJUnit3TestCase() {
    TestClass testClass = executeProcessorOnTest(AdvancedJUnit3TestCase.class);
    assertThat(testClass.isJUnit3()).isTrue();
    assertThat(testClass.packageName()).isEqualTo("com.google.j2cl.junit.apt");
    assertThat(testClass.simpleName()).isEqualTo("AdvancedJUnit3TestCase");
    assertThat(testClass.afterClassMethods()).isEmpty();
    assertThat(testClass.beforeClassMethods()).isEmpty();
    assertThat(testClass.testMethods())
        .containsExactly(
            method("test"),
            method("test1"),
            method("testOverridenFromGrandParent"),
            method("testOverridenFromParent"),
            method("testParent1"),
            method("testParent"),
            method("testFromGrandParent1"),
            method("testFromGrandParent"))
        .inOrder();
  }

  @Test
  public void testJUnit4NonPublicMethod() {
    assertError(ErrorMessage.NON_PUBLIC, JUnit4TestCaseWithNonPublicTestMethod.class, "test");
  }

  @Test
  public void testJUnit4StaticMethod() {
    assertError(ErrorMessage.IS_STATIC, JUnit4TestCaseWithStaticTestMethod.class, "test");
  }

  @Test
  public void testJUnit4ArgumentsOnMethod() {
    assertError(ErrorMessage.HAS_ARGS, JUnit4TestCaseWithArgumentsOnMethod.class, "test");
  }

  @Test
  public void testJUnit4NonStaticClassMethod() {
    assertError(
        ErrorMessage.NON_STATIC, JUnit4TestCaseWithNonStaticClassMethod.class, "beforeClass");
  }

  @Test
  public void testJUnit4NonStaticParameterizedMethod() {
    assertError(
        ErrorMessage.NON_STATIC, JUnit4TestCaseWithNonStaticParameterizedMethod.class, "data");
  }

  @Test
  public void testJUnit4NonPublicParameterizedField() {
    assertError(
        ErrorMessage.NON_PUBLIC, JUnit4TestCaseWithNonPublicParameterizedField.class, "field1");
  }

  @Test
  public void testJUnit4StaticParameterizedField() {
    assertError(ErrorMessage.IS_STATIC, JUnit4TestCaseWithStaticParameterizedField.class, "field1");
  }

  @Test
  public void testJUnit4FinalParameterizedField() {
    assertError(ErrorMessage.IS_FINAL, JUnit4TestCaseWithFinalParameterizedField.class, "field1");
  }

  @Test
  public void testJUnit4InvalidParameterizedFieldValue() {
    assertParameterError(
        ErrorMessage.INVALID_PARAMETER_VALUE,
        JUnit4TestCaseInvalidParameterizedFieldValue.class,
        1,
        1,
        0);
    verifyParameterPrintMessage(ErrorMessage.MISSING_PARAMETER, 0);
  }

  @Test
  public void testJUnit4DuplicateParameterizedField() {
    assertParameterError(
        ErrorMessage.DUPLICATE_PARAMETER,
        JUnit4TestCaseDuplicateAndMissingParameterizedField.class,
        1,
        2);
    verifyParameterPrintMessage(ErrorMessage.MISSING_PARAMETER, 2);
  }

  @Test
  public void testJUnit4NonPublicClassMethod() {
    assertError(
        ErrorMessage.NON_PUBLIC, JUnit4TestCaseWithNonPublicClassMethod.class, "beforeClass");
  }

  @Test
  public void testJUnit4ArgumentsOnClassMethod() {
    assertError(
        ErrorMessage.HAS_ARGS, JUnit4TestCaseWithArgumentsOnClassMethod.class, "beforeClass");
  }

  @Test
  public void testJUnit4NonVoidReturnTypeClassMethod() {
    assertError(
        ErrorMessage.NON_PROMISE_RETURN,
        JUnit4TestCaseNonVoidReturnTypeClassMethod.class,
        "beforeClass");
  }

  @Test
  public void testJUnit4VoidReturnTypeParameterizedMethod() {
    assertError(
        ErrorMessage.NON_ITERABLE_OR_ARRAY_RETURN,
        JUnit4TestCaseVoidReturnTypeParameterizedMethod.class,
        "data");
  }

  @Test
  public void testJUnit4TestCasePrimitiveArrayReturnTypeParameterizedMethod() {
    assertError(
        ErrorMessage.NON_ITERABLE_OR_ARRAY_RETURN,
        JUnit4TestCasePrimitiveArrayReturnTypeParameterizedMethod.class,
        "data");
  }

  @Test
  public void testJUnit4InnerClass() {
    assertError(ErrorMessage.NON_TOP_LEVEL_TYPE, JUnit4TestCaseOnInnerClass.Inner.class);
  }

  @Test
  public void testNonJunit4Suite() {
    assertError(ErrorMessage.JUNIT3_SUITE, JUnit3Suite.class);
    assertError(ErrorMessage.EMPTY_SUITE, EmptySuite.class);
    assertError(ErrorMessage.SKIPPED_TYPE, RandomDependency.class);
  }

  @Test
  public void testNoTestInput() {
    J2clTestingProcessingStep step = createProcessor();
    step.writeSummary();
    verifyPrintMessage(ErrorMessage.NO_TEST_INPUT, null);
    verifyNoMoreInteractions(messager);
  }

  @Test
  public void testJUnit3NonPublicMethod() {
    assertError(ErrorMessage.NON_PUBLIC, JUnit3TestCaseWithNonPublicTestMethod.class, "test");
  }

  @Test
  public void testJUnit3StaticMethod() {
    TestClass testClass = executeProcessorOnTest(JUnit3TestCaseWithStaticTestMethod.class);
    assertThat(testClass.isJUnit3()).isTrue();
    assertThat(testClass.packageName()).isEqualTo("com.google.j2cl.junit.apt");
    assertThat(testClass.simpleName()).isEqualTo("JUnit3TestCaseWithStaticTestMethod");
    assertThat(testClass.afterClassMethods()).isEmpty();
    assertThat(testClass.beforeClassMethods()).isEmpty();
    assertThat(testClass.testMethods()).containsExactly(staticMethod("test"));
  }

  @Test
  public void testJUnit3NonVoidReturnTypeMethod() {
    assertError(
        ErrorMessage.NON_VOID_RETURN,
        JUnit3TestCaseNonVoidReturnTypeTestMethod.class,
        "testIllegal");
  }

  @Test
  public void testJUnit3InnerClass() {
    assertError(ErrorMessage.NON_TOP_LEVEL_TYPE, JUnit3TestCaseOnInnerClass.Inner.class);
  }

  @Test
  public void testAsyncTestWithoutTimeout() {
    assertError(
        ErrorMessage.ASYNC_HAS_NO_TIMEOUT,
        TestTimeOutNotProvided.class,
        "beforeThenable",
        "beforeFuture",
        "afterFuture",
        "afterThenable",
        "futureDoesNotHaveTimeout",
        "thenableDoesNotHaveTimeout");
  }

  @Test
  public void testAsyncTestWithExpectedException() {
    assertError(ErrorMessage.ASYNC_HAS_EXPECTED_EXCEPTION, TestWithExpectedException.class, "test");
  }

  @Test
  public void testAsyncTestNonVoidReturnTypeMethod() {
    assertError(
        ErrorMessage.NON_PROMISE_RETURN,
        TestReturnTypeNotStructuralPromise.class,
        "returnTypeNotStructuralPromise",
        "before",
        "after");
  }

  @Test
  public void testAsyncTestThenNameRedefined() {
    assertError(
        ErrorMessage.NON_PROMISE_RETURN,
        TestReturnTypeNotStructuralPromiseThenNameRedefined.class,
        "returnTypeNotQuiteThenable");
  }

  @Test
  public void testAsyncTestThenParameterCount() {
    assertError(
        ErrorMessage.NON_PROMISE_RETURN,
        TestReturnTypeNotStructuralPromiseThenParameterCount.class,
        "test");
  }

  @Test
  public void testAsyncTestThenParameterNotJsType() {
    assertError(
        ErrorMessage.NON_PROMISE_RETURN,
        TestReturnTypeNotStructuralPromiseThenParameterNotJsType.class,
        "test");
  }

  @Test
  public void testNonAsyncTestProvidesTimeout() {
    assertError(
        ErrorMessage.NON_ASYNC_HAS_TIMEOUT,
        TestReturnsVoidTimeoutProvided.class,
        "test",
        "before",
        "after");
  }

  @Test
  public void testClassLevelIgnore() {
    assertError(ErrorMessage.IGNORE_ON_TYPE, JUnit4ClassLevelIgnoreTestCase.class);
  }

  private void assertError(ErrorMessage expectedError, Class<?> testClass, String... methodNames) {
    assertThat(executeProcessorOnTest(testClass)).isNull();
    if (methodNames.length == 0) {
      verifyPrintMessage(expectedError, testClass.getCanonicalName());
    } else {
      for (String methodName : methodNames) {
        verifyPrintMessage(expectedError, testClass.getCanonicalName() + "." + methodName);
      }
    }
    verifyNoMoreInteractions(messager);
  }

  private void assertParameterError(
      ErrorMessage expectedError, Class<?> testClass, Object... args) {
    assertThat(executeProcessorOnTest(testClass)).isNull();
    verifyParameterPrintMessage(expectedError, args);
  }

  private void verifyPrintMessage(ErrorMessage expectedError, String errorArg) {
    verify(messager).printMessage(expectedError.kind(), expectedError.format(errorArg));
  }

  private void verifyParameterPrintMessage(ErrorMessage expectedError, Object... errorArgs) {
    verify(messager).printMessage(expectedError.kind(), expectedError.format(errorArgs));
  }

  private TestClass executeProcessorOnTest(Class<?> test) {
    J2clTestingProcessingStep step = createProcessor();
    step.handleClass(test.getCanonicalName());
    return Iterables.getOnlyElement(step.getTestClasses(), null);
  }

  private J2clTestingProcessingStep createProcessor() {
    ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
    when(processingEnv.getTypeUtils()).thenReturn(compilation.getTypes());
    when(processingEnv.getElementUtils()).thenReturn(compilation.getElements());
    when(processingEnv.getMessager()).thenReturn(messager);
    when(processingEnv.getFiler()).thenReturn(mock(Filer.class));
    when(processingEnv.getOptions()).thenReturn(ImmutableMap.of("testPlatform", "CLOSURE"));
    return new J2clTestingProcessingStep(processingEnv);
  }

  // TODO(goktug): Remove after testAdvancedJUnit3TestCase is moved to integration test and update
  // the visibilities of used classes.
  private TestMethod method(String methodName) {
    return TestMethod.builder().javaMethodName(methodName).isStatic(false).build();
  }

  private TestMethod staticMethod(String methodName) {
    return TestMethod.builder().javaMethodName(methodName).isStatic(true).build();
  }
}
