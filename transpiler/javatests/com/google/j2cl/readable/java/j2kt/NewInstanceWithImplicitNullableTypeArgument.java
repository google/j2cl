/*
 * Copyright 2026 Google Inc.
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

@NullMarked
public final class NewInstanceWithImplicitNullableTypeArgument {
  public static class ParameterizedEmptyClass<T extends @Nullable Object> {
    public ParameterizedEmptyClass() {}

    public ParameterizedEmptyClass(T t) {}
  }

  public static class SubParameterizedEmptyClass<T extends @Nullable Object>
      extends ParameterizedEmptyClass<T> {
    public SubParameterizedEmptyClass() {}

    public SubParameterizedEmptyClass(T t) {}
  }

  public static Object testFromArguments(@Nullable String nullableString) {
    return new ParameterizedEmptyClass<>(nullableString);
  }

  public static void testFromDeclaration() {
    ParameterizedEmptyClass<@Nullable String> emptyClass = new ParameterizedEmptyClass<>();
  }

  public static void testFromAssignment() {
    ParameterizedEmptyClass<@Nullable String> emptyClass;
    emptyClass = new ParameterizedEmptyClass<>();
  }

  public static ParameterizedEmptyClass<@Nullable String> testFromReturnType() {
    return new ParameterizedEmptyClass<>();
  }

  public static void testImplicitTypeArguments_fromParameterType() {
    acceptOfNullableString(new ParameterizedEmptyClass<>());
  }

  public static void acceptOfNullableString(ParameterizedEmptyClass<@Nullable String> emptyClass) {}

  public static void testFromSupertypeDeclaration() {
    ParameterizedEmptyClass<@Nullable String> emptyClass = new SubParameterizedEmptyClass<>();
  }

  public static void testFromSupertypeAssignment() {
    ParameterizedEmptyClass<@Nullable String> emptyClass;
    emptyClass = new SubParameterizedEmptyClass<>();
  }

  public static ParameterizedEmptyClass<@Nullable String> testFromSupertypeReturnType() {
    return new SubParameterizedEmptyClass<>();
  }

  public static void testFromSupertypeArgument() {
    acceptOfNullableString(new SubParameterizedEmptyClass<>());
  }

  public static ParameterizedEmptyClass<@Nullable String> testFromSupertypeAndArguments(
      @Nullable String nullableString) {
    return new SubParameterizedEmptyClass<>(nullableString);
  }

  public static void testWithMultpleTypeParameters() {
    class Parent<T extends @Nullable Object> {}
    class Child<U extends @Nullable Object, V extends @Nullable Object> extends Parent<V> {
      Child(U u) {}
    }
    Parent<String> parent = new Child<>(1);
  }
}
