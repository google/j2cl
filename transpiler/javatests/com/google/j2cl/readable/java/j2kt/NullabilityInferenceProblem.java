/*
 * Copyright 2024 Google Inc.
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

import static java.util.Comparator.comparing;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class NullabilityInferenceProblem {
  interface ImmutableList<T> extends List<T> {}

  interface User {
    Optional<String> getName();
  }

  private static <E> ImmutableList<E> sortedCopyOf(
      Comparator<? super E> comparator, Iterable<? extends E> elements) {
    throw new RuntimeException();
  }

  private static <T> T checkNotNull(@Nullable T reference) {
    throw new RuntimeException();
  }

  private static ImmutableList<User> testImplicitTypeArguments(Iterable<User> users) {
    return sortedCopyOf(comparing(user -> user.getName().orElse("")), users);
  }

  private static ImmutableList<User> testExplicitTypeArguments(Iterable<User> users) {
    return sortedCopyOf(
        Comparator.<User, String>comparing(user -> user.getName().orElse("")), users);
  }

  private static ImmutableList<User> testCheckNotNull(Iterable<User> users) {
    return sortedCopyOf(comparing(user -> checkNotNull(user.getName().orElse(""))), users);
  }
}
