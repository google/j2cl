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
package systemgetproperty;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;

public class Main {

  private static int counter = 0;

  public static void main(String... args) {
    assertTrue(System.getProperty("jre.classMetadata").equals("SIMPLE"));
    assertTrue(System.getProperty("jre.bar", "bar").equals("bar"));

    assertTrue(counter == 0);
    assertTrue(
        System.getProperty("jre.classMetadata", getDefaultWithSideEffect()).equals("SIMPLE"));
    // Side effect should not be lost.
    assertTrue(counter == 1);

    testReadingPropertyBeforeRegistration();
  }

  @Wasm("nop")
  private static void testReadingPropertyBeforeRegistration() {
    // If the code is compiled we'll inline
    if ("true".equals(System.getProperty("COMPILED"))) {
      return;
    }

    // Reading bazz and then registering it should yield a runtime error.
    assertTrue(System.getProperty("bazz", "default").equals("default"));
    try {
      registerBazz();
      fail();
    } catch (Exception ex) {
      assertEquals(
          "Error: System property \"bazz\" read before registration via"
              + " jre.addSystemPropertyFromGoogDefine.",
          ex.getMessage());
    }
  }

  private static String getDefaultWithSideEffect() {
    counter++;
    return "Foo";
  }

  @JsMethod
  private static native void registerBazz();
}
