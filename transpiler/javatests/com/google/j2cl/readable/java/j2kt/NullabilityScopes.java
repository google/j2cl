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

import j2kt.NullabilityScopes.NullMarkedScope.Array;
import j2kt.NullabilityScopes.NullMarkedScope.Cell;
import j2kt.NullabilityScopes.NullMarkedScope.Table;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public class NullabilityScopes {
  @NullMarked
  public static class NullMarkedScope {
    public static class Foo<K extends @Nullable Object> {
      public Foo() {}

      public Foo(Foo<? extends K> foo) {}
    }

    public interface Array<T extends @Nullable Object> {}

    public interface Cell<T extends Cell<T>> {}

    public interface Table<T extends Cell<T>> {
      Array<@Nullable T> getNullableCells();
    }

    public void testNullMarkedWildcardConstructorRaw(NullMarkedScope.Foo<String> nullMarkedFoo) {
      new NullMarkedScope.Foo(nullMarkedFoo);
    }

    public void testNullMarkedWildcardConstructorImplicitTypeArguments(
        NullMarkedScope.Foo<String> nullMarkedFoo) {
      new NullMarkedScope.Foo<>(nullMarkedFoo);
    }

    public void testNullMarkedWildcardConstructorExplicitTypeArguments(
        NullMarkedScope.Foo<String> nullMarkedFoo) {
      new NullMarkedScope.Foo<String>(nullMarkedFoo);
    }

    public void testNonNullMarkedWildcardConstructorRaw(
        NonNullMarkedScope.Foo<String> nonNullMarkedFoo) {
      new NonNullMarkedScope.Foo(nonNullMarkedFoo);
    }

    public void testNonNullMarkedWildcardConstructorImplicitTypeArguemnts(
        NonNullMarkedScope.Foo<String> nonNullMarkedFoo) {
      new NonNullMarkedScope.Foo<>(nonNullMarkedFoo);
    }

    public void testNonNullMarkedWildcardConstructorExplicitTypeArguments(
        NonNullMarkedScope.Foo<String> nonNullMarkedFoo) {
      new NonNullMarkedScope.Foo<String>(nonNullMarkedFoo);
    }
  }

  public static class NonNullMarkedScope {
    public static class Foo<K extends @Nullable Object> {
      public Foo() {}

      public Foo(Foo<? extends K> foo) {}
    }

    public void testNullMarkedWildcardConstructorRaw(NullMarkedScope.Foo<String> nullMarkedFoo) {
      new NullMarkedScope.Foo(nullMarkedFoo);
    }

    public void testNullMarkedWildcardConstructorImplicitTypeArguments(
        NullMarkedScope.Foo<String> nullMarkedFoo) {
      new NullMarkedScope.Foo<>(nullMarkedFoo);
    }

    public void testNullMarkedWildcardConstructorExplicitTypeArguments(
        NullMarkedScope.Foo<String> nullMarkedFoo) {
      new NullMarkedScope.Foo<String>(nullMarkedFoo);
    }

    public void testNonNullMarkedWildcardConstructorRaw(
        NonNullMarkedScope.Foo<String> nonNullMarkedFoo) {
      new NonNullMarkedScope.Foo(nonNullMarkedFoo);
    }

    public void testNonNullMarkedWildcardConstructorImplicitTypeArguments(
        NonNullMarkedScope.Foo<String> nonNullMarkedFoo) {
      new NonNullMarkedScope.Foo<>(nonNullMarkedFoo);
    }

    public void testNonNullMarkedWildcardConstructorExplicitTypeArguments(
        NonNullMarkedScope.Foo<String> nonNullMarkedFoo) {
      new NonNullMarkedScope.Foo<String>(nonNullMarkedFoo);
    }

    public Array<? extends Cell<?>> testRecursiveWildcardConversion(
        Table<? extends Cell<?>> table) {
      return table.getNullableCells();
    }
  }
}
