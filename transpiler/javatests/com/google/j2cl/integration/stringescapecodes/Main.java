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
package com.google.j2cl.integration.stringescapecodes;

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
  }

  private static void testEmptyStringLiteral() {
    assertTrue("".length() == 0);
  }

  private static void testSpecialEscapes() {
    assertTrue("\b".equals(String.valueOf((char) 8)));
    assertTrue("\t".equals(String.valueOf((char) 9)));
    assertTrue("\n".equals(String.valueOf((char) 10)));
    assertTrue("\f".equals(String.valueOf((char) 12)));
    assertTrue("\r".equals(String.valueOf((char) 13)));
    assertTrue("\"".equals(String.valueOf((char) 34)));
    assertTrue("\'".equals(String.valueOf((char) 39)));
    assertTrue("\\".equals(String.valueOf((char) 92)));
    assertTrue(DEL_CHARACTER.equals(String.valueOf((char) 127)));
  }

  private static void testUnicodeEscapes() {
    assertTrue("\uD800\uDF46".equals("𐍆"));
    assertTrue("\u0000".length() == 1);
  }

  private static void testOctalEscapes() {
    assertTrue("\0".equals(String.valueOf((char) 0)));
    assertTrue("\1".equals(String.valueOf((char) 1)));
    assertTrue("\2".equals(String.valueOf((char) 2)));
    assertTrue("\3".equals(String.valueOf((char) 3)));
    assertTrue("\4".equals(String.valueOf((char) 4)));
    assertTrue("\5".equals(String.valueOf((char) 5)));
    assertTrue("\6".equals(String.valueOf((char) 6)));
    assertTrue("\7".equals(String.valueOf((char) 7)));
    assertTrue("\00".equals(String.valueOf((char) 0)));
    assertTrue("\01".equals(String.valueOf((char) 1)));
    assertTrue("\02".equals(String.valueOf((char) 2)));
    assertTrue("\03".equals(String.valueOf((char) 3)));
    assertTrue("\04".equals(String.valueOf((char) 4)));
    assertTrue("\05".equals(String.valueOf((char) 5)));
    assertTrue("\06".equals(String.valueOf((char) 6)));
    assertTrue("\07".equals(String.valueOf((char) 7)));
    assertTrue("\55".equals(String.valueOf((char) (5 * 8 + 5))));
    assertTrue("\055".equals(String.valueOf((char) (5 * 8 + 5))));
    assertTrue("\155".equals(String.valueOf((char) (1 * 64 + 5 * 8 + 5))));
    assertTrue("\0155".equals(String.valueOf((char) (1 * 8 + 5) + "5")));

    assertTrue("\u001b[31m".equals("\033[31m"));
  }
}
