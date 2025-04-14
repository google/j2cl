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
public class NullabilityInferenceProblem8 {
  interface NullableSupplier<V extends @Nullable Object> {
    @Nullable V get(String key);
  }

  interface Function<I extends @Nullable Object, O extends @Nullable Object> {
    O apply(I i);
  }

  public static <I, O> Function<I, O> nonNullWithLowerBound(Function<? super I, O> fn) {
    throw new RuntimeException();
  }

  public static void acceptParameterized(Function<String, Object> fn) {
    throw new RuntimeException();
  }

  public static void testMethodReference(NullableSupplier<Object> supplier) {
    acceptParameterized(nonNullWithLowerBound(supplier::get));
  }

  public static void testLambda(NullableSupplier<Object> supplier) {
    acceptParameterized(nonNullWithLowerBound(key -> supplier.get(key)));
  }
}
