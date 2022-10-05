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

import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

public class DefaultNullable {
  static void testNonNullableLambdas() {
    NotNullable.Consumer<String> lambda = s -> {};
    NotNullable.Consumer<String> methodReference = DefaultNullable::accept;
    NotNullable.Supplier<String> constructorReference = String::new;
    NotNullable.IntFunction<String[]> newArrayReference = String[]::new;
  }

  static void accept(String s) {}
}

@NullMarked
class NotNullable {
  interface Supplier<T extends @Nullable Object> {
    T get();
  }

  interface Consumer<T extends @Nullable Object> {
    void accept(T t);
  }

  interface IntFunction<T extends @Nullable Object> {
    T accept(int i);
  }
}
