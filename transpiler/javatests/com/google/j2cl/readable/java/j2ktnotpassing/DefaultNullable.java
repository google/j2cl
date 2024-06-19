/*
 * Copyright 2022 Google Inc.
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
package j2ktnotpassing;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public class DefaultNullable {
  static void testNonNullableLambdas() {
    NotNullable.Consumer<String> methodReference = DefaultNullable::accept;
  }

  static void accept(String s) {}
}

@NullMarked
class NotNullable {
  interface Consumer<T extends @Nullable Object> {
    void accept(T t);
  }
}

@NullMarked
abstract class Ordering<T extends @Nullable Object> {
  <S extends T> Ordering<S> reverse() {
    return null;
  }

  static <E extends @Nullable Object> Ordering<? super E> reversed(Ordering<? super E> ordering) {
    return ordering.reverse();
  }
}
