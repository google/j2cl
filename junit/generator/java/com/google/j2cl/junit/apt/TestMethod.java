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
import javax.annotation.Nullable;

/** Represents a test method in a JUnit test case. */
@AutoValue
public abstract class TestMethod implements Method {

  public static Builder builder() {
    return new AutoValue_TestMethod.Builder().isAsync(false).timeout(0).isIgnored(false);
  }

  @Nullable
  public abstract String expectedExceptionQualifiedName();

  public abstract boolean isAsync();

  public abstract double timeout();

  public abstract boolean isIgnored();

  public String jsTestMethodName() {
    String javaMethodName = javaMethodName();
    if (javaMethodName.startsWith("test")) {
      return javaMethodName;
    }
    return "test_" + javaMethodName;
  }

  /** Builder for {@link TestMethod}. */
  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder javaMethodName(String s);

    abstract Builder isStatic(boolean isStatic);

    abstract Builder expectedExceptionQualifiedName(String s);

    abstract Builder isAsync(boolean async);

    abstract Builder timeout(double timeout);

    abstract Builder isIgnored(boolean async);

    abstract TestMethod build();
  }
}
