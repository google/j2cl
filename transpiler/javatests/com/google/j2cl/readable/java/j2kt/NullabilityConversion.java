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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullabilityConversion {
  public static class Parent {}

  public static class Child extends Parent {}

  public static class Generic<T extends @Nullable Parent> {}

  public static class Tests {
    public static class SimpleToSimple {
      public static @Nullable Parent nullableToNullable(@Nullable Child it) {
        return it;
      }

      public static Parent nullableToNonNull(@Nullable Child it) {
        return it;
      }

      public static @Nullable Parent nonNullToNullable(Child it) {
        return it;
      }

      public static Parent nonNullToNonNull(Child it) {
        return it;
      }
    }

    public static class VariableToVariable {
      public static <T extends @Nullable Parent> T defaultToDefault(T it) {
        return it;
      }

      public static <T extends @Nullable Parent> @Nullable T defaultToNullable(T it) {
        return it;
      }

      public static <T extends @Nullable Parent> @NonNull T defaultToNonNull(T it) {
        return it;
      }

      public static <T extends @Nullable Parent> T nullableToDefault(@Nullable T it) {
        return it;
      }

      public static <T extends @Nullable Parent> @Nullable T nullableToNullable(@Nullable T it) {
        return it;
      }

      public static <T extends @Nullable Parent> @NonNull T nullableToNonNull(@Nullable T it) {
        return it;
      }

      public static <T extends @Nullable Parent> T nonNullToDefault(@NonNull T it) {
        return it;
      }

      public static <T extends @Nullable Parent> @Nullable T nonNullToNullable(@NonNull T it) {
        return it;
      }

      public static <T extends @Nullable Parent> @NonNull T nonNullToNonNull(@NonNull T it) {
        return it;
      }
    }

    public static class Parameterized {
      public static class SimpleToSimple {
        public static Generic<@Nullable Parent> nullableToNullable(Generic<@Nullable Parent> it) {
          return it;
        }

        public static Generic<Parent> nullableToNonNull(Generic<@Nullable Parent> it) {
          return it;
        }

        public static Generic<@Nullable Parent> nonNullToNullable(Generic<Parent> it) {
          return it;
        }

        public static Generic<Parent> nonNullToNonNull(Generic<Parent> it) {
          return it;
        }
      }

      public static class SimpleToUpperWildcard {
        public static Generic<? extends @Nullable Parent> nullableToNullable(
            Generic<@Nullable Child> it) {
          return it;
        }

        public static Generic<? extends Parent> nullableToNonNull(Generic<@Nullable Child> it) {
          return it;
        }

        public static Generic<? extends @Nullable Parent> nonNullToNullable(Generic<Child> it) {
          return it;
        }

        public static Generic<? extends Parent> nonNullToNonNull(Generic<Child> it) {
          return it;
        }
      }

      public static class SimpleToLowerWildcard {
        public static Generic<? super @Nullable Child> nullableToNullable(
            Generic<@Nullable Parent> it) {
          return it;
        }

        public static Generic<? super Child> nullableToNonNull(Generic<@Nullable Parent> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static Generic<? super @Nullable Child> nonNullToNullable(Generic<Parent> it) {
        //   return it;
        // }

        public static Generic<? super Child> nonNullToNonNull(Generic<Parent> it) {
          return it;
        }
      }

      public static class SimpleUpperWildcardToUpperWildcard {
        public static Generic<? extends @Nullable Parent> nullableToNullable(
            Generic<? super @Nullable Child> it) {
          return it;
        }

        public static Generic<? extends Parent> nullableToNonNull(
            Generic<? extends @Nullable Child> it) {
          return it;
        }

        public static Generic<? extends @Nullable Parent> nonNullToNullable(
            Generic<? extends Child> it) {
          return it;
        }

        public static Generic<? extends Parent> nonNullToNonNull(Generic<? extends Child> it) {
          return it;
        }
      }

      public static class SimpleLowerWildcardToLowerWildcard {
        public static Generic<? super @Nullable Child> nullableToNullable(
            Generic<? super @Nullable Parent> it) {
          return it;
        }

        public static Generic<? super Child> nullableToNonNull(
            Generic<? super @Nullable Parent> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static Generic<? super @Nullable Child> nonNullToNullable(
        //     Generic<? super Parent> it) {
        //   return it;
        // }

        public static Generic<? super Child> nonNullToNonNull(Generic<? super Parent> it) {
          return it;
        }
      }

      public static class VariableToVariable {
        public static <T extends @Nullable Parent> Generic<T> defaultToDefault(Generic<T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<@Nullable T> defaultToNullable(
            Generic<T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<@NonNull T> defaultToNonNull(
        //     Generic<T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<T> nullableToDefault(
            Generic<@Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<@Nullable T> nullableToNullable(
            Generic<@Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<@NonNull T> nullableToNonNull(
            Generic<@Nullable T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<T> nonNullToDefault(
        //     Generic<@NonNull T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<@Nullable T> nonNullToNullable(
            Generic<@NonNull T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<@NonNull T> nonNullToNonNull(
            Generic<@NonNull T> it) {
          return it;
        }
      }

      public static class VariableToUpperWildcard {
        public static <T extends @Nullable Parent> Generic<? extends T> defaultToDefault(
            Generic<T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @Nullable T> defaultToNullable(
            Generic<T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? extends @NonNull T>
        // defaultToNonNull(
        //     Generic<T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<? extends T> nullableToDefault(
            Generic<@Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent>
            Generic<? extends @Nullable T> nullableToNullable(Generic<@Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @NonNull T> nullableToNonNull(
            Generic<@Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends T> nonNullToDefault(
            Generic<@NonNull T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @Nullable T> nonNullToNullable(
            Generic<@NonNull T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @NonNull T> nonNullToNonNull(
            Generic<@NonNull T> it) {
          return it;
        }
      }

      public static class VariableToLowerWildcard {
        public static <T extends @Nullable Parent> Generic<? super T> defaultToDefault(
            Generic<T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? super @Nullable T>
        // defaultToNullable(
        //     Generic<T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<? super @NonNull T> defaultToNonNull(
            Generic<T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super T> nullableToDefault(
            Generic<@Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super @Nullable T> nullableToNullable(
            Generic<@Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super @NonNull T> nullableToNonNull(
            Generic<@Nullable T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? super T> nonNullToDefault(
        //     Generic<@NonNull T> it) {
        //   return it;
        // }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? super @Nullable T>
        // nonNullToNullable(
        //     Generic<@NonNull T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<? super @NonNull T> nonNullToNonNull(
            Generic<@NonNull T> it) {
          return it;
        }
      }

      public static class VariableUpperWildcardToUpperWildcard {
        public static <T extends @Nullable Parent> Generic<? extends T> defaultToDefault(
            Generic<? extends T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @Nullable T> defaultToNullable(
            Generic<? extends T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? extends @NonNull T>
        // defaultToNonNull(
        //     Generic<? extends T> it) {
        //   return it;
        // }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? extends T> nullableToDefault(
        //     Generic<? extends @Nullable T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent>
            Generic<? extends @Nullable T> nullableToNullable(Generic<? extends @Nullable T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? extends @NonNull T>
        // nullableToNonNull(
        //     Generic<? extends @Nullable T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<? extends T> nonNullToDefault(
            Generic<? extends @NonNull T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @Nullable T> nonNullToNullable(
            Generic<? extends @NonNull T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? extends @NonNull T> nonNullToNonNull(
            Generic<? extends @NonNull T> it) {
          return it;
        }
      }

      public static class VariableLowerWildcardToLowerWildcard {
        public static <T extends @Nullable Parent> Generic<? super T> defaultToDefault(
            Generic<? super T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? super @Nullable T>
        // defaultToNullable(
        //     Generic<? super T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<? super @NonNull T> defaultToNonNull(
            Generic<? super T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super T> nullableToDefault(
            Generic<? super @Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super @Nullable T> nullableToNullable(
            Generic<? super @Nullable T> it) {
          return it;
        }

        public static <T extends @Nullable Parent> Generic<? super @NonNull T> nullableToNonNull(
            Generic<? super @Nullable T> it) {
          return it;
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? super T> nonNullToDefault(
        //     Generic<? super @NonNull T> it) {
        //   return it;
        // }

        // TODO(b/361769898): Uncomment when fixed
        // public static <T extends @Nullable Parent> Generic<? super @Nullable T>
        // nonNullToNullable(
        //     Generic<? super @NonNull T> it) {
        //   return it;
        // }

        public static <T extends @Nullable Parent> Generic<? super @NonNull T> nonNullToNonNull(
            Generic<? super @NonNull T> it) {
          return it;
        }
      }
    }
  }
}
