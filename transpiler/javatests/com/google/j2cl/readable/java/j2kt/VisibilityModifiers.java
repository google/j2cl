/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package j2kt;

import com.google.auto.value.AutoValue;

public final class VisibilityModifiers {
  public static class Public {}

  static class PackagePrivate {}

  protected static class Protected {}

  private static class Private {}

  @AutoValue
  abstract static class Value {
    @AutoValue.Builder
    abstract static class Builder {
      abstract Value build();
    }
  }

  // Emulates generated AutoConverter class.
  abstract static class AutoConverter_Converter {}

  // Emulates generated AutoEnumConverter class.
  abstract static class AutoEnumConverter_EnumConverter {}

  public static final class Converter extends AutoConverter_Converter {}

  public static final class EnumConverter extends AutoEnumConverter_EnumConverter {}

  abstract static class NotAutoConverter_Converter {}

  abstract static class NotAutoEnumConverter_EnumConverter {}
}
