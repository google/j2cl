/*
 * Copyright 2023 Google Inc.
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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefinitelyNotNull {
  static class Ordering<T extends @Nullable Object> {
    <S extends T> Ordering<S> reverse() {
      throw new RuntimeException();
    }

    <S extends T> Ordering<@Nullable S> nullsLast() {
      throw new RuntimeException();
    }
  }

  final class NullsFirstOrdering<T extends @Nullable Object> extends Ordering<@Nullable T> {
    @SuppressWarnings("nullness")
    final Ordering<? super T> ordering;

    NullsFirstOrdering(Ordering<? super T> ordering) {
      this.ordering = ordering;
    }

    @Override
    public <S extends @Nullable T> Ordering<S> reverse() {
      // Type inference problem detected in Guava.
      return ordering.reverse().nullsLast();
    }

    @Override
    @SuppressWarnings("nullness") // probably a bug in our checker?
    public <S extends @Nullable T> Ordering<@Nullable S> nullsLast() {
      // Type inference problem detected in Guava.
      return ordering.nullsLast();
    }
  }

  // Reproduction of Guava code with immutable lists.
  public static class ImmutableList<E> {
    public static <E> ImmutableList<E> copyOf(Iterable<E> iterable) {
      throw new RuntimeException();
    }

    @SuppressWarnings("nullness")
    public static <E extends @Nullable Object> ImmutableList<E> copyOfNullable(
        Iterable<E> iterable) {
      // "iterable" requires manual "as Iterable<E & Any>" unchecked cast in Kotlin.
      return ImmutableList.copyOf(iterable);
    }
  }
}
