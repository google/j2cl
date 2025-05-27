/*
 * Copyright 2017 Google Inc.
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
package fieldmethodclasscollision;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import javaemul.internal.annotations.KtDisabled;

/**
 * Test field, method and class name collision.
 */
public class Main {
  private static class Foo {}

  public static int Foo = 1;

  public static int Foo() {
    return 2;
  }

  public static void main(String... args) {
    assertTrue(new Foo() instanceof Foo);
    assertTrue(Foo == 1);
    testMethodConstructorCollision();
  }

  // TODO(b/219914876): Name collision
  @KtDisabled
  private static void testMethodConstructorCollision() {
    assertTrue(Foo() == 2);
  }
}
