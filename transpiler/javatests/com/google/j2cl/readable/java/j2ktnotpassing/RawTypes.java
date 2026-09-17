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
package j2ktnotpassing;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class RawTypes {

  class NullableBound<T extends @Nullable Object> {}

  class Parent<T> {
    void accept(NullableBound<T> nullableBound) {}
  }

  class Child<T> extends Parent<T> {}

  class RecursiveChild<T extends RecursiveChild<T>> extends Parent<T> {}

  <T extends RecursiveChild<T>> RecursiveChild<T> copy(RecursiveChild<T> child) {
    return child;
  }

  <T extends RecursiveChild<T>> Parent<T> toParent(RecursiveChild<T> a) {
    return a;
  }

  // Repro for b/504902037.
  @SuppressWarnings({"rawtypes", "unchecked"})
  void testRawParent(NullableBound<Child<?>> nullableBound, Parent rawParent) {
    rawParent.accept(nullableBound);
  }

  // Repro for b/504902037.
  @SuppressWarnings({"rawtypes", "unchecked"})
  void testRawParentRecursive(NullableBound<RecursiveChild<?>> nullableBound, Parent rawParent) {
    rawParent.accept(nullableBound);
  }

  // Repro for b/450867235.
  Parent returnsRaw(RecursiveChild<?> parent) {
    return toParent(copy((RecursiveChild) parent));
  }
}
