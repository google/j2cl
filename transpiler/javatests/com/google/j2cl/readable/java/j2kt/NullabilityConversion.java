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
  public interface Parent {}

  public interface Child extends Parent {}

  public interface Generic<T extends @Nullable Parent> {}

  public interface Consumer<T extends @Nullable Parent> {
    void set(T t);
  }

  public interface Supplier<T extends @Nullable Child> {
    T get();
  }

  public static class Tests {

    public static class Types {

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

      public static class UpperWildcardToSimple {
        public static @Nullable Parent nullableToNullable(
            Supplier<? extends @Nullable Child> supplier) {
          return supplier.get();
        }

        public static Parent nullableToNonNull(Supplier<? extends @Nullable Child> supplier) {
          return supplier.get();
        }

        public static @Nullable Parent nonNullToNullable(Supplier<? extends Child> supplier) {
          return supplier.get();
        }

        public static Parent nonNullToNonNull(Supplier<? extends Child> supplier) {
          return supplier.get();
        }
      }

      public static class LowerWildcardToSimple {
        public static @Nullable Parent nullableToNullable(
            Supplier<? super @Nullable Child> supplier) {
          return supplier.get();
        }

        public static Parent nullableToNonNull(Supplier<? super @Nullable Child> supplier) {
          return supplier.get();
        }

        public static @Nullable Parent nonNullToNullable(Supplier<? super Child> supplier) {
          return supplier.get();
        }

        public static Parent nonNullToNonNull(Supplier<? super Child> supplier) {
          return supplier.get();
        }
      }

      public static class SimpleToLowerWildcard {
        public static void nullableToNullable(
            Consumer<? super @Nullable Parent> consumer, @Nullable Child it) {
          consumer.set(it);
        }

        public static void nullableToNonNull(
            Consumer<? super Parent> consumer, @Nullable Child it) {
          consumer.set(it);
        }

        public static void nonNullToNullable(
            Consumer<? super @Nullable Parent> consumer, Child it) {
          consumer.set(it);
        }

        public static void nonNullToNonNull(Consumer<? super Parent> consumer, Child it) {
          consumer.set(it);
        }
      }

      public static class SimpleUpperWildcardToLowerWildcard {
        public static void nullableToNullable(
            Consumer<? super @Nullable Parent> consumer,
            Supplier<? extends @Nullable Child> supplier) {
          consumer.set(supplier.get());
        }

        // TODO(b/361769898): Uncomment when fixed
        // public static void nullableToNonNull(
        //     Consumer<? super Parent> consumer, Supplier<? extends @Nullable Child> supplier) {
        //   consumer.set(supplier.get());
        // }

        public static void nonNullToNullable(
            Consumer<? super @Nullable Parent> consumer, Supplier<? extends Child> supplier) {
          consumer.set(supplier.get());
        }

        public static void nonNullToNonNull(
            Consumer<? super Parent> consumer, Supplier<? extends Child> supplier) {
          consumer.set(supplier.get());
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

        public static class VariableToLowerWildcard {
          public static <T extends @Nullable Child> void defaultToDefault(
              Consumer<? super T> consumer, T t) {
            consumer.set(t);
          }

          public static <T extends @Nullable Child> void defaultToNullable(
              Consumer<? super @Nullable T> consumer, T t) {
            consumer.set(t);
          }

          // TODO(b/361769898): Uncomment when fixed
          // public static <T extends @Nullable Child> void defaultToNonNull(
          //     Consumer<? super @NonNull T> consumer, T t) {
          //   consumer.set(t);
          // }

          public static <T extends @Nullable Child> void nullableToDefault(
              Consumer<? super T> consumer, @Nullable T t) {
            consumer.set(t);
          }

          public static <T extends @Nullable Child> void nullableToNullable(
              Consumer<? super @Nullable T> consumer, @Nullable T t) {
            consumer.set(t);
          }

          public static <T extends @Nullable Child> void nullableToNonNull(
              Consumer<? super @NonNull T> consumer, @Nullable T t) {
            consumer.set(t);
          }

          public static <T extends @Nullable Child> void nonNullToDefault(
              Consumer<? super T> consumer, @NonNull T t) {
            consumer.set(t);
          }

          public static <T extends @Nullable Child> void nonNullToNullable(
              Consumer<? super @Nullable T> consumer, @NonNull T t) {
            consumer.set(t);
          }

          public static <T extends @Nullable Child> void nonNullToNonNull(
              Consumer<? super @NonNull T> consumer, @NonNull T t) {
            consumer.set(t);
          }
        }

        public static class UpperWildcardToVariable {
          public static <T extends @Nullable Child> T defaultToDefault(
              Supplier<? extends T> supplier) {
            return supplier.get();
          }

          public static <T extends @Nullable Child> @Nullable T defaultToNullable(
              Supplier<? extends T> supplier) {
            return supplier.get();
          }

          // TODO(b/361769898): Uncomment when fixed
          // public static <T extends @Nullable Child> @NonNull T defaultToNonNull(
          //     Supplier<? extends T> supplier) {
          //   return supplier.get();
          // }

          // TODO(b/361769898): Uncomment when fixed
          // public static <T extends @Nullable Child> T nullableToDefault(
          //     Supplier<? extends @Nullable T> supplier) {
          //   return supplier.get();
          // }

          public static <T extends @Nullable Child> @Nullable T nullableToNullable(
              Supplier<? extends @Nullable T> supplier) {
            return supplier.get();
          }

          public static <T extends @Nullable Child> @NonNull T nullableToNonNull(
              Supplier<? extends @Nullable T> supplier) {
            return supplier.get();
          }

          public static <T extends @Nullable Child> T nonNullToDefault(
              Supplier<? extends @NonNull T> supplier) {
            return supplier.get();
          }

          public static <T extends @Nullable Child> @Nullable T nonNullToNullable(
              Supplier<? extends @NonNull T> supplier) {
            return supplier.get();
          }

          public static <T extends @Nullable Child> @NonNull T nonNullToNonNull(
              Supplier<? extends @NonNull T> supplier) {
            return supplier.get();
          }
        }
      }

      public static class TypeArguments {

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
              Generic<? extends @Nullable Child> it) {
            return it;
          }

          // TODO(b/361769898): Uncomment when fixed
          // public static Generic<? extends Parent> nullableToNonNull(
          //     Generic<? extends @Nullable Child> it) {
          //   return it;
          // }

          public static Generic<? extends @Nullable Parent> nonNullToNullable(
              Generic<? extends Child> it) {
            return it;
          }

          public static Generic<? extends Parent> nonNullToNonNull(Generic<? extends Child> it) {
            return it;
          }
        }

        public static class SimpleLowerWildcardToUpperWildcard {
          public static Generic<? extends @Nullable Parent> nullableToNullable(
              Generic<? super @Nullable Child> it) {
            return it;
          }

          // TODO(b/361769898): Uncomment when fixed
          // public static Generic<? extends Parent> nullableToNonNull(
          //     Generic<? super @Nullable Child> it) {
          //   return it;
          // }

          public static Generic<? extends @Nullable Parent> nonNullToNullable(
              Generic<? super Child> it) {
            return it;
          }

          // TODO(b/361769898): Uncomment when fixed
          // public static Generic<? extends Parent> nonNullToNonNull(Generic<? super Child> it) {
          //   return it;
          // }
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

          public static <T extends @Nullable Parent>
              Generic<? extends @Nullable T> defaultToNullable(Generic<T> it) {
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

          public static <T extends @Nullable Parent>
              Generic<? extends @NonNull T> nullableToNonNull(Generic<@Nullable T> it) {
            return it;
          }

          public static <T extends @Nullable Parent> Generic<? extends T> nonNullToDefault(
              Generic<@NonNull T> it) {
            return it;
          }

          public static <T extends @Nullable Parent>
              Generic<? extends @Nullable T> nonNullToNullable(Generic<@NonNull T> it) {
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

          public static <T extends @Nullable Parent>
              Generic<? super @Nullable T> nullableToNullable(Generic<@Nullable T> it) {
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

          public static <T extends @Nullable Parent>
              Generic<? extends @Nullable T> defaultToNullable(Generic<? extends T> it) {
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

          public static <T extends @Nullable Parent>
              Generic<? extends @Nullable T> nonNullToNullable(Generic<? extends @NonNull T> it) {
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

          public static <T extends @Nullable Parent>
              Generic<? super @Nullable T> nullableToNullable(Generic<? super @Nullable T> it) {
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
}
