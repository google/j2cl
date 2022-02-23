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
package com.google.j2cl.transpiler.ast;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** Kotlin type information. */
@AutoValue
public abstract class KtTypeInfo {
  @Nullable
  public abstract String getPackageName();

  @Nullable
  public abstract String getName();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_KtTypeInfo.Builder();
  }

  /** The builder. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setPackageName(@Nullable String packageName);

    public abstract Builder setName(@Nullable String name);

    public abstract KtTypeInfo build();
  }
}
