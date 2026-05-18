/*
 * Copyright 2026 Google Inc.
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityInferenceProblem11 {
  public static class Node<T extends @Nullable Object, N extends Node<T, N>> {
    public @Nullable N getChild() {
      return null;
    }
  }

  public static class MyNode<T extends @Nullable Object> extends Node<T, MyNode<T>> {}

  public static <X> @NonNull X assertNotNull(@Nullable X ref) {
    throw new RuntimeException();
  }

  interface Callback<R extends @Nullable Object> {
    R run(R arg);
  }

  static <R extends @Nullable Object> R call(Callback<R> callback) {
    throw new RuntimeException();
  }

  // Repro for b/514134124
  public static <T> MyNode<@Nullable T> test() {
    return call(
        node -> {
          return assertNotNull(node.getChild());
        });
  }
}
