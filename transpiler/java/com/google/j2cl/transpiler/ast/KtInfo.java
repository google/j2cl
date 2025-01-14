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

import static com.google.common.base.Ascii.isUpperCase;
import static com.google.common.base.Ascii.toLowerCase;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** Kotlin member information. */
@AutoValue
public abstract class KtInfo {
  public static final KtInfo NONE = KtInfo.newBuilder().build();

  public abstract boolean isProperty();

  public abstract boolean isDisabled();

  public abstract boolean isUninitializedWarningSuppressed();

  public abstract boolean isThrows();

  @Nullable
  public abstract String getName();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_KtInfo.Builder()
        .setProperty(false)
        .setDisabled(false)
        .setThrows(false)
        .setUninitializedWarningSuppressed(false);
  }

  /** The builder. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProperty(boolean isProperty);

    public abstract Builder setDisabled(boolean isDisabled);

    public abstract Builder setUninitializedWarningSuppressed(boolean isLateInit);

    public abstract Builder setName(String name);

    public abstract Builder setThrows(boolean isThrows);

    public abstract KtInfo build();
  }

  public static String computePropertyName(String methodName) {
    return startsWithCamelCase(methodName, "get")
        ? decapitalize(methodName.substring(3))
        : methodName;
  }

  private static boolean startsWithCamelCase(String string, String prefix) {
    return string.length() > prefix.length()
        && string.startsWith(prefix)
        && Character.isUpperCase(string.charAt(prefix.length()));
  }

  /**
   * Decapitalizes a string, following this convention:
   *
   * <ul>
   *   <li>"FooBar" -> "fooBar"
   *   <li>"FOOBar" -> "fooBar"
   *   <li>"FOO" -> "foo"
   *   <li>"FOO_BAR" -> "foO_BAR"
   * </ul>
   */
  // Based on:
  // https://github.com/JetBrains/kotlin/blob/master/core/util.runtime/src/org/jetbrains/kotlin/utils/capitalizeDecapitalize.kt#L27
  // TODO(micapolos): Use Kotlin's utility directly when Kotlin is always linked to the transpiler.
  private static String decapitalize(String name) {
    if (name.isEmpty()) {
      return name;
    }

    if (!isUpperCase(name.charAt(0))) {
      return name;
    }

    char[] chars = name.toCharArray();

    // Lower-case first character.
    chars[0] = toLowerCase(chars[0]);

    // Lower-case following upper-case characters until the end, or until the last upper-case
    // character (exclusive).
    for (int i = 1; i < chars.length; i++) {
      int i1 = i + 1;
      if (i1 == chars.length || isUpperCase(chars[i1])) {
        chars[i] = toLowerCase(chars[i]);
      } else {
        break;
      }
    }

    return new String(chars);
  }
}
