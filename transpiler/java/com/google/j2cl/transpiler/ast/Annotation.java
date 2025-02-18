/*
 * Copyright 2025 Google Inc.
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

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Map;

/** Represents an annotation instance and its element values. */
@AutoValue
public abstract class Annotation {

  public abstract DeclaredTypeDescriptor getTypeDescriptor();

  public abstract ImmutableMap<String, Literal> getValues();

  public static Builder newBuilder() {
    return new AutoValue_Annotation.Builder();
  }

  /** Builder for Annotation. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract DeclaredTypeDescriptor getTypeDescriptor();

    public abstract Builder setTypeDescriptor(DeclaredTypeDescriptor typeDescriptor);

    @CanIgnoreReturnValue
    public Builder addValue(String elementName, Literal value) {
      valuesBuilder().put(elementName, value);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addAllValues(Iterable<? extends Map.Entry<String, Literal>> values) {
      valuesBuilder().putAll(values);
      return this;
    }

    abstract ImmutableMap.Builder<String, Literal> valuesBuilder();

    public Annotation build() {
      checkState(getTypeDescriptor().isAnnotation(), "Annotation must be an annotation type.");
      return autoBuild();
    }

    abstract Annotation autoBuild();
  }
}
