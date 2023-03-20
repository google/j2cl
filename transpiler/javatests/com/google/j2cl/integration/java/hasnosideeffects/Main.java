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
package hasnosideeffects;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import javaemul.internal.annotations.HasNoSideEffects;

public class Main {
  public static void main(String... args) {
    testSideEffectInQualifier();
  }

  private static class ClassWithMethodWithoutSideEffects {
    private int unusedField = 0;

    @HasNoSideEffects
    private ClassWithMethodWithoutSideEffects methodWithoutSideEffects() {
      return this;
    }

    private static void staticMethod() {}
  }

  private static int sideEffects = 0;

  private static final ClassWithMethodWithoutSideEffects INSTANCE =
      new ClassWithMethodWithoutSideEffects();

  private static ClassWithMethodWithoutSideEffects getInstanceWithSideEffect() {
    sideEffects++;
    return INSTANCE;
  }

  private static void testSideEffectInQualifier() {
    assertEquals(0, sideEffects);
    getInstanceWithSideEffect().methodWithoutSideEffects();
    assertEquals(1, sideEffects);
    getInstanceWithSideEffect().methodWithoutSideEffects().staticMethod();
    assertEquals(2, sideEffects);
    getInstanceWithSideEffect().unusedField++;
    assertEquals(3, sideEffects);
  }
}
