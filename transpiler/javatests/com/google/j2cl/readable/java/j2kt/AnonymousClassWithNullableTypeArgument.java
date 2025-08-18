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
public class AnonymousClassWithNullableTypeArgument {
  interface Foo<V extends @Nullable Object> {
    V get();
  }

  static Foo<@Nullable Object> fooExplicitTypeArguments() {
    // In javac frontend it becomes Foo<Object>
    return new Foo<@Nullable Object>() {
      @Override
      public @Nullable Object get() {
        return null;
      }
    };
  }

  static Foo<@Nullable Object> fooImplicitTypeArguments() {
    // In javac frontend it's inferred as Foo<Object>
    return new Foo<>() {
      @Override
      public @Nullable Object get() {
        return null;
      }
    };
  }
}
