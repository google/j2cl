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

import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

@NullMarked
public class Main {
  public static void main(String... args) {
    testVoid();
    testNullableVoid();
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
}
