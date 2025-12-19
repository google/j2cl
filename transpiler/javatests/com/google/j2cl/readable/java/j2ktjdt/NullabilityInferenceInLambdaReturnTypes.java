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

@NullMarked
public class NullabilityInferenceInLambdaReturnTypes {
  interface Foo<V extends @Nullable Object> {}

  interface Supplier<V extends @Nullable Object> {
    V get();
  }

  interface Operator<V extends @Nullable Object> {
    V apply(V v);
  }

  static <V extends @Nullable Object> void accept(Supplier<V> supplier) {
    throw new RuntimeException();
  }

  static <V extends @Nullable Object> Supplier<V> transform(Supplier<V> supplier) {
    throw new RuntimeException();
  }

  private void testAccept() {
    accept(() -> null);
  }

  private void testTransformAndAccept() {
    accept(transform(() -> null));
  }

  private <V extends @Nullable Object> Operator<Foo<V>> testReturnArgument_expliciParameterType() {
    return (Foo<V> foo) -> foo;
  }

  private <V extends @Nullable Object> Operator<Foo<V>> testReturnArgument_inferredParameterType() {
    return foo -> foo;
  }

  private <V extends @Nullable Object> Operator<Foo<V>> testReturnLocal_explicitParameterType() {
    return (Foo<V> foo) -> {
      Foo<V> localFoo = foo;
      return localFoo;
    };
  }

  private <V extends @Nullable Object> Operator<Foo<V>> testReturnLocal_inferredParameterType() {
    return foo -> {
      Foo<V> localFoo = foo;
      return localFoo;
    };
  }
}
