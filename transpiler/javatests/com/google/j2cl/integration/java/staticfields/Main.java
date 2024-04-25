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
package staticfields;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/** Test static field initializer, and static field reference in field declaration. */
public class Main {
  public static void main(String... args) {
    testStaticFields_visibilities();
    testStaticFields_superInterface();
    testStaticFields_internal();
  }

  public static int a = 2;
  protected static int b = a * 3;

  private static void testStaticFields_visibilities() {
    assertTrue(Main.a == 2);
    assertTrue(Main.b == 6);
  }

  // Fields and methods that start with $ do not get a prefix. This is a static field and will be
  // rewritten into getter/setter plus backing field. The backing field name will also start with
  // $, but in this case since it is synthetic it will be appropriately prefixed.
  public static int $internal = 3;

  private static void testStaticFields_internal() {
    assertTrue(Main.$internal == 3);
    Main.$internal++;
    assertTrue(Main.$internal == 4);
  }

  interface Super {
    Object SUPERFIELD = new Object();
  }

  interface Sub extends Super {}

  private static void testStaticFields_superInterface() {
    assertTrue(Sub.SUPERFIELD != null);
  }
}
