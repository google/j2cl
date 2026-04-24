/*
 * Copyright 2026 Google Inc.
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
package com.google.j2cl.transpiler.frontend.common;

/** Converts character offsets to UTF-8 byte offsets. */
public final class Utf8ByteOffsetConverter {
  // Array where the index is the charOffset and the value is the byteOffset.
  // Null if the string is purely ASCII.
  private final int[] byteOffsets;

  private static final Utf8ByteOffsetConverter ASCII_ONLY_INSTANCE =
      new Utf8ByteOffsetConverter(null);

  public static Utf8ByteOffsetConverter create(CharSequence src) {
    int length = src.length();
    int[] offsets = new int[length + 1];
    int currentByteOffset = 0;
    boolean isAsciiOnly = true;

    for (int i = 0; i < length; i++) {
      offsets[i] = currentByteOffset;
      char c = src.charAt(i);

      int byteLen;
      if (c <= 0x007F) {
        byteLen = 1;
      } else {
        isAsciiOnly = false;
        if (c <= 0x07FF || Character.isSurrogate(c)) {
          byteLen = 2;
        } else {
          byteLen = 3;
        }
      }
      currentByteOffset += byteLen;
    }

    // Store the total byte length at the very end
    offsets[length] = currentByteOffset;

    if (isAsciiOnly) {
      return ASCII_ONLY_INSTANCE;
    }

    return new Utf8ByteOffsetConverter(offsets);
  }

  private Utf8ByteOffsetConverter(int[] byteOffsets) {
    this.byteOffsets = byteOffsets;
  }

  /**
   * Returns the UTF-8 byte offset corresponding to the given character offset.
   *
   * <p>If the requested character offset falls precisely between a high and low surrogate pair, the
   * returned offset is exactly 2 bytes past the start of the 4-byte supplementary character.
   */
  public int getByteOffset(int charOffset) {
    if (byteOffsets == null) {
      return charOffset;
    }
    return byteOffsets[charOffset];
  }
}
