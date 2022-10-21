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

import javax.tools.Diagnostic.Kind;

enum ErrorMessage {
  NON_VOID_RETURN("Method %s has a non void return type."),
  NON_PROMISE_RETURN(
      "Method %s has a non void return type that is not a promise-like. "
          + "A promise-like type is a type that is annotated with @JsType "
          + "and has a 'then' method. 'then' method should have a 'success' callback parameter "
          + "and an optional 'failure' callback parameter where both are "
          + "@JsFunction or @FunctionalInterface."),
  NON_ITERABLE_OR_ARRAY_RETURN(
      "Method %s annotated with @Parameters must return an Iterable or arrays."),
  HAS_ARGS("Method %s cannot have arguments."),
  IS_STATIC("Method %s cannot be static."),
  IS_FINAL("Member %s cannot be final"),
  NON_STATIC("Class level method %s cannot be non-static."),
  NON_PUBLIC("Member %s cannot be non-public."),
  NON_ASYNC_HAS_TIMEOUT("Method %s has timeout but doesn't return a promise-like type."),
  ASYNC_HAS_EXPECTED_EXCEPTION(
      "Method %s has expectedException attribute but returns a promise-like type."),
  ASYNC_HAS_NO_TIMEOUT("Method %s is missing timeout but returns a promise-like type."),
  TEST_HAS_TIMEOUT_ANNOTATION(
      "Method %s has @Timeout annotation. @Timeout can only be used with @Before/@After. "
          + "Test methods should use @Test(timeout=x) instead."),
  INVALID_PARAMETER_VALUE(
      "Invalid @Parameter value: %d. @Parameter fields counted: %d. "
          + "Please use an index between 0 and %d."),
  MISSING_PARAMETER("@Parameter(%d) is never used."),
  DUPLICATE_PARAMETER("@Parameter(%d) is used more than once (%d)."),
  NON_TOP_LEVEL_TYPE("Type %s is not a top level class."),
  IGNORE_ON_TYPE("Type %s has @Ignore. @Ignore on types are currently not supported."),
  SKIPPED_TYPE("Type %s is not a JUnit test or a JUnit4 style suite. Skipped.", Kind.WARNING),
  EMPTY_SUITE("Test suite %s doesn't include any tests."),
  JUNIT3_SUITE("Type %s is a JUnit3 style suite. j2cl_test supports only JUnit4 style suites."),
  NO_TEST("No tests found."),
  NO_TEST_INPUT(
      "Test class is not found. "
          + "Ensure that test_class is a fully qualified class name and exists in the classpath."),
  CANNOT_WRITE_RESOURCE("Can not write jsunit test suite file: %s"),
  UNSUPPORTED_PLATFORM("Platform %s is unknown.");

  private final String formattedMsg;
  private final Kind kind;

  private ErrorMessage(String formattedMsg, Kind kind) {
    this.formattedMsg = formattedMsg;
    this.kind = kind;
  }

  private ErrorMessage(String formattedMsg) {
    this(formattedMsg, Kind.ERROR);
  }

  public Kind kind() {
    return kind;
  }

  public String format(Object... args) {
    return String.format(formattedMsg, args);
  }
}
