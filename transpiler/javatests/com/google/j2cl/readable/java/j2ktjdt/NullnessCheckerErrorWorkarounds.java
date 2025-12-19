/*
 * Copyright 2024 Google Inc.
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
package j2ktjdt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * There's a bug in checker-framework (https://github.com/eisop/checker-framework/issues/1010),
 * which incorrectly detects nullability violation in valid code.
 *
 * <p>Users solve this problem in two ways:
 *
 * <ul>
 *   <li>by inserting @SuppressWarnings annotation,
 *   <li>by adding @Nullable annotation to a type declaration. This approach may be problematic for
 *       J2KT if the type bound of annotated element is non-null.
 * </ul>
 *
 * J2KT should support both workarounds.
 */
@NullMarked
class NullnessCheckerErrorWorkarounds {
  interface Array<T extends @Nullable Object> {
    @Nullable T get();
  }

  interface Cell<C extends Cell<C>> {}

  interface Table<C extends Cell<C>> extends Array<C> {}

  static void testNullableAnnotation(Table<? extends Cell<?>> table) {
    Cell<@Nullable ?> cell = table.get();
  }

  static void testSuppressWarnings(Table<? extends Cell<?>> table) {
    @SuppressWarnings("nullness:assignment")
    Cell<?> cell = table.get();
  }
}
