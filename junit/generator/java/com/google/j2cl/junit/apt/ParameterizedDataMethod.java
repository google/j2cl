/*
 * Copyright 2022 Google Inc.
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

/** Represents a parameterized test method in a JUnit test case. */
@AutoValue
public abstract class ParameterizedDataMethod implements Method {

  public static Builder builder() {
    return new AutoValue_ParameterizedDataMethod.Builder();
  }

  public abstract String parameterizedNameTemplate();

  /** Builder for {@link ParameterizedDataMethod}. */
  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder javaMethodName(String s);

    abstract Builder isStatic(boolean isStatic);

    abstract Builder parameterizedNameTemplate(String name);

    abstract ParameterizedDataMethod build();
  }
}
