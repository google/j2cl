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
public class NullabilityInferenceProblem6 {
  interface Consumer<V extends @Nullable Object> {
    void accept(V v);
  }

  interface Container<V extends @Nullable Object> {
    void forEach(Consumer<V> consumer);

    void add(V v);
  }

  interface Foo {}

  interface FooConsumer<V extends @Nullable Foo> {
    void accept(V v);
  }

  interface FooContainer<V extends @Nullable Foo> {
    void forEach(Consumer<V> consumer);

    void add(V v);
  }

  static void testLambda(Container<?> from, Container<? super @Nullable Object> to) {
    // The type of "it" parameter is incorrectly inferred as capture of non-null Object in the AST.
    from.forEach(it -> to.add(it));
  }

  static void testMethodReference(Container<?> from, Container<? super @Nullable Object> to) {
    // The type of "it" parameter is incorrectly inferred as capture of non-null Object in the AST.
    from.forEach(to::add);
  }

  static void testLambda(FooContainer<?> from, FooContainer<? super @Nullable Foo> to) {
    // The type of "it" parameter is correctly inferred as capture of nullable Foo in the AST.
    from.forEach(it -> to.add(it));
  }

  static void testMethodReference(FooContainer<?> from, FooContainer<? super @Nullable Foo> to) {
    // The type of "it" parameter is correctly inferred as capture of nullable Foo in the AST.
    from.forEach(to::add);
  }
}
