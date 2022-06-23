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
package nullability.defaultnullable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import org.jspecify.nullness.Nullable;

public class DefaultNullable {
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
  public String m1(String a, List<Double> b) {
    return "";
  }

  @Nonnull private Object f13;
  private Object f14;

  public DefaultNullable() {
    f13 = new Object();
  }

  @Nullable
  public String m2(@Nullable String a, List<@Nullable Double> b) {
    return null;
  }

  @JsMethod
  public void m3(String... args) {
  }

  interface NullableBound<T extends NullableBound<T>> {}

  interface NonNullableBound<T extends @JsNonNull NonNullableBound<T>> {}

  <T extends NullableBound<T>> void methodWithNullableBound() {}

  <T extends @JsNonNull NonNullableBound<T>> void methodWithNonNullableBound() {}

  interface NullableBoundWithNonNullArgument
      extends NullableBound<@JsNonNull NullableBoundWithNonNullArgument> {}

  interface NullableBoundWithNullableArgument
      extends NullableBound<NullableBoundWithNullableArgument> {}

  interface NonNullBoundWithNonNullArgument
      extends NonNullableBound<NonNullBoundWithNonNullArgument> {}

  interface NonNullBoundWithNullableArgument
      extends NonNullableBound<NonNullBoundWithNullableArgument> {}

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
