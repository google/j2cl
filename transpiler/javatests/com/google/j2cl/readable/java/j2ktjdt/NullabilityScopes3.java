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
package j2ktjdt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public class NullabilityScopes3 {
  @NullMarked
  static final class Optional<T> {
    static <T> Optional<T> of(T value) {
      throw new RuntimeException();
    }
  }

  @NullMarked
  interface List<T extends @Nullable Object> {}

  @NullMarked
  static class ImmutableList<T> implements List<T> {
    static <T> ImmutableList<T> of() {
      throw new RuntimeException();
    }
  }

  void testNonNullMarked() {
    Optional.<List<String>>of(ImmutableList.of());
  }
}
