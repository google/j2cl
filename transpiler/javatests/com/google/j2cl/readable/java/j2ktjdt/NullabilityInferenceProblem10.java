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

public class NullabilityInferenceProblem10 {
  // Non @NullMarked
  interface Lazy<V> {
    V get();
  }

  @NullMarked
  static class InNullMarked {
    interface Foo {}

    interface Future<V extends @Nullable Object> {}

    interface Observable<V extends @Nullable Object> {
      void addObserver(Observer<V> observer);
    }

    interface Observer<V extends @Nullable Object> {
      Future<@Nullable Void> observe(V v);
    }

    static Future<@Nullable Void> observeNullable(@Nullable Foo foo) {
      throw new RuntimeException();
    }

    static void test(Lazy<Observable<Foo>> lazyObservable) {
      // TODO(b/...): Uncomment when fixed
      // lazyObservable.get().addObserver(InNullMarked::observeNullable);
    }
  }
}
