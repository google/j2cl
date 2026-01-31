/*
 * Copyright 2023 Google Inc.
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

import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Main {

  public static void main(String... args) {
    testVoid();
    testNullableVoid();

    testArrayLiteral();
    testNewArray();

    testExplicitInvocationTypeArguments();
    testExplicitConstructorTypeArguments();

    testImplicitInvocationTypeArguments();
    testImplicitConstructorTypeArguments();
    testRawConstructorTypeArguments();

    testImplicitInvocationTypeArgumentsWithWildcards();
    testImplicitConstructorTypeArgumentsWithWildcards();
    testRawConstructorTypeArgumentsWithWildcards();

    testImplicitConstructorTypeArgumentsWithInference();

    testLambdaReturnTypeInference();
    testUnsafeNull();
    testDefaultValue("");
    testNullWildcardInLambda();

    testLambdaParameterTypeInference();
  }

  private static final String STRING = "foo";
  private static final @Nullable String NULL_STRING = null;

  // Currently, both non-null and nullable Void are translated to nullable type in Kotlin, which is
  // consistent with checker framework, but inconsistent with JSpecify.
  private static void testVoid() {
    Box<@Nullable Void> voidBox = new Box<>(null);
    assertNull(voidBox.value);

    @Nullable Void v = null;
    assertNull(v);
  }

  private static void testNullableVoid() {
    assertNull(getNullableVoid());

    Box<@Nullable Void> voidBox = new Box<>(null);
    assertNull(voidBox.value);

    @Nullable Void v = (@Nullable Void) null;
    assertNull(v);
  }

  private static @Nullable Void getNullableVoid() {
    return null;
  }

  private static final class Box<T extends @Nullable Object> {
    final T value;

    Box(T value) {
      this.value = value;
    }
  }

  private static void testArrayLiteral() {
    @Nullable String[] unusedArray1 = {STRING, NULL_STRING};
    @Nullable String[] unusedArray2 = {NULL_STRING, STRING};
  }

  private static void testNewArray() {
    @Nullable String[] unusedArray1 = new @Nullable String[] {STRING, NULL_STRING};
    @Nullable String[] unusedArray2 = new @Nullable String[] {NULL_STRING, STRING};

    // Lack of @Nullable annotation in array creation expression should not cause NULL_STRING!!
    @Nullable String[] unusedArray3 = new String[] {STRING, NULL_STRING};
    @Nullable String[] unusedArray4 = new String[] {NULL_STRING, STRING};
  }

  private static void testExplicitInvocationTypeArguments() {
    Main.<@Nullable String>accept1(STRING);
    Main.<@Nullable String>accept1(NULL_STRING);

    Main.<@Nullable String>accept2(NULL_STRING, STRING);
    Main.<@Nullable String>accept2(STRING, NULL_STRING);

    Main.<@Nullable Object>acceptVarargs();
    Main.<@Nullable String>acceptVarargs(STRING);
    Main.<@Nullable String>acceptVarargs(NULL_STRING);
    Main.<@Nullable String>acceptVarargs(STRING, NULL_STRING);
    Main.<@Nullable String>acceptVarargs(NULL_STRING, STRING);
  }

  private static void testExplicitConstructorTypeArguments() {
    new Consumer<@Nullable String>(STRING);
    new Consumer<@Nullable String>(NULL_STRING);

    new Consumer<@Nullable String>(NULL_STRING, STRING);
    new Consumer<@Nullable String>(STRING, NULL_STRING);

    new VarargConsumer<@Nullable String>();
    new VarargConsumer<@Nullable String>(STRING);
    new VarargConsumer<@Nullable String>(NULL_STRING);
    new VarargConsumer<@Nullable String>(STRING, NULL_STRING);
    new VarargConsumer<@Nullable String>(NULL_STRING, STRING);
  }

  private static void testImplicitInvocationTypeArguments() {
    accept1(STRING);
    accept1(NULL_STRING);

    acceptVarargs();
    acceptVarargs(STRING);
    acceptVarargs(NULL_STRING);

    // T inferred as Any, instead of Any?
    accept1(null);

    // T inferred as String, instead of String?
    accept2(NULL_STRING, STRING);
    accept2(STRING, NULL_STRING);

    // T inferred as String, instead of String?
    acceptVarargs(STRING, NULL_STRING);
    acceptVarargs(NULL_STRING, STRING);
  }

  private static void testImplicitConstructorTypeArguments() {
    new Consumer<>(STRING);
    new Consumer<>(NULL_STRING);

    new VarargConsumer<>();
    new VarargConsumer<>(STRING);
    new VarargConsumer<>(NULL_STRING);

    // T inferred as Any, instead of Any?
    new Consumer<>(null);

    // T inferred as String, instead of String?
    new Consumer<>(NULL_STRING, STRING);
    new Consumer<>(STRING, NULL_STRING);

    // T inferred as String, instead of String?
    new VarargConsumer<>(STRING, NULL_STRING);
    new VarargConsumer<>(NULL_STRING, STRING);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testRawConstructorTypeArguments() {
    new Consumer(STRING);
    new Consumer(NULL_STRING);

    new VarargConsumer();
    new VarargConsumer(STRING);
    new VarargConsumer(NULL_STRING);

    new Consumer(null);

    new Consumer(NULL_STRING, STRING);
    new Consumer(STRING, NULL_STRING);

    new VarargConsumer(STRING, NULL_STRING);
    new VarargConsumer(NULL_STRING, STRING);
  }

  private static void testImplicitInvocationTypeArgumentsWithWildcards() {
    Supplier<?> supplier = Supplier.<@Nullable String>of(NULL_STRING);
    accept1(supplier.getValue());
    accept2(STRING, supplier.getValue());
    acceptVarargs(STRING, supplier.getValue());
  }

  private static void testImplicitConstructorTypeArgumentsWithWildcards() {
    Supplier<?> supplier = Supplier.<@Nullable String>of(NULL_STRING);
    new Consumer<>(supplier.getValue());
    new Consumer<>(STRING, supplier.getValue());
    new VarargConsumer<>(STRING, supplier.getValue());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testRawConstructorTypeArgumentsWithWildcards() {
    Supplier<?> supplier = Supplier.<@Nullable String>of(NULL_STRING);
    new Consumer(supplier.getValue());
    new Consumer(STRING, supplier.getValue());
    new VarargConsumer(STRING, supplier.getValue());
  }

  private static void testImplicitConstructorTypeArgumentsWithInference() {
    new Consumer<>(STRING, null).accept(NULL_STRING);
    new VarargConsumer<>(STRING, null).accept(NULL_STRING);
  }

  private static void testLambdaReturnTypeInference() {
    acceptSupplier(() -> null);
  }

  private static void testUnsafeNull() {
    // This line should not throw NPE, as the bound is nullable.
    Object x = getUnsafeNull();

    if (x != null) {
      // It should be safe to use `x` since there's explicit null-check.
      accept1(x.hashCode());
    }
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  private static <T extends @Nullable Object> T getUnsafeNull() {
    return null;
  }

  private static void testDefaultValue(@Nullable Object value) {
    if (value != null) {
      // This line should not throw NPE, even though the return type has non-null bound.
      Object x = getDefaultValue(value.getClass());

      if (x != null) {
        // It should be safe to use `x` since there's explicit null-check.
        accept1(x.hashCode());
      }
    }
  }

  private static <T> T getDefaultValue(Class<T> unusedClass) {
    return getUnsafeNull();
  }

  public static <C extends @Nullable Object> void testNullableAcceptNullable2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNullable2Varargs(localNonNull, nonNull, nonNull);
  }

  public static <C> void testNonNullAcceptNullable2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNullable2Varargs(localNonNull, nonNull, nonNull);
  }

  public static <C> void testNonNullAcceptNonNull2Vararg(C nonNull) {
    C localNonNull = nonNull;
    acceptNonNull2Varargs(localNonNull, nonNull, nonNull);
  }

  private static <T extends @Nullable Object> void accept1(T unused) {}

  private static <T extends @Nullable Object> void accept2(T unused1, T unused2) {}

  private static <T extends @Nullable Object> void acceptVarargs(T... unused) {}

  private static <T extends @Nullable Object> void acceptNullable2Varargs(
      T unused1, T unused2, T... unused) {}

  private static <T> void acceptNonNull2Varargs(T unused1, T unused2, T... unused) {}

  private static <V extends @Nullable Object> void acceptSupplier(Supplier<V> unused) {}

  private interface Supplier<V extends @Nullable Object> {
    V getValue();

    static <V extends @Nullable Object> Supplier<V> of(V v) {
      return () -> v;
    }
  }

  private static class Consumer<T extends @Nullable Object> {
    private Consumer(T unused) {}

    private Consumer(T unused1, T unused2) {}

    private void accept(T unused) {}
  }

  private static class VarargConsumer<T extends @Nullable Object> {
    private VarargConsumer(T... unused) {}

    private void accept(T unused) {}
  }

  private static void testNullWildcardInLambda() {
    Supplier<?> supplier = wrap(() -> null);
    supplier.getValue();
  }

  private static <T extends @Nullable Object> Supplier<T> wrap(Supplier<? extends T> supplier) {
    return () -> supplier.getValue();
  }

  private static void testLambdaParameterTypeInference() {
    assertTrue(1 == Main.<Integer>applyToNull(value -> (value == null) ? 1 : value + 1));
  }

  private static <T> int applyToNull(Function<? super @Nullable T, Integer> function) {
    return function.apply(null);
  }
}
