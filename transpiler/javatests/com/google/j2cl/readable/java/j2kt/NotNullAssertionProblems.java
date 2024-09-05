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
  public void testExplicitInvocationTypeArguments() {
    NotNullAssertionProblems.<@Nullable String>accept1(null);

    NotNullAssertionProblems.<@Nullable String>accept2("foo", null);
    NotNullAssertionProblems.<@Nullable String>accept2("foo", nullableString());

    NotNullAssertionProblems.<@Nullable String>acceptVararg("foo", null);
    NotNullAssertionProblems.<@Nullable String>acceptVararg("foo", nullableString());
  }

  public void testImplicitInvocationTypeArguments() {
    accept1(null);

    accept2("foo", null);
    accept2("foo", nullableString());

    acceptVararg("foo", null);
    acceptVararg("foo", nullableString());
  }

  public void testExplicitConstructorTypeArguments() {
    new Consumer<@Nullable String>(null);

    new Consumer<@Nullable String>("foo", null);
    new Consumer<@Nullable String>("foo", nullableString());

    new VarargConsumer<@Nullable String>("foo", null);
    new VarargConsumer<@Nullable String>("foo", nullableString());
  }

  public void testImplicitConstructorTypeArguments() {
    new Consumer<>(null);

    new Consumer<>("foo", null);
    new Consumer<>("foo", nullableString());

    new VarargConsumer<>("foo", null);
    new VarargConsumer<>("foo", nullableString());
  }

  public void testImplicitRawConstructorTypeArguments() {
    new Consumer(null);

    new Consumer("foo", null);
    new Consumer("foo", nullableString());

    new VarargConsumer("foo", null);
    new VarargConsumer("foo", nullableString());
  }

  public static void testImplicitInvocationTypeArguments_wildcards(Supplier<?> wildcardSupplier) {
    // Non-null assertion should not be inserted for {@code supplier.getValue()}.
    accept1(wildcardSupplier.getValue());
    accept2("foo", wildcardSupplier.getValue());
    acceptVararg("foo", wildcardSupplier.getValue());
  }

  public static void testImplicitConstructorTypeArguments_wildcards(Supplier<?> wildcardSupplier) {
    // Non-null assertion should not be inserted for {@code supplier.getValue()}.
    new Consumer<>(wildcardSupplier.getValue());
    new Consumer<>("foo", wildcardSupplier.getValue());
    new VarargConsumer<>("foo", wildcardSupplier.getValue());
  }

  public static void testRawConstructorTypeArguments_wildcards(Supplier<?> wildcardSupplier) {
    // Non-null assertion should not be inserted for {@code supplier.getValue()}.
    new Consumer(wildcardSupplier.getValue());
    new Consumer("foo", wildcardSupplier.getValue());
    new VarargConsumer("foo", wildcardSupplier.getValue());
  }

  public static void testImplicitConstructorTypeArguments_inference() {
    // Non-null assertion should not be inserted for {@code accept(null)}.
    new Consumer<>("foo", null).accept(null);
    new VarargConsumer<>("foo", null).accept(null);
  }

  public static <T extends @Nullable Object> void accept1(T t) {}

  public static <T extends @Nullable Object> void accept2(T t1, T t2) {}

  public static <T extends @Nullable Object> void acceptVararg(T... varargs) {}

  public static class Consumer<T extends @Nullable Object> {
    public Consumer(T t) {}

    public Consumer(T t1, T t2) {}

    public void accept(T t) {}
  }

  public static class VarargConsumer<T extends @Nullable Object> {
    public VarargConsumer(T... ts) {}

    public void accept(T t) {}
  }

  public static @Nullable String nullableString() {
    return null;
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
}
