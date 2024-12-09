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
package j2ktnotpassing;

import static j2ktnotpassing.NullabilityScopes2.NullMarkedScope.toList;

import j2ktnotpassing.NullabilityScopes2.NullMarkedScope.Recursive;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reproduces problems in {@code InsertCastsOnNullabilityMismatch} pass on nullability scope
 * boundaries.
 */
class NullabilityScopes2 {
  @NullMarked
  static class NullMarkedScope {
    public static <E extends @Nullable Object> Collector<E, ?, List<E>> toList() {
      throw new RuntimeException();
    }

    interface Recursive<M extends Recursive<M>> {
      List<Recursive<M>> toList();
    }
  }

  static class NonNullMarkedScope<T extends Recursive<T>> {
    public List<Object> flatten(Recursive<T> recursive) {
      return recursive.toList().stream()
          .map(this::flatten)
          .flatMap(Collection::stream)
          .collect(toList());
    }
  }
}
