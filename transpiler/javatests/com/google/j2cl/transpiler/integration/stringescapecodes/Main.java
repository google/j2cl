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
package com.google.j2cl.transpiler.integration.stringescapecodes;

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
    assert "".length() == 0;
  }

  private static void testSpecialEscapes() {
    assert "\b".equals(String.valueOf((char) 8));
    assert "\t".equals(String.valueOf((char) 9));
    assert "\n".equals(String.valueOf((char) 10));
    assert "\f".equals(String.valueOf((char) 12));
    assert "\r".equals(String.valueOf((char) 13));
    assert "\"".equals(String.valueOf((char) 34));
    assert "\'".equals(String.valueOf((char) 39));
    assert "\\".equals(String.valueOf((char) 92));
    assert DEL_CHARACTER.equals(String.valueOf((char) 127));
  }

  private static void testUnicodeEscapes() {
    assert "\uD800\uDF46".equals("𐍆");
    assert "\u0000".length() == 1;
  }

  private static void testOctalEscapes() {
    assert "\0".equals(String.valueOf((char) 0));
    assert "\1".equals(String.valueOf((char) 1));
    assert "\2".equals(String.valueOf((char) 2));
    assert "\3".equals(String.valueOf((char) 3));
    assert "\4".equals(String.valueOf((char) 4));
    assert "\5".equals(String.valueOf((char) 5));
    assert "\6".equals(String.valueOf((char) 6));
    assert "\7".equals(String.valueOf((char) 7));
    assert "\00".equals(String.valueOf((char) 0));
    assert "\01".equals(String.valueOf((char) 1));
    assert "\02".equals(String.valueOf((char) 2));
    assert "\03".equals(String.valueOf((char) 3));
    assert "\04".equals(String.valueOf((char) 4));
    assert "\05".equals(String.valueOf((char) 5));
    assert "\06".equals(String.valueOf((char) 6));
    assert "\07".equals(String.valueOf((char) 7));
    assert "\55".equals(String.valueOf((char) (5 * 8 + 5)));
    assert "\055".equals(String.valueOf((char) (5 * 8 + 5)));
    assert "\155".equals(String.valueOf((char) (1 * 64 + 5 * 8 + 5)));
    assert "\0155".equals(String.valueOf((char) (1 * 8 + 5) + "5"));

    assert "\u001b[31m".equals("\033[31m");
  }
}
