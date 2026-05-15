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
package j2kt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AnonymousClassWithNullableTypeArgument {
  public interface Supplier<V extends @Nullable Object> {
    V get();
  }

  public interface Consumer<V extends @Nullable Object> {
    void accept(V value);
  }

  public abstract static class AbstractHolder<V extends @Nullable Object> {
    public AbstractHolder(V value) {}

    public abstract V get();
  }

  public static class Holder<V extends @Nullable Object> {
    public Holder(V value) {}

    public V get() {
      throw new RuntimeException();
    }
  }

  public static Supplier<@Nullable Object> testExplicitTypeArguments() {
    return new Supplier<@Nullable Object>() {
      @Override
      public @Nullable Object get() {
        return null;
      }
    };
  }

  // TODO(b/440316295): Uncomment when fixed
  // public static void testImplicitTypeArguments_inferredFromParameters() {
  //   AnonymousClassWithNullableTypeArgument.<Supplier<@Nullable Object>>accept(
  //       new Supplier<>() {
  //         @Override
  //         public @Nullable Object get() {
  //           return null;
  //         }
  //       });
  // }

  // private static <T extends @Nullable Object> void accept(T t) {}

  // TODO(b/440316295): Uncomment when fixed
  // public static Supplier<@Nullable Object>
  //     testImplicitTypeArguments_inferredFromMembersAndReturnType() {
  //   return new Supplier<>() {
  //     @Override
  //     public @Nullable Object get() {
  //       return null;
  //     }
  //   };
  // }

  // TODO(b/440316295): Uncomment when fixed
  // public static void testImplicitTypeArguments_inferredFromMembers() {
  //   new Supplier<>() {
  //     @Override
  //     public @Nullable Object get() {
  //       return null;
  //     }
  //   };
  // }

  // TODO(b/440316295): Uncomment when fixed
  // public static void testImplicitTypeArguments_inferredFromMembersAndArgument() {
  //   new AbstractHolder<>("Supplier") {
  //     @Override
  //     public @Nullable String get() {
  //       return null;
  //     }
  //   };
  // }

  public static void testImplicitTypeArguments_inferredFromArgument() {
    new Holder<>(nullableString()) {};
  }

  public static Holder<@Nullable String>
      testImplicitTypeArguments_inferredFromArgumentAndReturnType() {
    return new Holder<>("Supplier") {};
  }

  public static class ParameterizedEmptyClass<T extends @Nullable Object> {}

  public interface ParameterizedEmptyInterface<T extends @Nullable Object> {}

  public static <T extends @Nullable Object>
      ParameterizedEmptyClass<@Nullable T> testExplicitSuperclassTypeArguments() {
    new ParameterizedEmptyClass<@Nullable Void>() {};
    return new ParameterizedEmptyClass<@Nullable T>() {};
  }

  public static <T extends @Nullable Object>
      ParameterizedEmptyInterface<@Nullable T> testExplicitSuperInterfaceTypeArguments() {
    new ParameterizedEmptyInterface<@Nullable Void>() {};
    return new ParameterizedEmptyInterface<@Nullable T>() {};
  }

  public static void testAnonymousClass_implicitTypeArguments_fromDeclaration() {
    ParameterizedEmptyClass<@Nullable String> emptyClass = new ParameterizedEmptyClass<>() {};
  }

  public static void testAnonymousClass_implicitTypeArguments_fromAssignment() {
    ParameterizedEmptyClass<@Nullable String> emptyClass;
    emptyClass = new ParameterizedEmptyClass<>() {};
  }

  public static ParameterizedEmptyClass<@Nullable String>
      testAnonymousClass_implicitTypeArguments_fromReturnType() {
    return new ParameterizedEmptyClass<>() {};
  }

  public static void testAnonymousClass_implicitTypeArguments_fromParameterType() {
    acceptOfNullableString(new ParameterizedEmptyClass<>() {});
  }

  public static void acceptOfNullableString(ParameterizedEmptyClass<@Nullable String> emptyClass) {}

  public static void testAnonymousInterface_implicitTypeArguments_fromDeclaration() {
    ParameterizedEmptyInterface<@Nullable String> emptyClass =
        new ParameterizedEmptyInterface<>() {};
  }

  public static void testAnonymousInterface_implicitTypeArguments_fromAssignment() {
    ParameterizedEmptyInterface<@Nullable String> emptyClass;
    emptyClass = new ParameterizedEmptyInterface<>() {};
  }

  public static ParameterizedEmptyInterface<@Nullable String>
      testAnonymousInterface_implicitTypeArguments_fromReturnType() {
    return new ParameterizedEmptyInterface<>() {};
  }

  public static void testAnonymousInterface_implicitTypeArguments_fromParameterType() {
    acceptOfNullableString(new ParameterizedEmptyInterface<>() {});
  }

  public static void acceptOfNullableString(
      ParameterizedEmptyInterface<@Nullable String> emptyClass) {}

  public static @Nullable String nullableString() {
    return null;
  }

  public static void acceptWildcardConsumer(Consumer<?> consumer) {
    throw new RuntimeException();
  }

  public static void testAcceptWildcardConsumerOfNonNullString() {
    acceptWildcardConsumer(
        new Consumer<String>() {
          @Override
          public void accept(String string) {}
        });
  }
}
