/*
 * Copyright 2018 Google Inc.
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
import com.google.j2cl.common.ThreadLocalInterner;

/** Encapsulates JsEnum information. */
@AutoValue
public abstract class JsEnumInfo {

  /** Returns {@code true} if this @JsEnum has a custom value type. */
  public abstract boolean hasCustomValue();

  public abstract boolean supportsComparable();

  public abstract boolean supportsOrdinal();

  abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_JsEnumInfo.Builder().setHasCustomValue(false).setSupportsComparable(true);
  }

  /** A Builder for JsEnumInfo. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setHasCustomValue(boolean hasCustomValue);

    public abstract Builder setSupportsComparable(boolean supportsComparable);

    public abstract Builder setSupportsOrdinal(boolean supportsOrdinal);

    abstract JsEnumInfo autoBuild();

    abstract boolean hasCustomValue();

    abstract boolean supportsOrdinal();

    public JsEnumInfo build() {
      checkState(!supportsOrdinal() || !hasCustomValue());
      return interner.intern(autoBuild());
    }

    public static Builder from(JsEnumInfo jsEnumInfo) {
      return jsEnumInfo.toBuilder();
    }

    private static final ThreadLocalInterner<JsEnumInfo> interner = new ThreadLocalInterner<>();
  }
}
