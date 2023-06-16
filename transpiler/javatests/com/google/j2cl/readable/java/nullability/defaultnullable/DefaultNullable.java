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
import org.jspecify.nullness.NullMarked;
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
    @Nullable N nullable;
    @JsNonNull N nonNullable;
    N defaultNullability;

    ParameterizedDefaultNullability(@JsNonNull N nonNullable, N defaultNullability) {
      this.nonNullable = nonNullable;
      this.defaultNullability = defaultNullability;
    }

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

    void setNullable(@Nullable N n) {}

    void setNonNull(@JsNonNull N n) {}

    void setDefaultNullability(N n) {}
  }

  static class ParameterizedNullable<N extends @Nullable Object> {
    @Nullable N nullable;
    @JsNonNull N nonNullable;
    N defaultNullability;

    ParameterizedNullable(@JsNonNull N nonNullable, N defaultNullability) {
      this.nonNullable = nonNullable;
      this.defaultNullability = defaultNullability;
    }

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

    void setNullable(@Nullable N n) {}

    void setNonNull(@JsNonNull N n) {}

    void setDefaultNullability(N n) {}
  }

  static class ParameterizedNonNullable<N extends @JsNonNull Object> {
    @Nullable N nullable;
    @JsNonNull N nonNullable;
    N defaultNullability;

    ParameterizedNonNullable(@JsNonNull N n) {
      this.nonNullable = n;
      this.defaultNullability = n;
    }

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

    void setNullable(@Nullable N n) {}

    void setNonNull(@JsNonNull N n) {}

    void setDefaultNullability(N n) {}
  }

  static <N extends @Nullable Object> void genericNullableMethod(
      ParameterizedNullable<N> o, @Nullable N n) {
    o.setNullable(n);
    o.setNonNull(n);
    o.setDefaultNullability(n);

    o.getNullable().hashCode();
    o.getNonNullable().hashCode();
    o.getDefaultNullability().hashCode();

    o.nullable = n;
    o.nonNullable = n;
    o.defaultNullability = n;

    o.nullable.hashCode();
    o.nonNullable.hashCode();
    o.defaultNullability.hashCode();
  }

  static <N extends @JsNonNull Object> void genericNonNullMethod(
      ParameterizedNonNullable<N> o, @JsNonNull N n) {
    o.setNullable(n);
    o.setNonNull(n);
    o.setDefaultNullability(n);

    o.getNullable().hashCode();
    o.getNonNullable().hashCode();
    o.getDefaultNullability().hashCode();

    o.nullable = n;
    o.nonNullable = n;
    o.defaultNullability = n;

    o.nullable.hashCode();
    o.nonNullable.hashCode();
    o.defaultNullability.hashCode();
  }

  static <N> void genericDefaultNullabilityMethod(ParameterizedDefaultNullability<N> o, N n) {
    o.setNullable(n);
    o.setNonNull(n);
    o.setDefaultNullability(n);

    o.getNullable().hashCode();
    o.getNonNullable().hashCode();
    o.getDefaultNullability().hashCode();

    o.nullable = n;
    o.nonNullable = n;
    o.defaultNullability = n;

    o.nullable.hashCode();
    o.nonNullable.hashCode();
    o.defaultNullability.hashCode();
  }

  static void parametrizedNullableMethod(
      ParameterizedNullable<@Nullable String> o, @Nullable String s) {
    o.setNullable(s);
    o.setNonNull(s);
    o.setDefaultNullability(s);

    o.getNullable().length();
    o.getNonNullable().length();
    o.getDefaultNullability().length();

    o.nullable = s;
    o.nonNullable = s;
    o.defaultNullability = s;

    o.nullable.length();
    o.nonNullable.length();
    o.defaultNullability.length();
  }

  static void parametrizedNonNullMethod(
      ParameterizedNonNullable<@JsNonNull String> o, @JsNonNull String s) {
    o.setNullable(s);
    o.setNonNull(s);
    o.setDefaultNullability(s);

    o.getNullable().length();
    o.getNonNullable().length();
    o.getDefaultNullability().length();

    o.nullable = s;
    o.nonNullable = s;
    o.defaultNullability = s;

    o.nullable.length();
    o.nonNullable.length();
    o.defaultNullability.length();
  }

  static void parametrizedDefaultNullabilityMethod(
      ParameterizedDefaultNullability<String> o, String s) {
    o.setNullable(s);
    o.setNonNull(s);
    o.setDefaultNullability(s);

    o.getNullable().length();
    o.getNonNullable().length();
    o.getDefaultNullability().length();

    o.nullable = s;
    o.nonNullable = s;
    o.defaultNullability = s;

    o.nullable.length();
    o.nonNullable.length();
    o.defaultNullability.length();
  }

  public void casts() {
    List<String> listOfString = (List<String>) null;
    @Nullable List<String> nullableListOfString = (@Nullable List<String>) null;
    List<@Nullable String> listOfNullableString = (List<@Nullable String>) null;
    @Nullable List<@Nullable String> nullableListOfNullableString =
        (@Nullable List<@Nullable String>) null;
    @JsNonNull List<String> nonNullListOfString = (@JsNonNull List<String>) null;
    List<@JsNonNull String> listOfNonNullString = (List<@JsNonNull String>) null;
    @JsNonNull
    List<@JsNonNull String> nonNullListOfNonNullString = (@JsNonNull List<@JsNonNull String>) null;
  }

  // Wildcards
  static void testListOfWildcard(List<?> l) {}

  static void testListOfWildcardExtendsDefaultNullabilityObject(List<? extends Object> l) {}

  static void testListOfWildcardExtendsNullableObject(List<? extends @Nullable Object> l) {}

  static void testListOfWildcardExtendsNonNullObject(List<? extends @JsNonNull Object> l) {}

  static void testListOfWildcardExtendsDefaultNullabilityString(List<? extends String> l) {}

  static void testListOfWildcardExtendsNullableString(List<? extends @Nullable String> l) {}

  static void testListOfWildcardExtendsNonNullString(List<? extends @JsNonNull String> l) {}

  static <T> void testListOfWildcardExtendsDefaultNullabilityVariable(List<? extends T> l) {}

  static <T> void testListOfWildcardExtendsNullableVariable(List<? extends @Nullable T> l) {}

  static void testListOfWildcardSuperDefaultNullabilityObject(List<? super Object> l) {}

  static void testListOfWildcardSuperNullableObject(List<? super @Nullable Object> l) {}

  static void testListOfWildcardSuperNonNullObject(List<? super @JsNonNull Object> l) {}

  static void testListOfWildcardSuperDefaultNullabilityString(List<? super String> l) {}

  static void testListOfWildcardSuperNullableString(List<? super @Nullable String> l) {}

  static void testListOfWildcardSuperNonNullString(List<? super @JsNonNull String> l) {}

  static <T> void testListOfWildcardSuperDefaultNullabilityVariable(List<? super T> l) {}

  static <T> void testListOfWildcardSuperNullableVariable(List<? super @Nullable T> l) {}

  interface Consumer<T> {
    void accept(T t);
  }

  static void testLocalNullability() {
    Consumer<String> stringConsumer = (Consumer<String>) null;
    Consumer<@Nullable String> nullableStringConsumer = (Consumer<@Nullable String>) null;
    Consumer<@JsNonNull String> nonNullStringConsumer = (Consumer<@JsNonNull String>) null;

    boolean b = null instanceof Consumer<?>;
  }

  @NullMarked
  interface NullMarkedSupplier<T extends @Nullable Object> {
    T get();
  }

  @NullMarked
  interface NullMarkedConsumer<T extends @Nullable Object> {
    void accept(T t);
  }

  @NullMarked
  interface NullMarkedIntFunction<T extends @Nullable Object> {
    T accept(int i);
  }

  static void testNonNullableLambdas() {
    NullMarkedConsumer<String> lambda = s -> {};
    NullMarkedSupplier<String> constructorReference = String::new;
    NullMarkedIntFunction<String[]> newArrayReference = String[]::new;
  }

  static void accept(String s) {}
}
