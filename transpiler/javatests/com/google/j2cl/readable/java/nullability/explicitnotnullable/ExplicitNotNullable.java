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
package nullability.explicitnotnullable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

@NullMarked
public class ExplicitNotNullable {
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

  private String[][] f15 = {};
  private String[] @Nullable [] f16 = {};
  private String @Nullable [][] f17 = {};
  private String @Nullable [] @Nullable [] f18 = {};
  private @Nullable String[][] f19 = {};
  private @Nullable String[] @Nullable [] f20 = {};
  private @Nullable String @Nullable [][] f21 = {};
  private @Nullable String @Nullable [] @Nullable [] f22 = {};

  private char[][] f23 = {};
  private char[] @Nullable [] f24 = {};
  private char @Nullable [][] f25 = {};
  private char @Nullable [] @Nullable [] f26 = {};

  private String[] f27 = new String[1];
  private String[] f28 = new @Nullable String[1];
  private @Nullable String[] f29 = new String[1];
  private @Nullable String[] f30 = new @Nullable String[1];

  public ExplicitNotNullable() {
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
    @Nullable N nullable;
    @JsNonNull N nonNullable;
    N defaultNullability;

    ParameterizedDefaultNullability(N n) {
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

  static class ParameterizedNullable<N extends @Nullable Object> {
    @Nullable N nullable;
    @JsNonNull N nonNullable;
    N defaultNullability;

    ParameterizedNullable(N n) {
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

  static class ParameterizedNonNullable<N extends @JsNonNull Object> {
    @Nullable N nullable;
    @JsNonNull N nonNullable;
    N defaultNullability;

    ParameterizedNonNullable(N n) {
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

  interface Consumer<T extends @Nullable Object> {
    void accept(T t);
  }

  interface Supplier<T extends @Nullable Object> {
    T get();
  }

  interface IntFunction<T extends @Nullable Object> {
    T accept(int i);
  }

  static void testLambdaNullability() {
    Consumer<String> lambda = s -> {};
    Consumer<String> nullable = DefaultNullable::nullableAccept;
    Consumer<String> nonNullable = ExplicitNotNullable::nonNullableAccept;
    Supplier<Object> newObject = Object::new;
    IntFunction<Object[]> newArray = Object[]::new;
  }

  static void nonNullableAccept(String s) {}

  @Nullable Consumer<?> collection;

  @Nullable Consumer<? extends Object> nonNullableCollection;

  void unboundedWildCard(Consumer<?> c, Consumer<? extends Object> nc) {
    collection = c;
    nonNullableCollection = nc;
  }

  static <T> void consume(T t) {}

  static void testUnboundWildcardTypeArgumentInference(Consumer<?> c) {
    consume(c);
  }

  interface Function<I extends @Nullable Object, O extends @Nullable Object> {
    O apply(I i);
  }

  Function<String, String> i =
      new Function<String, String>() {
        @Override
        public String apply(String s) {
          return s;
        }
      };

  // Replicates wildcard problems in Guava's PairwiseEquivalence.
  static class DependentTypeParameters<E, T extends @Nullable E> {
    DependentTypeParameters<E, T> getThis() {
      return this;
    }
  }

  DependentTypeParameters<?, ?> testDependentWildcards(DependentTypeParameters<?, ?> x) {
    return x;
    // TODO(b/255955130): This is not yet working. Uncomment when fixed.
    // return x.getThis();
  }

  static void testLocalNullability() {
    Consumer<String> stringConsumer = (Consumer<String>) null;
    Consumer<@Nullable String> nullableStringConsumer = (Consumer<@Nullable String>) null;
    ;
    Consumer<@JsNonNull String> nonNullStringConsumer = (Consumer<@JsNonNull String>) null;

    boolean b = null instanceof Consumer<?>;
  }

  static String testParametrizedMethod(
      Function<? super String, ? extends String> f, String string) {
    // The type of "localString" is "@Nullable String".
    String localString = string;

    // The type of "apply" is inferred as "@Nullable String apply(@Nullable String)", so "!!" is not
    // inserted for "localString" parameter. But in Kotlin, the inferred type of "apply" is
    // "fun apply(String): String", and "!!" is required.
    return f.apply(localString);
  }

  static <T> T assertNotNull(@Nullable T nullable) {
    if (nullable == null) {
      throw new NullPointerException();
    }
    return nullable;
  }

  static void testAssertNotNull_parametrized(@Nullable String nullable) {
    String nonNull = assertNotNull(nullable);
  }

  static <T> void testAssertNotNull_notNullBounds(@Nullable T nullable) {
    T notNull = assertNotNull(nullable);
  }

  static <T extends @Nullable Object> void testAssertNotNull_nullableBounds(@Nullable T nullable) {
    T notNull = assertNotNull(nullable);
  }
}

class DefaultNullable {
  static void nullableAccept(String s) {}
}

// Repros fixed incosistency adding the outer parameter in ResolveCaptures.
@NullMarked
class OuterClass<E> {
  class InnerClass<E> {}

  {
    new InnerClass<E>() {};
    new InnerClass<String>() {};
  }
}
