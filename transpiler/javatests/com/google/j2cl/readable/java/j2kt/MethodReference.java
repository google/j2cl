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
// TODO(b/319346347): Move to j2kt when the bug is fixed.
package j2kt;

final class MethodReference {
  static <T> void foo(final Foo<? extends Foo<? extends T>> inputs) {
    transform(inputs, Foo::bar);
  }

  static <F, T> void transform(Foo<F> foo, Function<? super F, ? extends T> function) {}

  interface Function<T, R> {
    <H> R apply(T t);
  }

  interface Foo<T> {
    Foo<T> bar();
  }
}
