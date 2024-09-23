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
package j2ktnotpassing;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityConversion {
  public static class Parent {}

  public static class Child extends Parent {}

  public static class Generic<T extends @Nullable Parent> {}

  public static class Tests {
    public static class Parameterized {
      public static class SimpleToLowerWildcard {
        public static Generic<? super @Nullable Child> nonNullToNullable(Generic<Parent> it) {
          return it;
        }
      }

      public static class SimpleUpperWildcardToUpperWildcard {
        public static Generic<? extends Parent> nullableToNonNull(
            Generic<? extends @Nullable Child> it) {
          return it;
        }
      }

      public static class SimpleLowerWildcardToLowerWildcard {
        public static Generic<? super @Nullable Child> nonNullToNullable(
            Generic<? super Parent> it) {
          return it;
        }
      }

      public static class VariableToVariable {
        public static <T extends @Nullable Parent> Generic<@NonNull T> defaultToNonNull(
            Generic<T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<T> nonNullToDefault(
            Generic<@NonNull T> it) {
          return it;
        }
      }

      public static class VariableToUpperWildcard {
        public static <T extends @Nullable Parent> Generic<? extends @NonNull T> defaultToNonNull(
            Generic<T> it) {
          return it;
        }
      }

      public static class VariableToLowerWildcard {
        public static <T extends @Nullable Parent> Generic<? super @Nullable T> defaultToNullable(
            Generic<T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super T> nonNullToDefault(
            Generic<@NonNull T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super @Nullable T> nonNullToNullable(
            Generic<@NonNull T> it) {
          return it;
        }
      }

      public static class VariableUpperWildcardToUpperWildcard {
        public static <T extends @Nullable Parent> Generic<? extends @NonNull T> defaultToNonNull(
            Generic<? extends T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends T> nullableToDefault(
            Generic<? extends @Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @NonNull T> nullableToNonNull(
            Generic<? extends @Nullable T> it) {
          return it;
        }
      }

      public static class VariableLowerWildcardToLowerWildcard {
        public static <T extends @Nullable Parent> Generic<? super @Nullable T> defaultToNullable(
            Generic<? super T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super T> nonNullToDefault(
            Generic<? super @NonNull T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super @Nullable T> nonNullToNullable(
            Generic<? super @NonNull T> it) {
          return it;
        }
      }
    }
  }
}
