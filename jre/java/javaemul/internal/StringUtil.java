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

  public static String replace(String str, char from, char to, boolean ignoreCase) {
    return nativeReplace(str, escapeForRegExpSearch(from), to, /* replaceAll= */ true, ignoreCase);
  }

  public static String replace(String str, CharSequence from, CharSequence to, boolean ignoreCase) {
    // Implementation note: This uses a regex replacement instead of
    // a string literal replacement because Safari does not
    // follow the spec for "$$" in the replacement string: it
    // will insert a literal "$$". IE and Firefox, meanwhile,
    // treat "$$" as "$".

    return nativeReplace(
        str,
        escapeForRegExpSearch(from),
        escapeForRegExpSearch(to),
        /* replaceAll= */ true,
        ignoreCase);
  }

  /**
   * Regular expressions vary from the standard implementation. The <code>regex</code> parameter is
   * interpreted by JavaScript as a JavaScript regular expression. For consistency, use only the
   * subset of regular expression syntax common to both Java and JavaScript.
   *
   * <p>TODO(jat): properly handle Java regex syntax
   */
  public static String replaceAll(String str, String regex, String replace, boolean ignoreCase) {
    return nativeReplace(str, regex, replace, /* replaceAll= */ true, ignoreCase);
  }

  /**
   * Regular expressions vary from the standard implementation. The <code>regex</code> parameter is
   * interpreted by JavaScript as a JavaScript regular expression. For consistency, use only the
   * subset of regular expression syntax common to both Java and JavaScript.
   *
   * <p>TODO(jat): properly handle Java regex syntax
   */
  public static String replaceFirst(String str, String regex, String replace, boolean ignoreCase) {
    return nativeReplace(str, regex, replace, /* replaceAll= */ false, ignoreCase);
  }

  /** Replaces the first instance of the literal match with the literal replacement. */
  public static String replaceFirstLiteral(String str, char from, char to, boolean ignoreCase) {
    return nativeReplace(str, escapeForRegExpSearch(from), to, /* replaceAll= */ false, ignoreCase);
  }

  /** Replaces the first instance of the literal match with the literal replacement. */
  public static String replaceFirstLiteral(
      String str, CharSequence from, CharSequence to, boolean ignoreCase) {
    return nativeReplace(
        str,
        escapeForRegExpSearch(from),
        escapeForRegExpReplacement(to),
        /* replaceAll= */ false,
        ignoreCase);
  }

  private static String nativeReplace(
      String str, String regex, String replace, boolean replaceAll, boolean ignoreCase) {
    String flags = (replaceAll ? "g" : "") + (ignoreCase ? "i" : "");
    return str.nativeReplace(new NativeRegExp(regex, flags), translateReplaceString(replace));
  }

  private static String nativeReplace(
      String str, String regex, char replace, boolean replaceAll, boolean ignoreCase) {
    String flags = (replaceAll ? "g" : "") + (ignoreCase ? "i" : "");
    return str.nativeReplace(new NativeRegExp(regex, flags), replace);
  }

  /**
   * This method converts Java-escaped dollar signs "\$" into JavaScript-escaped dollar signs "$$",
   * and removes all other lone backslashes, which serve as escapes in Java but are passed through
   * literally in JavaScript.
   */
  private static String translateReplaceString(String replaceStr) {
    int pos = 0;
    while (0 <= (pos = replaceStr.indexOf("\\", pos))) {
      if (replaceStr.charAt(pos + 1) == '$') {
        replaceStr = replaceStr.substring(0, pos) + "$" + replaceStr.substring(++pos);
      } else {
        replaceStr = replaceStr.substring(0, pos) + replaceStr.substring(++pos);
      }
    }
    return replaceStr;
  }

  private static String escapeForRegExpSearch(char c) {
    // Translate 'from' into unicode escape sequence (\\u and a four-digit hexadecimal number).
    // Escape sequence replacement is used instead of a string literal replacement
    // in order to escape regexp special characters (e.g. '.').
    String hex = Integer.toHexString(c);
    return "\\u" + "0000".substring(hex.length()) + hex;
  }

  /** Escapes the given CharSerquence such that is can be used in a RegExp search. */
  private static String escapeForRegExpSearch(CharSequence str) {
    return str.toString().replaceAll("([/\\\\\\.\\*\\+\\?\\|\\(\\)\\[\\]\\{\\}$^])", "\\\\$1");
  }

  /** Escapes the given CharSerquence such that is can be used in a RegExp repleacement. */
  private static String escapeForRegExpReplacement(CharSequence str) {
    // Escape $ since it is for match backrefs and \ since it is used to escape $.
    return str.toString().toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\$");
  }

  private StringUtil() {}
}
