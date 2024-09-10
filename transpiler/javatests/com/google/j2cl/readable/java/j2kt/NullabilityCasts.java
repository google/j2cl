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
package j2kt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityCasts {
  interface AsyncFunction<I extends @Nullable Object, O extends @Nullable Object> {
    Async<O> apply(I i);
  }

  interface Async<V extends @Nullable Object> {}

  static <I extends @Nullable Object, O extends @Nullable Object> Async<O> tranformAsync(
      Async<I> async, AsyncFunction<? super I, ? extends O> function) {
    throw new RuntimeException();
  }

  void testCapturesInLambdas(boolean condition, Async<@Nullable String> async) {
    tranformAsync(
        async,
        it -> {
          if (condition) {
            return async;
          } else {
            return async;
          }
        });
  }
}
