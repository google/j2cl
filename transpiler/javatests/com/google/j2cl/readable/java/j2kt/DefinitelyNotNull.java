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
package j2kt;

import jsinterop.annotations.JsNonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefinitelyNotNull {
  interface NotNullSupplier<T extends @Nullable Object> {
    @JsNonNull
    T getNotNull();
  }

  static String testNotNullSupplier(NotNullSupplier<? extends @Nullable String> supplier) {
    // TODO(b/361088311): It fails to compile in Kotlin because of:
    //  https://youtrack.jetbrains.com/issue/KT-70814
    // The type of {@code supplier.getNonNull()} expression is inferred as {@code String?} in
    // Kotlin, so J2KT needs to generate a non-null assertion to match {@code String} return type.
    return supplier.getNotNull();
  }

  static class Ordering<T extends @Nullable Object> {
    <S extends T> Ordering<S> reverse() {
      throw new RuntimeException();
    }

    static <E extends @Nullable Object> Ordering<? super E> reversed(Ordering<? super E> ordering) {
      // The inferred type parameter for reverse() should be <E> and not <E & Any>
      // See: b/268006049, b/272714235.
      return ordering.reverse();
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
      return ImmutableList.copyOf((Iterable<@JsNonNull E>) iterable);
    }
  }

  interface Equivalence<T> {
    boolean equivalent(@Nullable T a, @Nullable T b);
  }

  public static <T extends @Nullable Object> boolean testEquivalence(
      Equivalence<? super @JsNonNull T> equivalence, @Nullable T a, @Nullable T b) {
    return equivalence.equivalent(a, b);
  }
}
