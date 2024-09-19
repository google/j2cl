/*
 * Copyright 2024 Google Inc.
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
import com.google.j2cl.common.ThreadLocalInterner;
import javax.annotation.Nullable;

/** A package declaration. */
@AutoValue
public abstract class PackageDeclaration {

  public abstract String getName();

  @Nullable
  abstract String getCustomizedJsNamespace();

  public String getJsNamespace() {
    return getCustomizedJsNamespace() != null ? getCustomizedJsNamespace() : getName();
  }

  public static Builder newBuilder() {
    return new AutoValue_PackageDeclaration.Builder();
  }

  /** Builder for a PackageDeclaration. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setName(String name);

    public abstract Builder setCustomizedJsNamespace(@Nullable String jsNamespace);

    private static final ThreadLocalInterner<PackageDeclaration> interner =
        new ThreadLocalInterner<>();

    abstract PackageDeclaration autoBuild();

    public PackageDeclaration build() {
      return interner.intern(autoBuild());
    }
  }
}
