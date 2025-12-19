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
public class NullabilityInferenceProblem7 {
  interface Foo {}

  interface Ordering<V extends @Nullable Object> {
    <T extends V> T max(T v1, T v2);
  }

  interface FooOrdering extends Ordering<Foo> {}

  static <T> T test1(Ordering<T> ordering, T t1, T t2) {
    // Here, local variables are nullable in the AST and type argument of max() method call has
    // non-null bound, so J2KT should insert `!!` instead of making type arguments nullable.
    T local1 = t1;
    T local2 = t2;
    return ordering.max(local1, local2);
  }

  static Foo test2(Ordering<Foo> ordering, Foo foo1, Foo foo2) {
    // Here, local variables are nullable in the AST and type argument of max() method call has
    // non-null bound, so J2KT should insert `!!` instead of making type arguments nullable.
    Foo localFoo1 = foo1;
    Foo localFoo2 = foo2;
    return ordering.max(localFoo1, localFoo2);
  }

  static Foo test3(FooOrdering ordering, Foo foo1, Foo foo2) {
    // Here, local variables are nullable in the AST and type argument of max() method call has
    // non-null bound, so J2KT should insert `!!` instead of making type arguments nullable.
    Foo localFoo1 = foo1;
    Foo localFoo2 = foo2;
    return ordering.max(localFoo1, localFoo2);
  }
}
