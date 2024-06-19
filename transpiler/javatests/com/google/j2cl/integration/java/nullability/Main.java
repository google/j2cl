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
import static com.google.j2cl.integration.testing.TestUtils.isJ2Kt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Main {
  public static void main(String... args) {
    testVoid();
    testNullableVoid();
    testExplicitTypeArguments();
    testImplicitTypeArguments();
  }

  // Currently, both non-null and nullable Void are translated to nullable type in Kotlin, which is
  // consistent with checker framework, but inconsistent with JSpecify.
  private static void testVoid() {
    assertNull(getVoid());

    Box<Void> voidBox = new Box<>(null);
    assertNull(voidBox.value);

    Void v = (Void) null;
    assertNull(v);
  }

  private static void testNullableVoid() {
    assertNull(getNullableVoid());

    Box<@Nullable Void> voidBox = new Box<>(null);
    assertNull(voidBox.value);

    @Nullable Void v = (@Nullable Void) null;
    assertNull(v);
  }

  private static Void getVoid() {
    return null;
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

  private static void testExplicitTypeArguments() {
    String string = "foo";
    @Nullable String nullableString = null;

    Main.<@Nullable String>accept1(string);
    Main.<@Nullable String>accept1(nullableString);

    Main.<@Nullable String>accept2(nullableString, string);
    Main.<@Nullable String>accept2(string, nullableString);

    Main.<@Nullable Object>acceptVarargs();
    Main.<@Nullable String>acceptVarargs(string);
    Main.<@Nullable String>acceptVarargs(nullableString);
    Main.<@Nullable String>acceptVarargs(string, nullableString);
    Main.<@Nullable String>acceptVarargs(nullableString, string);
  }

  private static void testImplicitTypeArguments() {
    String string = "foo";
    @Nullable String nullableString = null;

    accept1(string);
    accept1(nullableString);

    acceptVarargs();
    acceptVarargs(string);
    acceptVarargs(nullableString);

    // TODO(b/324940602): Use TestUtils.isJ2ktWeb() when it's implemented, or...
    // TODO(b/324550390): Remove the condition when the bug is fixed.
    if (!isJ2Kt()) {
      // T inferred as Any, instead of Any?
      accept1(null);

      // T inferred as String, instead of String?
      accept2(nullableString, string);
      accept2(string, nullableString);

      // T inferred as String, instead of String?
      acceptVarargs(string, nullableString);
      acceptVarargs(nullableString, string);
    }
  }

  private static <T extends @Nullable Object> void accept1(T t) {}

  private static <T extends @Nullable Object> void accept2(T t1, T t2) {}

  private static <T extends @Nullable Object> void acceptVarargs(T... t) {}
}
