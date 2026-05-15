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

// repro for b/454662844
@NullMarked
public final class NullabilityPropagationInMethodCallChains {
  public interface List<V extends @Nullable Object> {
    V get(int index);
  }

  public interface Function<T extends @Nullable Object, R extends @Nullable Object> {
    R apply(T t);
  }

  public interface FluentFunction<T extends @Nullable Object, R extends @Nullable Object> {
    Fluent<R> apply(T t);
  }

  public interface Fluent<V extends @Nullable Object> {
    <R extends @Nullable Object> Fluent<R> transform(Function<V, R> function);

    <R extends @Nullable Object> Fluent<R> transformFluent(FluentFunction<? super V, R> function);

    static <V extends @Nullable Object> Fluent<V> from(Fluent<V> fluent) {
      return fluent;
    }

    static Fluent<@Nullable Void> ofVoid() {
      throw new UnsupportedOperationException();
    }

    static <V extends @Nullable Object> Fluent<V> of(V value) {
      throw new UnsupportedOperationException();
    }

    static <V extends @Nullable Object> Fluent<List<V>> flatten(Fluent<? extends V>... fluents) {
      throw new UnsupportedOperationException();
    }
  }

  private static Fluent<String> testFluent_length0() {
    return Fluent.flatten(Fluent.of("foo"), Fluent.ofVoid()) // Fluent<List<Any?>>
        .transform(fluents -> (String) fluents.get(0)) // Fluent<String?>
        .transformFluent(string -> Fluent.of(string + "!")); // Fluent<String>
  }

  private static Fluent<String> testFluent_length1() {
    return Fluent.flatten(Fluent.of("foo"), Fluent.ofVoid()) // Fluent<List<Any?>>
        .transform(list -> list) // Fluent<List<Any?>>
        .transform(fluents -> (String) fluents.get(0)) // Fluent<String?>
        .transformFluent(string -> Fluent.of(string + "!")); // Fluent<String>
  }

  private static Fluent<String> testFluent_length2() {
    return Fluent.flatten(Fluent.of("foo"), Fluent.ofVoid()) // Fluent<List<Any?>>
        .transform(list -> list) // Fluent<List<Any?>>
        .transform(list -> list) // Fluent<List<Any?>>
        .transform(fluents -> (String) fluents.get(0)) // Fluent<String?>
        .transformFluent(string -> Fluent.of(string + "!")); // Fluent<String>
  }

  private static Fluent<String> testFluent_veryLong() {
    return Fluent.flatten(Fluent.of("foo"), Fluent.ofVoid()) // Fluent<List<Any?>>
        .transform(list -> list) // Fluent<List<Any?>>
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(list -> list)
        .transform(fluents -> (String) fluents.get(0)) // Fluent<String?>
        .transformFluent(string -> Fluent.of(string + "!")); // Fluent<String>
  }
}
