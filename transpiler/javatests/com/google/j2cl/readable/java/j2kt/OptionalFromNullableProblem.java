/*
 * Copyright 2025 Google Inc.
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
public class OptionalFromNullableProblem {
  static class Optional<T> {
    static <T> Optional<T> fromNullable(@Nullable T t) {
      throw new RuntimeException();
    }
  }

  static <T extends @Nullable Object> void test(T value) {
    // TODO(b/404225677): Uncomment when fixed.
    // Kotlin/Native compiler fails with error: type mismatch: inferred type is T but T & Any was
    // expected
    // Kotlin/JVM compiles without errors
    // Optional.fromNullable(value);
  }
}
