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

public final class VisibilityModifiers {
  public static class Public {}

  static class PackagePrivate {}

  protected static class Protected {}

  private static class Private {}

  abstract static class AutoValue_Value {}

  abstract static class AutoOneOf_OneOf {}

  abstract static class AutoConverter_Converter {}

  abstract static class AutoEnumConverter_EnumConverter {}

  public static final class Value extends AutoValue_Value {}

  public static final class OneOf extends AutoOneOf_OneOf {}

  public static final class Converter extends AutoConverter_Converter {}

  public static final class EnumConverter extends AutoEnumConverter_EnumConverter {}

  abstract static class NotAutoValue_Value {}

  abstract static class NotAutoOneOf_OneOf {}

  abstract static class NotAutoConverter_Converter {}

  abstract static class NotAutoEnumConverter_EnumConverter {}
}
