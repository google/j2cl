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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityInferenceProblem16 {

  // E is not nullable.
  public abstract class ImmutableList<E> {}

  public interface Predicate<E extends @Nullable Object> {
    boolean test(E e);
  }

  public interface Holder<V extends @Nullable Object> {
    boolean check(Predicate<V> isError);
  }

  public static <V extends @Nullable Object> Holder<ImmutableList<Holder<V>>> wrap(
      ImmutableList<Holder<V>> holders) {
    throw new RuntimeException();
  }

  // Repro for b/450979894
  public static void test(ImmutableList<Holder<@Nullable Void>> holders) {
    // If the ImmutableList type parameter is nullable, the issue goes away.
    wrap(holders).check(list -> true);
  }
}
