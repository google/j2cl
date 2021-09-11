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
package strings;

public class Strings {
  private String someString = "This is a string literal";
  private static final String ESCAPE_CODES = "\b\f\n\r\t\"\'\\\u0000\u007F𐍆：";
  private String nonBmpChar = "𐍆";
  private String wideColon = "：";
  private static final String ESCAPE_CODES_COPY = ESCAPE_CODES;

  private static class StringHolder {
    String s = "A";
  }

  public void test() {
    // Two Null String instances.
    String s1 = null;
    String s2 = null;
    String s3 = s1 + s2; // two nullable strings
    s2 += s2; // nullable string compound assignment, plus a nullable string.
    s1 += "a"; // nullable string compound assignment, plus a string literal.
    // multiple nullable string instances concatenation, and plus a string literal.
    s3 = s1 + s1 + s2 + null + "a";
    // a string literal plus multiple nullable string instances.
    s3 = "a" + s1 + s1 + s2 + null;

    // Char + String
    String s4;
    char c1 = 'F';
    char c2 = 'o';
    s4 = c1 + c2 + "o";
    s4 += 1L + "";

    // Compound operation and string conversions
    s4 += 1L;
    s4 += 'C';
    s4 += 1;
    s4 += 1d;
    s4 += 1f;
    s4 += (short) 1;

    (new StringHolder()).s += s4;
    (new StringHolder()).s += c1;
    (new StringHolder()).s += "o";

    s1 = 1 + 2 + "s";
    s1 = "s" + 1 + 2;
  }
}
