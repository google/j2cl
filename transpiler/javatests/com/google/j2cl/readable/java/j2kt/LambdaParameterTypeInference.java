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
public class LambdaParameterTypeInference {
  public interface Supplier<T extends @Nullable Object> {
    T get();
  }

  public interface Consumer<T extends @Nullable Object> {
    void accept(T t);
  }

  public static <T extends @Nullable Object> Consumer<T> wrap(Consumer<T> consumer) {
    throw new RuntimeException();
  }

  public static <T extends @Nullable Object> void add(
      Supplier<T> supplier, Consumer<? super T> consumer) {
    throw new RuntimeException();
  }

  // J2KT inserts nullability- and generic- related casts in case of type mismatch, using
  // projections if necessary, which can potentially affect type inference in lambdas. This test
  // covers a case which would cause [CANNOT_INFER_PARAMETER_TYPE] compilation error in Kotlin if
  // unnecessary cast was inserted for "wrap()" call.
  public static <T extends @Nullable Object> void test(Supplier<? extends T> supplier) {
    add(supplier, wrap(x -> {}));
  }
}
