/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.common;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;

/** Utilities to produce Strings in code. */
public final class StringUtils {

  /** Return the String with first letter capitalized. */
  public static String capitalize(String string) {
    if (string.isEmpty()) {
      return string;
    }
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }

  public static String escapeAsWtf16(String string) {
    // The chars in the CharSequence are already in WTF16. Hence iterate over the 16 bits chars
    // and decide how to encode in the string.
    return string.chars().mapToObj(StringUtils::escapeAsWtf16).collect(joining());
  }

  public static String escapeAsWtf16(int c) {
    return escape(c, /* forUtf8= */ false);
  }

  /** Converts a potentially ill-formed UTF-16 string (WTF-16) into a UTF-8 string literal. */
  public static String escapeAsUtf8(String string) {
    StringBuilder escaped = new StringBuilder();
    string
        .codePoints()
        .forEach(
            codepoint -> {
              if (codepoint < 0x80) {
                escaped.append(escapeAsUtf8(codepoint));
              } else if (codepoint < 0x800) {
                escaped.append(escapeAsUtf8(0xC0 | (codepoint >> 6))); // upper bits
                escaped.append(escapeAsUtf8(0x80 | (codepoint & 0x3F))); // bits 0-5
              } else if (codepoint < 0x10000) {
                escaped.append(escapeAsUtf8(0xE0 | (codepoint >> 12))); // upper bits
                escaped.append(escapeAsUtf8(0x80 | ((codepoint >> 6) & 0x3F))); // bits 6-11
                escaped.append(escapeAsUtf8(0x80 | (codepoint & 0x3F))); // bits 0-5
              } else {
                escaped.append(escapeAsUtf8(0xF0 | (codepoint >> 18))); // upper bits
                escaped.append(escapeAsUtf8(0x80 | ((codepoint >> 12) & 0x3F))); // bits 12-17
                escaped.append(escapeAsUtf8(0x80 | ((codepoint >> 6) & 0x3F))); // bits 6-11
                escaped.append(escapeAsUtf8(0x80 | (codepoint & 0x3F))); // bits 0-5
              }
            });
    return escaped.toString();
  }

  public static String escapeAsUtf8(int c) {
    return escape(c, /* forUtf8= */ true);
  }

  /** Produce a readable encoding of a byte in a String. */
  private static String escape(int c, boolean forUtf8) {
    switch (c) {
      case 0x09: // tab
        return "\\t";
      case 0x0A: // newline
        return "\\n";
      case 0x0D: // return
        return "\\r";
      case 0x22: // "
        return "\\\"";
      case 0x27: // '
        return "\\'";
      case 0x5c: // \
        return "\\\\";
    }

    // The rest of the ascii range characters do not need escaping in either representation.
    if (c >= 0x20 && c < 0x7F) {
      return String.valueOf((char) c);
    }

    if (forUtf8) {
      checkArgument(c >= 0 && c <= 0xFF);
      return String.format("\\%02X", (byte) c);
    } else {
      return String.format("\\u%04X", c);
    }
  }

  private StringUtils() {}
}
