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
import java.beans.Introspector;
import javax.annotation.Nullable;

/** Kotlin member information. */
@AutoValue
public abstract class KtInfo {
  public static final KtInfo NONE = KtInfo.newBuilder().build();

  public abstract boolean isProperty();

  public abstract boolean isDisabled();

  public abstract boolean isUninitializedWarningSuppressed();

  @Nullable
  public abstract String getName();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_KtInfo.Builder()
        .setProperty(false)
        .setDisabled(false)
        .setUninitializedWarningSuppressed(false);
  }

  /** The builder. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProperty(boolean isProperty);

    public abstract Builder setDisabled(boolean isDisabled);

    public abstract Builder setUninitializedWarningSuppressed(boolean isLateInit);

    public abstract Builder setName(String name);

    public abstract KtInfo build();
  }

  public static String computePropertyName(String methodName) {
    return startsWithCamelCase(methodName, "get")
        ? Introspector.decapitalize(methodName.substring(3))
        : methodName;
  }

  private static boolean startsWithCamelCase(String string, String prefix) {
    return string.length() > prefix.length()
        && string.startsWith(prefix)
        && Character.isUpperCase(string.charAt(prefix.length()));
  }
}
