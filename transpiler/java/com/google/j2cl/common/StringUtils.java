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

  public static String escape(String string) {
    // The chars in the CharSequence are already in UTF16. Hence iterate over the 16 bits chars
    // and decide how to encode in the string.
    return string.chars().mapToObj(StringUtils::escape).collect(joining());
  }

  /** Decides how to encode a char as in a String. */
  public static String escape(int c) {
    checkArgument(c <= 0xFFFF);
    switch (c) {
      case 0x08: // backspace
        return "\\b";
      case 0x09: // tab
        return "\\t";
      case 0x0A: // newline
        return "\\n";
      case 0x0C: // formfeed
        return "\\f";
      case 0x0D: // return
        return "\\r";
      case 0x22: // "
        return "\\\"";
      case 0x27: // '
        return "\\'";
      case 0x5c: // \
        return "\\\\";
      default:
        if (c >= 0x20 && c < 0x7F) {
          // These characters do not need escaping
          return String.valueOf((char) c);
        }
        return String.format("\\u%04X", c);
    }
  }

  private StringUtils() {}
}
