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
package staticcompiletimeconstant;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {

  private static final String CONST_WITH_ESCAPES = "A\"SD\"F";

  // Class with circular static initialization used as a way to test that compile time constants are
  // initialized even before $clinit.
  private static class Foo {
    public static String circular = init();

    public static final String STRING_COMPILE_TIME_CONSTANT = "qwer";
    public static final byte BYTE_COMPILE_TIME_CONSTANT = 100;
    public static final short SHORT_COMPILE_TIME_CONSTANT = 100;
    public static final int INT_COMPILE_TIME_CONSTANT = 100;
    public static final long LONG_COMPILE_TIME_CONSTANT = 100L;
    public static final float FLOAT_COMPILE_TIME_CONSTANT = 100;
    public static final double DOUBLE_COMPILE_TIME_CONSTANT = 100;
    public static final char CHAR_COMPILE_TIME_CONSTANT = 100;
    public static final boolean BOOLEAN_COMPILE_TIME_CONSTANT = true;

    public static Object initialized = 1; // Intention boxing to avoid binaryen optimization

    private static String init() {
      assertTrue(initialized == null);
      assertTrue(STRING_COMPILE_TIME_CONSTANT == "qwer");
      assertTrue(BYTE_COMPILE_TIME_CONSTANT == 100);
      assertTrue(SHORT_COMPILE_TIME_CONSTANT == 100);
      assertTrue(INT_COMPILE_TIME_CONSTANT == 100);
      assertTrue(LONG_COMPILE_TIME_CONSTANT == 100);
      assertTrue(FLOAT_COMPILE_TIME_CONSTANT == 100);
      assertTrue(DOUBLE_COMPILE_TIME_CONSTANT == 100);
      assertTrue(CHAR_COMPILE_TIME_CONSTANT == 100);
      assertTrue(BOOLEAN_COMPILE_TIME_CONSTANT == true);

      return Bar.circular;
    }
  }

  // Class with circular static initialization used as a way to test that compile time constants are
  // initialized even before $clinit.
  private static class Bar {
    public static String circular = init();

    public static final String STRING_COMPILE_TIME_CONSTANT = "qwer";
    public static final byte BYTE_COMPILE_TIME_CONSTANT = 100;
    public static final short SHORT_COMPILE_TIME_CONSTANT = 100;
    public static final int INT_COMPILE_TIME_CONSTANT = 100;
    public static final long LONG_COMPILE_TIME_CONSTANT = 100;
    public static final float FLOAT_COMPILE_TIME_CONSTANT = 100;
    public static final double DOUBLE_COMPILE_TIME_CONSTANT = 100;
    public static final char CHAR_COMPILE_TIME_CONSTANT = 100;
    public static final boolean BOOLEAN_COMPILE_TIME_CONSTANT = true;

    public static Object initialized = 1; // Intention boxing to avoid binaryen optimization

    private static String init() {
      assertTrue(initialized == null);
      assertTrue(STRING_COMPILE_TIME_CONSTANT == "qwer");
      assertTrue(BYTE_COMPILE_TIME_CONSTANT == 100);
      assertTrue(SHORT_COMPILE_TIME_CONSTANT == 100);
      assertTrue(INT_COMPILE_TIME_CONSTANT == 100);
      assertTrue(LONG_COMPILE_TIME_CONSTANT == 100);
      assertTrue(FLOAT_COMPILE_TIME_CONSTANT == 100);
      assertTrue(DOUBLE_COMPILE_TIME_CONSTANT == 100);
      assertTrue(CHAR_COMPILE_TIME_CONSTANT == 100);
      assertTrue(BOOLEAN_COMPILE_TIME_CONSTANT == true);

      return Foo.circular;
    }
  }

  @SuppressWarnings("unused")
  public static void main(String... args) {
    // Trigger the $clinit;
    String a = Bar.circular;

    // Verify that even compile time constants handle string escaping the same as regular strings.
    assertTrue(Main.CONST_WITH_ESCAPES.equals("A\"SD\"F"));
  }
}
