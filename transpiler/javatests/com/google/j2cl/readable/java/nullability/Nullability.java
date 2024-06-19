/*
 * Copyright 2017 Google Inc.
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
package nullability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import org.jspecify.annotations.Nullable;

public class Nullability {
  @Nonnull private String f1 = "Hello";
  private String f2 = null;
  @Nullable private String f3 = null;
  private @Nonnull List<@JsNonNull String> f4 = new ArrayList<>();
  private List<@JsNonNull String> f5 = new ArrayList<>();
  private @Nonnull List<String> f6 = new ArrayList<>();
  private List<String> f7 = null;
  private @JsNonNull String @JsNonNull [] f8 = {};
  // Nonnullable array of nullable strings.
  private String @JsNonNull [] f9 = {};
  // Nullable array of non-nullable strings.
  private @JsNonNull String[] f10 = {};
  // Conversion from generic type parameter.
  private List<String> f12 = new ArrayList<>();
  @Nonnull private Object f13;
  private Object f14;

  @JsConstructor
  public Nullability(@Nonnull String a) {
    f13 = new Object();
  }

  @Nonnull
  public String m1(@Nonnull String a, @Nonnull List<@JsNonNull Double> b, String c) {
    return "";
  }

  public String m2(String a, @Nonnull List<@Nullable Double> b) {
    return "";
  }

  @JsMethod
  public String m3(@Nonnull String a, @JsNonNull String... args) {
    return null;
  }

  public void m4(@Nonnull MyFunction f) {}

  static class Foo<T> {
    void bar(T t) {}

    @Nullable
    T baz() {
      return null;
    }

    @Override
    public String toString() {
      return "Foo";
    }
  }

  @JsFunction
  interface MyFunction {
    @Nonnull
    String x(@Nonnull String a);
  }

  public interface StringList extends List<String> {}

  // Should implement Comparator<string>
  public static class StringComparator implements Comparator<@JsNonNull String> {
    @Override
    public int compare(@Nonnull String a, @Nonnull String b) {
      return 0;
    }
  }

  // Should implement Comparator<?string>
  public static class NullableStringComparator implements Comparator<@Nullable String> {
    @Override
    public int compare(@Nullable String a, @Nullable String b) {
      return 0;
    }
  }

  interface NullableTemplatedReturn<T> {
    @Nullable
    T foo();
  }

  public static class NullableTemplatedReturnOverride implements NullableTemplatedReturn<String> {
    @Nullable
    @Override
    public String foo() {
      return "foo";
    }
  }

  static class ParameterizedDefaultNullability<N> {
    @Nullable N getNullable() {
      return null;
    }

    @JsNonNull
    N getNonNullable() {
      throw new RuntimeException();
    }

    N getDefaultNullability() {
      return null;
    }
  }

  static class ParameterizedNullable<N extends @Nullable Object> {
    @Nullable N getNullable() {
      return null;
    }

    @JsNonNull
    N getNonNullable() {
      throw new RuntimeException();
    }

    N getDefaultNullability() {
      return null;
    }
  }

  static class ParameterizedNonNullable<N extends @JsNonNull Object> {
    @Nullable N getNullable() {
      return null;
    }

    @JsNonNull
    N getNonNullable() {
      throw new RuntimeException();
    }

    N getDefaultNullability() {
      return null;
    }
  }

  static class NonNullableInsideNullable<T extends @Nullable Object> {
    void nonNullableTest(ParameterizedNonNullable<T> nonNullable) {}
  }

  static <T extends String> void testDefaultNullabilityBounds(
      ParameterizedDefaultNullability<T> defaultNullability) {
    defaultNullability.getNonNullable().length();
    defaultNullability.getNullable().length();
    defaultNullability.getDefaultNullability().length();
  }

  static <T extends String> void testDefaultNullabilityBounds(ParameterizedNullable<T> nullable) {
    nullable.getNonNullable().length();
    nullable.getNullable().length();
    nullable.getDefaultNullability().length();
  }

  static <T extends @Nullable String> void testNullableBounds(
      ParameterizedDefaultNullability<T> defaultNullability) {
    defaultNullability.getNonNullable().length();
    defaultNullability.getNullable().length();
    defaultNullability.getDefaultNullability().length();
  }

  static <T extends @Nullable String> void testNullableBounds(ParameterizedNullable<T> nullable) {
    nullable.getNonNullable().length();
    nullable.getNullable().length();
    nullable.getDefaultNullability().length();
  }

  static <T extends @JsNonNull String> void testNonNullableBounds(
      ParameterizedDefaultNullability<T> defaultNullability) {
    defaultNullability.getNonNullable().length();
    defaultNullability.getNullable().length();
    defaultNullability.getDefaultNullability().length();
  }

  static <T extends @JsNonNull String> void testNonNullableBounds(
      ParameterizedNullable<T> nullable) {
    nullable.getNonNullable().length();
    nullable.getNullable().length();
    nullable.getDefaultNullability().length();
  }

  static <T extends @JsNonNull String> void testNonNullableBounds(
      ParameterizedNonNullable<T> nonNullable) {
    nonNullable.getNonNullable().length();
    nonNullable.getNullable().length();
    nonNullable.getDefaultNullability().length();
  }

  static void testDefaultNullabilityWildcards(
      ParameterizedDefaultNullability<? extends String> nonNullable) {
    nonNullable.getNonNullable().length();
    nonNullable.getNullable().length();
    nonNullable.getDefaultNullability().length();
  }

  static void testDefaultNullabilityWildcards(ParameterizedNullable<? extends String> nonNullable) {
    nonNullable.getNonNullable().length();
    nonNullable.getNullable().length();
    nonNullable.getDefaultNullability().length();
  }

  static void testDefaultNullabilityWildcards(
      ParameterizedNonNullable<? extends String> nonNullable) {
    nonNullable.getNonNullable().length();
    nonNullable.getNullable().length();
    nonNullable.getDefaultNullability().length();
  }

  static class Recursive<T extends @JsNonNull Recursive<T> & Marker> {}

  static class RecursiveNullable<T extends RecursiveNullable<T> & Marker> {}

  static class RecursiveChild extends Recursive<RecursiveChild> implements Marker {}

  static class RecursiveNullableChild extends RecursiveNullable<RecursiveNullableChild>
      implements Marker {}

  static class RecursiveParam<T extends Recursive<T> & Marker> {}

  static class RecursiveNullableParam<T extends RecursiveNullable<T> & Marker> {}

  static <T extends Recursive<T> & Marker> void testRecursive() {
    RecursiveParam<T> generic = new RecursiveParam<>();
    RecursiveParam<RecursiveChild> parametrized = new RecursiveParam<>();
  }

  static <T extends RecursiveNullable<T> & Marker> void testRecursiveNullable() {
    RecursiveNullableParam<T> generic = new RecursiveNullableParam<>();
    RecursiveNullableParam<RecursiveNullableChild> parametrized = new RecursiveNullableParam<>();
  }
}

interface Marker {}
