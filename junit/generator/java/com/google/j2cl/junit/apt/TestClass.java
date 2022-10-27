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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** Represents a JUnit test case. */
@AutoValue
public abstract class TestClass {

  public static Builder builder() {
    return new AutoValue_TestClass.Builder();
  }

  public abstract String packageName();

  public abstract String simpleName();

  public abstract String qualifiedName();

  public abstract TestConstructor testConstructor();

  public abstract ImmutableList<TestMethod> testMethods();

  public abstract ImmutableList<TestMethod> beforeMethods();

  public abstract ImmutableList<TestMethod> afterMethods();

  public abstract ImmutableList<TestMethod> beforeClassMethods();

  public abstract ImmutableList<TestMethod> afterClassMethods();

  public abstract ImmutableList<TestMethod> beforeParamMethods();

  public abstract ImmutableList<TestMethod> afterParamMethods();

  public abstract boolean isJUnit3();

  public abstract Optional<ParameterizedDataMethod> parameterizedDataMethod();

  public abstract ImmutableList<ParameterizedTestField> parameterizedFields();

  public int numberOfTestMethods() {
    return testMethods().size();
  }

  public boolean needsAsyncSetup() {
    return beforeMethods().stream().anyMatch(TestMethod::isAsync);
  }

  public boolean needsAsyncTeardown() {
    return afterMethods().stream().anyMatch(TestMethod::isAsync);
  }

  public String jsUnitPackageName() {
    return "javatests." + packageName();
  }

  public String jsUnitAdapterClassName() {
    return simpleName() + "_Adapter";
  }

  public String jsUnitAdapterQualifiedClassName() {
    return jsUnitPackageName() + "." + jsUnitAdapterClassName();
  }

  public String jsUnitQualifiedName() {
    return jsUnitAdapterQualifiedClassName() + "Suite";
  }

  public boolean isParameterized() {
    return parameterizedDataMethod().isPresent();
  }

  /** Builder for TestClass. */
  @AutoValue.Builder
  public abstract static class Builder {

    abstract Builder isJUnit3(boolean i);

    abstract Builder packageName(String s);

    abstract Builder simpleName(String s);

    abstract Builder qualifiedName(String s);

    abstract Builder testConstructor(TestConstructor t);

    abstract Builder testMethods(ImmutableList<TestMethod> t);

    abstract Builder beforeMethods(ImmutableList<TestMethod> t);

    abstract Builder afterMethods(ImmutableList<TestMethod> t);

    abstract Builder beforeClassMethods(ImmutableList<TestMethod> t);

    abstract Builder afterClassMethods(ImmutableList<TestMethod> t);

    abstract Builder beforeParamMethods(ImmutableList<TestMethod> t);

    abstract Builder afterParamMethods(ImmutableList<TestMethod> t);

    abstract Builder parameterizedDataMethod(Optional<ParameterizedDataMethod> t);

    abstract Builder parameterizedFields(ImmutableList<ParameterizedTestField> t);

    abstract TestClass autoBuild();

    public TestClass build() {
      TestClass testClass = autoBuild();
      checkState(
          !testClass.isJUnit3()
              || (testClass.beforeMethods().isEmpty() && testClass.afterMethods().isEmpty()),
          "When marked as JUnit3, beforeMethods and afterMethods should be empty");
      return testClass;
    }
  }
}
