/*
 * Copyright 2023 Google Inc.
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
public class VoidType {
  interface NullableBounds<T extends @Nullable Object> {}

  interface NonNullBounds<T> {}

  interface Consumer<V extends @Nullable Void> {
    void accept(V v);
  }

  static void testVoid() {
    Void nonNullVoid;
    @Nullable Void nullableVoid;
    NullableBounds<Void> nullableBoundsWithNonNullVoid;
    NullableBounds<@Nullable Void> nullableBoundsWithNullableVoid;
    NonNullBounds<Void> nonNullBoundsWithNonNullVoid;
    Class<Void> voidClass;
    Consumer<Void> nonNullVoidConsumer = v -> v.hashCode();
    Consumer<@Nullable Void> nullableVoidConsumer = v -> v.hashCode();
    Consumer<?> nonNullVoidConsumerWildcard = (Void v) -> v.hashCode();
    Consumer<?> nullableVoidConsumerWildcard = (@Nullable Void v) -> v.hashCode();
  }
}
