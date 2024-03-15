/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package stringescapecodes;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/**
 * Test String escape codes.
 */
public class Main {

  static final String DEL_CHARACTER = "" + (char) 127;

  public static void main(String... args) {
    testEmptyStringLiteral();
    testSpecialEscapes();
    testUnicodeEscapes();
    testOctalEscapes();
    testInvalidEscapes();
  }

  private static void testEmptyStringLiteral() {
    assertTrue("".length() == 0);
  }

  private static void testSpecialEscapes() {
    assertEquals("\b", String.valueOf((char) 8));
    assertEquals("\t", String.valueOf((char) 9));
    assertEquals("\n", String.valueOf((char) 10));
    assertEquals("\f", String.valueOf((char) 12));
    assertEquals("\r", String.valueOf((char) 13));
    assertEquals("\"", String.valueOf((char) 34));
    assertEquals("\'", String.valueOf((char) 39));
    assertEquals("\\", String.valueOf((char) 92));
    assertEquals(DEL_CHARACTER, String.valueOf((char) 127));
  }

  private static void testUnicodeEscapes() {
    assertTrue("\u0000".length() == 1);

    // This is how the gothic letter "faihu" is represented in unicode.
    String faihu = "\uD800\uDF46";
    assertEquals(faihu, "𐍆");
    assertTrue(faihu.codePointAt(0) == 0x10346);
    // Enable when supported in j2kt.
    // assertTrue(faihu.codePointCount(0, faihu.length()) == 1);
    // This string has 1 codepoint but its length as a string is 2.
    assertTrue(faihu.length() == 2);

    assertTrue("\uFFFF".charAt(0) == 0xFFFF);
    assertTrue("\uFFFF".length() == 1);
  }

  private static void testOctalEscapes() {
    assertEquals("\0", String.valueOf((char) 0));
    assertEquals("\1", String.valueOf((char) 1));
    assertEquals("\2", String.valueOf((char) 2));
    assertEquals("\3", String.valueOf((char) 3));
    assertEquals("\4", String.valueOf((char) 4));
    assertEquals("\5", String.valueOf((char) 5));
    assertEquals("\6", String.valueOf((char) 6));
    assertEquals("\7", String.valueOf((char) 7));
    assertEquals("\00", String.valueOf((char) 0));
    assertEquals("\01", String.valueOf((char) 1));
    assertEquals("\02", String.valueOf((char) 2));
    assertEquals("\03", String.valueOf((char) 3));
    assertEquals("\04", String.valueOf((char) 4));
    assertEquals("\05", String.valueOf((char) 5));
    assertEquals("\06", String.valueOf((char) 6));
    assertEquals("\07", String.valueOf((char) 7));
    assertEquals("\55", String.valueOf((char) (5 * 8 + 5)));
    assertEquals("\055", String.valueOf((char) (5 * 8 + 5)));
    assertEquals("\155", String.valueOf((char) (1 * 64 + 5 * 8 + 5)));
    assertEquals("\0155", String.valueOf((char) (1 * 8 + 5) + "5"));

    assertEquals("\u001b[31m", "\033[31m");
  }

  private static void testInvalidEscapes() {
    // Even though the escape sequence "\\uXXXX" has a meaning in Unicode, in Java each of those
    // is interpreted as the numeric value of a char. It is possible to construct strings that
    // are not valid unicode strings, such as the ones here where there is only the first character
    // for a surrogate pair.
    assertTrue("\uD801".charAt(0) == 0xd801);
    assertTrue("\uD800".charAt(0) == 0xd800);
    assertTrue("\uDC00".charAt(0) == 0xdC00);
    // An invalid surrogate pair: the lead byte of a proper pair followed by an invalid value for
    // the second value.
    assertTrue("\uD801\t".charAt(0) == 0xd801);
    assertTrue("\uD801\t".charAt(1) == 0x9);
    assertTrue("\uD801\t".codePointAt(0) == 0xd801);
    assertTrue("\uD801\t".codePointAt(1) == 0x9);

    // Test concatenation involving malformed UTF-16.
    String faihu = "\uD800\uDF46";
    String fai = "\uD800";
    String hu = "\uDF46";
    assertTrue(faihu.equals(fai + hu));
  }
}
