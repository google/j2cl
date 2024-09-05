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
package j2ktnotpassing;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// TODO(b/361769898): Remove when fixed
public class NullabilityScopes {
  @NullMarked
  public static class NullMarkedScope {
    public static class Foo<K extends @Nullable Object> {
      public Foo() {}

      public Foo(Foo<? extends K> foo) {}
    }

    public void testNonNullMarkedWildcardConstructorImplicitTypeArguments(
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
  }
}
