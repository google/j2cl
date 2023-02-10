/*
 * Copyright 2023 Google Inc.
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
package javaemul.internal;

/** Utility methods for operating with Strings. */
public final class StringUtil {

  private static final String whitespaceRegexStr =
      "[\\u1680\\u180E\\u2000-\\u2006\\u2008-\\u200A\\u2028\\u2029\\u205F\\u3000\\uFEFF]"
          + "|[\\t-\\r ]"
          + "|[\\x1C-\\x1F]";

  private static NativeRegExp whitespaceRegex;

  public static boolean isWhitespace(String str) {
    if (whitespaceRegex == null) {
      // The regex would just be /\s/, but browsers handle non-breaking spaces inconsistently. Also,
      // the Java definition includes separators.
      whitespaceRegex = new NativeRegExp("^(" + whitespaceRegexStr + ")+$");
    }
    return whitespaceRegex.test(str);
  }

  private static final String spaceRegexStr =
      "[\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u202F\\u2028\\u2029\\u205F\\u3000]";

  private static NativeRegExp spaceRegex;

  public static boolean isSpace(String str) {
    if (spaceRegex == null) {
      spaceRegex = new NativeRegExp("^(" + spaceRegexStr + ")+$");
    }
    return spaceRegex.test(str);
  }

  private static NativeRegExp whitespaceOrSpaceRegex;

  public static boolean isWhitespaceOrSpace(String str) {
    if (whitespaceOrSpaceRegex == null) {
      whitespaceOrSpaceRegex =
          new NativeRegExp("^(" + whitespaceRegexStr + "|" + spaceRegexStr + ")+$");
    }
    return whitespaceOrSpaceRegex.test(str);
  }

  private StringUtil() {}
}
