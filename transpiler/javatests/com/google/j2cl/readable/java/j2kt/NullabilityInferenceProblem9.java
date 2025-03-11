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
public class NullabilityInferenceProblem9 {
  interface Foo {}

  interface Future<V extends @Nullable Object> {}

  interface FutureFunction<I extends @Nullable Object, O extends @Nullable Object> {
    Future<O> apply(I i);
  }

  static <V extends @Nullable Object> Future<V> future(V v) {
    throw new RuntimeException();
  }

  static <I extends @Nullable Object, O extends @Nullable Object> Future<O> transformFuture(
      Future<I> future, FutureFunction<? super I, ? extends O> fn) {
    throw new RuntimeException();
  }

  static Future<Foo> test(Future<@Nullable Foo> nullableFooFuture, Future<Foo> fooFuture) {
    return transformFuture(
        nullableFooFuture,
        nullableFoo -> {
          if (nullableFoo != null) {
            // The inferred type here is still Future<@Nullable Foo>, which causes a mismatch.
            return future(nullableFoo);
          }
          return fooFuture;
        });
  }
}
