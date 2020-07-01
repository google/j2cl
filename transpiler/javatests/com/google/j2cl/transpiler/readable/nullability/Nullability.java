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
package com.google.j2cl.transpiler.readable.nullability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import org.checkerframework.checker.nullness.compatqual.NullableType;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Nullability {
  @Nonnull private String f1 = "Hello";
  private String f2 = null;
  @Nullable private String f3 = null;
  private @Nonnull List<@NonNull String> f4 = new ArrayList<>();
  private List<@NonNull String> f5 = new ArrayList<>();
  private @Nonnull List<String> f6 = new ArrayList<>();
  private List<String> f7 = null;
  private @NonNull String @NonNull [] f8 = {};
  // Nonnullable array of nullable strings.
  private String @NonNull [] f9 = {};
  // Nullable array of non-nullable strings.
  private @NonNull String[] f10 = {};
  // Conversion from generic type parameter.
  private List<String> f12 = new ArrayList<>();

  @JsConstructor
  public Nullability(@Nonnull String a) {}

  @Nonnull
  public String m1(@Nonnull String a, @Nonnull List<@NonNull Double> b, String c) {
    return "";
  }

  public String m2(String a, @Nonnull List<@NullableType Double> b) {
    return "";
  }

  @JsMethod
  public String m3(@Nonnull String a, @NonNull String... args) {
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

  public static class StringList extends ArrayList<String> {}

  // Should implement Comparator<string>
  public static class StringComparator implements Comparator<@NonNull String> {
    @Override
    public int compare(@Nonnull String a, @Nonnull String b) {
      return 0;
    }
  }

  // Should implement Comparator<?string>
  public static class NullableStringComparator implements Comparator<@NullableType String> {
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
}
