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
package staticblocklocalvar;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * References (directly and as a captured variable in an anonymous class) a local variable defined
 * inside of a static block to show that the "declaring class" finding logic can handle the
 * situation.
 */
public class Main {
  private static int directlySetValue = 0;
  private static IGetter indirectValueGetter;

  static {
    final int someLocalVar = 999;

    // Referencing a local variable defined in a block (not method) directly.
    directlySetValue = someLocalVar;

    // Referencing a local variable defined in a block (not method) via variable capture.
    indirectValueGetter =
        new IGetter() {
          @Override
          public int get() {
            return someLocalVar;
          }
        };
  }

  public static void main(String... args) {
    assertTrue(directlySetValue == 999);
    assertTrue(indirectValueGetter.get() == 999);
  }
}
