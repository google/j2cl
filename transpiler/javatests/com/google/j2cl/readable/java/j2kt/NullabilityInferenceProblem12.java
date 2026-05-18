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
package j2kt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityInferenceProblem12<K extends @Nullable Object> {
  public interface MyInterface<T extends @Nullable Object> {}

  static class Nested<N> implements MyInterface<N> {
    Nested(NullabilityInferenceProblem12<N> outer) {}
  }

  public MyInterface<K> test() {
    // Code like this is common java.util.ConcurrentHashMap. Note that `K` here, the type argument,
    // is nullable but the `N` in `Nested<N>` does not admit null.
    return new Nested<K>(this);
  }
}
