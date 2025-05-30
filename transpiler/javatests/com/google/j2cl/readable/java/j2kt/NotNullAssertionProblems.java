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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** A test covering cases with unnecessary not-null {@code !!} assertions. */
@NullMarked
public class NotNullAssertionProblems {
  public void testArrayLiteral(String string, @Nullable String nullableString) {
    @Nullable String[] array0 = {null};
    @Nullable String[] array1 = {string, null};
    @Nullable String[] array2 = {string, nullableString};
    @Nullable String[] array3 = {null, string};
    @Nullable String[] array4 = {nullableString, string};
  }

  public void testNewArray(String string, @Nullable String nullableString) {
    @Nullable String[] array0 = new @Nullable String[] {null};
    @Nullable String[] array1 = new @Nullable String[] {string, null};
    @Nullable String[] array2 = new @Nullable String[] {string, nullableString};
    @Nullable String[] array3 = new @Nullable String[] {null, string};
    @Nullable String[] array4 = new @Nullable String[] {nullableString, string};
  }

  public void testExplicitInvocationTypeArguments(
      String string, Supplier<String> supplier, @Nullable String nullableString) {
    NotNullAssertionProblems.<@Nullable String>accept1(null);

    NotNullAssertionProblems.<@Nullable String>accept2(string, null);
    NotNullAssertionProblems.<@Nullable String>accept2(string, nullableString);
    NotNullAssertionProblems.<@Nullable String>accept2(null, string);
    NotNullAssertionProblems.<@Nullable String>accept2(nullableString, string);

    NotNullAssertionProblems.<@Nullable String>acceptVararg(string, null);
    NotNullAssertionProblems.<@Nullable String>acceptVararg(string, nullableString);
    NotNullAssertionProblems.<@Nullable String>acceptVararg(null, string);
    NotNullAssertionProblems.<@Nullable String>acceptVararg(nullableString, string);

    NotNullAssertionProblems.<@Nullable String>acceptGeneric(supplier, nullableString);
    NotNullAssertionProblems.<@Nullable String>acceptUpperBound(supplier, nullableString);
  }

  public void testImplicitInvocationTypeArguments(
      String string, Supplier<String> supplier, @Nullable String nullableString) {
    accept1(null);

    accept2(string, null);
    accept2(string, nullableString);
    accept2(null, string);
    accept2(nullableString, string);

    acceptVararg(string, null);
    acceptVararg(string, nullableString);
    acceptVararg(null, string);
    acceptVararg(nullableString, string);

    acceptGeneric(supplier, nullableString);
    acceptUpperBound(supplier, nullableString);
  }

  public void testExplicitConstructorTypeArguments(
      String string, Supplier<String> supplier, @Nullable String nullableString) {
    new Consumer<@Nullable String>(null);

    new Consumer<@Nullable String>(string, null);
    new Consumer<@Nullable String>(string, nullableString);
    new Consumer<@Nullable String>(null, string);
    new Consumer<@Nullable String>(nullableString, string);

    new VarargConsumer<@Nullable String>(string, null);
    new VarargConsumer<@Nullable String>(string, nullableString);
    new VarargConsumer<@Nullable String>(null, string);
    new VarargConsumer<@Nullable String>(nullableString, string);

    new GenericConsumer<@Nullable String>(supplier, nullableString);
    new UpperWildcardConsumer<@Nullable String>(supplier, nullableString);
  }

  public void testImplicitConstructorTypeArguments(
      String string, Supplier<String> supplier, @Nullable String nullableString) {
    new Consumer<>(null);

    new Consumer<>(string, null);
    new Consumer<>(string, nullableString);
    new Consumer<>(null, string);
    new Consumer<>(nullableString, string);

    new VarargConsumer<>(string, null);
    new VarargConsumer<>(string, nullableString);
    new VarargConsumer<>(null, string);
    new VarargConsumer<>(nullableString, string);

    new GenericConsumer<>(supplier, nullableString);
    new UpperWildcardConsumer<>(supplier, nullableString);
  }

  public void testImplicitRawConstructorTypeArguments(
      String string, Supplier<String> supplier, @Nullable String nullableString) {
    new Consumer(null);

    new Consumer(string, null);
    new Consumer(string, nullableString);
    new Consumer(null, string);
    new Consumer(nullableString, string);

    new VarargConsumer(string, null);
    new VarargConsumer(string, nullableString);
    new VarargConsumer(null, string);
    new VarargConsumer(nullableString, string);

    new GenericConsumer(supplier, nullableString);
    new UpperWildcardConsumer(supplier, nullableString);
  }

  public static void testImplicitInvocationTypeArguments_wildcards(
      String string, Supplier<?> wildcardSupplier) {
    // Non-null assertion should not be inserted for {@code supplier.getValue()}.
    accept1(wildcardSupplier.getValue());
    accept2(string, wildcardSupplier.getValue());
    acceptVararg(string, wildcardSupplier.getValue());
  }

