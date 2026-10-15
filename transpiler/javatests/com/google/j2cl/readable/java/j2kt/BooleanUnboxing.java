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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BooleanUnboxing {
  public static boolean unbox(Boolean b) {
    return b;
  }

  public static boolean unboxNullable(@Nullable Boolean b) {
    return b;
  }

  public static <T extends Boolean> boolean unboxTypeVariable(T t) {
    return t;
  }

  public static <T extends @Nullable Boolean> boolean unboxNullableTypeVariable(T t) {
    return t;
  }

  public static boolean unboxConditional(boolean c, Boolean a, Boolean b) {
    return !(c ? a : b).booleanValue();
  }

  public static boolean compareWithPrimitive(Boolean a, boolean b) {
    return a == b;
  }

  public static boolean compareReferences(Boolean a, Boolean b) {
    // Reference comparison.
    return a == b;
  }

  public static boolean compareUnboxed(Boolean a, Boolean b) {
    // Value comparison, both operands are unboxed.
    return a == b.booleanValue();
  }

  public static boolean compareUnboxedNullable(@Nullable Boolean a, @Nullable Boolean b) {
    // Value comparison, both operands are unboxed.
    return a.booleanValue() != b.booleanValue();
  }

  public static <T extends Boolean> boolean compareUnboxedTypeVariable(T t, Boolean b) {
    // Value comparison, both operands are unboxed.
    return t == b.booleanValue();
  }

  public static void acceptObject(@Nullable Object o) {}

  public static void acceptBoxed(@Nullable Boolean b) {}

  public static void passUnboxed(Boolean a, @Nullable Boolean b) {
    acceptObject(a.booleanValue());
    acceptObject(b.booleanValue());
    acceptBoxed(a.booleanValue());
  }
}
