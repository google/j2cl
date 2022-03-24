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
package fieldshadowing;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/** Test field shadowing and super field access. */
public class Main {

  interface IntSupplier {
    int get();
  }

  private static class Foo {
    int value = 1;
  }

  private static class SubFoo extends Foo {
    int value = 2;

    int value() {
      return value;
    }

    int superValue() {
      return super.value;
    }

    private class InnerSubFoo {
      public int value() {
        return value;
      }

      public int superValue() {
        return SubFoo.super.value;
      }
    }

    IntSupplier valueSupplier = () -> value;
    IntSupplier superValueSupplier = () -> super.value;
  }

  public static void main(String... args) {
    assertTrue(new SubFoo().value() == 2);
    assertTrue(new SubFoo().value == 2);
    assertTrue(new SubFoo().new InnerSubFoo().value() == 2);
    assertTrue(new SubFoo().valueSupplier.get() == 2);
    assertTrue(new SubFoo().value == 2);
    assertTrue(new SubFoo().superValue() == 1);
    assertTrue(new SubFoo().new InnerSubFoo().superValue() == 1);
    assertTrue(new SubFoo().superValueSupplier.get() == 1);
    assertTrue(new Foo().value == 1);
  }
}