  public static void testImplicitInvocationTypeArguments_rawTypes(
      Supplier nonNull, @Nullable Supplier nullable) {
    accept2(nonNull, nullable);
    accept2(nullable, nonNull);
    acceptVararg(nonNull, nullable);
    acceptVararg(nullable, nonNull);
  }

  public static void testImplicitConstructorTypeArguments_wildcards(
      String string, Supplier<?> wildcardSupplier) {
    // Non-null assertion should not be inserted for {@code supplier.getValue()}.
    new Consumer<>(wildcardSupplier.getValue());
    new Consumer<>(string, wildcardSupplier.getValue());
    new VarargConsumer<>(string, wildcardSupplier.getValue());
  }

  public static void testRawConstructorTypeArguments_wildcards(
      String string, Supplier<?> wildcardSupplier) {
    // Non-null assertion should not be inserted for {@code supplier.getValue()}.
    new Consumer(wildcardSupplier.getValue());
    new Consumer(string, wildcardSupplier.getValue());
    new VarargConsumer(string, wildcardSupplier.getValue());
  }

  public static void testImplicitConstructorTypeArguments_inference(String string) {
    // Non-null assertion should not be inserted for {@code accept(null)}.
    new Consumer<>(string, null).accept(null);
    new VarargConsumer<>(string, null).accept(null);
  }

  public static <T> void testUnsafeNull() {
    // This line should not throw NPE, as the bound is nullable.
    Object x = getUnsafeNull();

    if (x != null) {
      // It should be safe to use `x` since there's explicit null-check.
      accept1(x.hashCode());
    }
  }

  public static <C extends @Nullable String> void testNullableAcceptNullable2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNullable2Vararg(localNonNull, nonNull, nonNull);
  }

  public static <C extends String> void testNonNullAcceptNullable2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNullable2Vararg(localNonNull, nonNull, nonNull);
  }

  public static <C extends String> void testNonNullAcceptNonNull2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNonNull2Vararg(localNonNull, nonNull, nonNull);
  }

  public static <U extends String, C extends U>
      void testNonNullParametericBoundAcceptNullable2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNullable2Vararg(localNonNull, nonNull, nonNull);
  }

  public interface Class1 {}

  public interface Interface1 {}

  public interface Interface2 {}

  public static <C extends Class1 & Interface1>
      void testNonNullClassInterfaceIntersectionAcceptNullable2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNullable2Vararg(localNonNull, nonNull, nonNull);
  }

  public static <C extends Interface1 & Interface2>
      void testNonNullInterfaceInterfaceIntersectionAcceptNullable2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNullable2Vararg(localNonNull, nonNull, nonNull);
  }

  public static <T extends @Nullable Object> T getUnsafeNull() {
    return null;
  }

  public static <T> void testDefaultValue(@Nullable Object value) {
    if (value != null) {
      // This line should not throw NPE, even though the return type has non-null bound.
      Object x = getDefaultValue(value.getClass());

      if (x != null) {
        // It should be safe to use `x` since there's explicit null-check.
        accept1(x.hashCode());
      }
    }
  }

  public static <T> T getDefaultValue(Class<T> cls) {
    return getUnsafeNull();
  }

  public static <T extends @Nullable Object> void accept1(T t) {}

  public static <T extends @Nullable Object> void accept2(T t1, T t2) {}

  public static <T extends @Nullable Object> void acceptUpperBound(
      Supplier<? extends T> t1, T t2) {}

  public static <T extends @Nullable Object> void acceptGeneric(Supplier<T> t1, T t2) {}

  public static <T extends @Nullable Object> void acceptVararg(T... varargs) {}

  public static <T extends @Nullable Object> void acceptNullable2Vararg(T t1, T t2, T... varargs) {}

  public static <T> void acceptNonNull2Vararg(T t1, T t2, T... varargs) {}

  public static class Consumer<T extends @Nullable Object> {
    public Consumer(T t) {}

    public Consumer(T t1, T t2) {}

    public void accept(T t) {}
  }

  public static class VarargConsumer<T extends @Nullable Object> {
    public VarargConsumer(T... ts) {}

    public void accept(T t) {}
  }

  public static class GenericConsumer<T extends @Nullable Object> {
    public GenericConsumer(Supplier<T> supplier, T t) {}

    public void accept(T t) {}
  }

  public static class UpperWildcardConsumer<T extends @Nullable Object> {
    public UpperWildcardConsumer(Supplier<? extends T> supplier, T t) {}

    public void accept(T t) {}
  }

  static class C<V extends @Nullable Object> {
    V defaultValue = (V) null;

    V f() {
      return true ? defaultValue : defaultValue;
    }
  }

  public interface Supplier<V extends @Nullable Object> {
    V getValue();
  }

  public static void testNullWildcardInLambda() {
    Supplier<?> supplier = wrap(() -> null);
    supplier.getValue();
  }

  public static <T extends @Nullable Object> Supplier<T> wrap(Supplier<? extends T> supplier) {
    return () -> supplier.getValue();
  }
}
