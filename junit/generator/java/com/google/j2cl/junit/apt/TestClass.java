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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents a JUnit test case.
 */
@AutoValue
public abstract class TestClass {

  public static Builder builder() {
    return new AutoValue_TestClass.Builder();
  }

  public abstract String packageName();
  public abstract String simpleName();
  public abstract String qualifiedName();

  public abstract ImmutableList<TestMethod> testMethods();
  public abstract ImmutableList<TestMethod> beforeMethods();
  public abstract ImmutableList<TestMethod> afterMethods();
  public abstract ImmutableList<TestMethod> beforeClassMethods();
  public abstract ImmutableList<TestMethod> afterClassMethods();

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

  /**
   * Builder for TestClass.
   */
  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder packageName(String s);
    abstract Builder simpleName(String s);
    abstract Builder qualifiedName(String s);
    abstract Builder testMethods(ImmutableList<TestMethod> t);
    abstract Builder beforeMethods(ImmutableList<TestMethod> t);
    abstract Builder afterMethods(ImmutableList<TestMethod> t);
    abstract Builder beforeClassMethods(ImmutableList<TestMethod> t);
    abstract Builder afterClassMethods(ImmutableList<TestMethod> t);

    abstract TestClass build();
  }
}
