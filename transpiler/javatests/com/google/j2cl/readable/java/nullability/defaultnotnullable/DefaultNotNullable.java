/*
 * Copyright 2021 Google Inc.
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
package nullability.defaultnotnullable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import org.jspecify.nullness.Nullable;

public class DefaultNotNullable {
  private String f1 = "Hello";
  @Nullable private String f2 = null;
  @javax.annotation.Nullable private String f3 = null;
  private List<String> f4 = new ArrayList<>();
  @Nullable private List<String> f5 = new ArrayList<>();
  private List<@Nullable String> f6 = new ArrayList<>();
  @Nullable private List<@Nullable String> f7 = null;
  private String[] f8 = {};
  // Nonnullable array of nullable strings.
  @Nullable private String[] f9 = {};
  // Nullable array of non-nullable strings.
  private String @Nullable [] f10 = {};
  private Void f11 = null;
  @Nonnull private Object f12 = new Object();
  private Object f13;
  @Nullable private Object f14;

  public DefaultNotNullable() {
    f13 = new Object();
  }

  public String m1(String a, List<Double> b) {
    return "";
  }

  @Nullable
  public String m2(@Nullable String a, List<@Nullable Double> b) {
    return null;
  }

  @JsMethod
  public void m3(String... args) {
  }

  interface ParameterizedInterface<T> {
    // Exposes the problems described in b/68726480.
    T add(T t);

    @Nullable
    T nullableAdd(@Nullable T t);
  }

  class ImplementsParameterizedInterface implements ParameterizedInterface<String> {
    @Override
    public String add(String s) {
      return "Hey";
    }

    public @Nullable String nullableAdd(@Nullable String s) {
      return "Hey";
    }
  }

  interface NullableBound<T extends @Nullable NullableBound<T>> {}

  interface NonNullableBound<T extends NonNullableBound<T>> {}

  <T extends @Nullable NullableBound<T>> void methodWithNullableBound() {}

  <T extends NonNullableBound<T>> void methodWithNonNullableBound() {}

  interface NullableBoundWithNonNullArgument
      extends NullableBound<NullableBoundWithNonNullArgument> {}

  interface NullableBoundWithNullableArgument
      extends NullableBound<@Nullable NullableBoundWithNullableArgument> {}

  interface NonNullBoundWithNonNullArgument
      extends NonNullableBound<NonNullBoundWithNonNullArgument> {}

  interface NonNullBoundWithNullableArgument
      extends NonNullableBound<@Nullable NonNullBoundWithNullableArgument> {}

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
}
