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
package j2ktjavac;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AnonymousClassWithNullableTypeArgument {
  public interface Supplier<V extends @Nullable Object> {
    V get();
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

  public static Supplier<@Nullable Object>
      testImplicitTypeArguments_inferredFromMembersAndReturnType() {
    // In javac frontend it's inferred as Supplier<Object>
    return new Supplier<>() {
      @Override
      public @Nullable Object get() {
        return null;
      }
    };
  }

  public static void testImplicitTypeArguments_inferredFromMembers() {
    // rerpo for b/408237089
    // In javac frontend it's inferred as Supplier<Object>
    new Supplier<>() {
      @Override
      public @Nullable Object get() {
        return null;
      }
    };
  }

  public static void testImplicitTypeArguments_inferredFromMembersAndArgument() {
    // rerpo for b/408237089
    // In javac frontend it's inferred as AbstractHolder<String>
    new AbstractHolder<>("Supplier") {
      @Override
      public @Nullable String get() {
        return null;
      }
    };
  }

  public static void testImplicitTypeArguments_inferredFromArgument() {
    // In javac frontend it's inferred as Holder<String>
    new Holder<>(nullableString()) {};
  }

  public static Holder<@Nullable String>
      testImplicitTypeArguments_inferredFromArgumentAndReturnType() {
    // In javac frontend it's inferred as Holder<String>
    return new Holder<>("Supplier") {};
  }

  static class ParameterizedEmptyClass<T extends @Nullable Object> {}

  interface ParameterizedEmptyInterface<T extends @Nullable Object> {}

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

  public static @Nullable String nullableString() {
    return null;
  }
}
