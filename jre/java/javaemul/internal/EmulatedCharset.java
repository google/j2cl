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
package javaemul.internal;

import java.nio.charset.Charset;

/**
 * Provides Charset implementations.
 */
public abstract class EmulatedCharset extends Charset {

  public static final EmulatedCharset UTF_8 = new UtfCharset("UTF-8");

  public static final EmulatedCharset ISO_LATIN_1 = new LatinCharset("ISO-LATIN-1");

  public static final EmulatedCharset ISO_8859_1 = new LatinCharset("ISO-8859-1");

  private static class LatinCharset extends EmulatedCharset {
    public LatinCharset(String name) {
      super(name);
    }

    @Override
    public byte[] getBytes(char[] buffer, int offset, int count, boolean throwOnInvalid) {
      int n = offset + count;
      byte[] bytes = new byte[0];
      // Pre-allocate to avoid re-sizing. Not using "new byte[n]" to avoid unnecessary init w/ `0`.
      bytes = ArrayHelper.setLength(bytes, count);
      for (int i = offset; i < n; ++i) {
        bytes[i] = (byte) (buffer[i] & 255);
      }
      return bytes;
    }

    @Override
    public byte[] getBytes(String str, boolean throwOnInvalid) {
      int n = str.length();
      byte[] bytes = new byte[0];
      // Pre-allocate to avoid re-sizing. Not using "new byte[n]" to avoid unnecessary init w/ `0`.
      bytes = ArrayHelper.setLength(bytes, n);
      for (int i = 0; i < n; ++i) {
        bytes[i] = (byte) (str.charAt(i) & 255);
      }
      return bytes;
    }

    @Override
    public char[] decodeString(byte[] bytes, int ofs, int len, boolean throwOnInvalid) {
      char[] chars = new char[0];
      // Pre-allocate to avoid re-sizing. Not using "new char[n]" to avoid unnecessary init w/ `0`.
      chars = ArrayHelper.setLength(chars, len);
      for (int i = 0; i < len; ++i) {
        chars[i] = (char) (bytes[ofs + i] & 255);
      }
      return chars;
    }
  }

  private static class UtfCharset extends EmulatedCharset {
    public UtfCharset(String name) {
      super(name);
    }

    private static final char REPLACEMENT_CHAR = '\uFFFD';

    @Override
    public char[] decodeString(byte[] bytes, int ofs, int len, boolean throwOnInvalid) {
      // TODO(b/229151472): Consider using TextEncoder/TextDecoder instead.

      char[] chars = new char[0];
      // Pre-allocate to avoid re-sizing. Not using "new char[n]" to avoid unnecessary init w/ `0`.
      // Note that we allocate double the length of the string to account for a worst-case string
      // containing only surrogate pairs. We'll resize the array down after decoding.
      chars = ArrayHelper.setLength(chars, len * 2);
      int outIdx = 0;
      int count = 0;
      for (int i = 0; i < len; ) {
        boolean invalid = false;
        int runStartIdx = i;
        int ch = bytes[ofs + i++];
        if ((ch & 0x80) == 0) {
          count = 1;
          ch &= 127;
        } else if ((ch & 0xE0) == 0xC0) {
          count = 2;
          ch &= 31;
        } else if ((ch & 0xF0) == 0xE0) {
          count = 3;
          ch &= 15;
        } else if ((ch & 0xF8) == 0xF0) {
          count = 4;
          ch &= 7;
        } else {
          // no 5+ byte sequences since max codepoint is less than 2^21, or this is an unexpected
          // continuation.
          invalid = true;
          count = 1;
        }

        int end = i + count - 1;
        if (end > len) {
          if (throwOnInvalid) {
            throw new IndexOutOfBoundsException();
          } else {
            invalid = true;
          }
        }

        while (!invalid && i < end) {
          byte b = bytes[ofs + i++];
          if ((b & 0xC0) != 0x80) {
            // If the byte doesn't have continuation markers then this is unexpected as this is a
            // start of a new char. We'll break here, decrement the i, and reread it as if it were
            // the start of a new run.
            invalid = true;
            i--;
          } else {
            ch = (ch << 6) | (b & 63);
          }
        }

        // We should have a code point for a full pair. If we ended up in the surrogate range then
        // we have a unpaired high/low.
        if (ch <= Character.MAX_VALUE && Character.isSurrogate((char) ch)) {
          invalid = true;
        }

        if (invalid || isOverlong(ch, count)) {
          if (throwOnInvalid) {
            throw new IllegalArgumentException();
          } else {
            // All the bytes we've read in this run are invalid, for each byte output a replacement
            // char.
            int j = runStartIdx;
            while (j++ < i) {
              chars[outIdx++] = REPLACEMENT_CHAR;
            }
          }
        } else {
          outIdx += Character.toChars(ch, chars, outIdx);
        }
      }
      // We might have over allocated initially; resize back.
      chars = ArrayHelper.setLength(chars, outIdx);
      return chars;
    }

    private static boolean isOverlong(int codepoint, int count) {
      return (codepoint <= 0x7F && count > 1)
          || (codepoint <= 0x07FF && count > 2)
          || (codepoint <= 0xFFFF && count > 3);
    }

    @Override
    public byte[] getBytes(char[] buffer, int offset, int count, boolean throwOnInvalid) {
      int n = offset + count;
      PrimitiveLists.Byte bytes = PrimitiveLists.createForByte();
      for (int i = offset; i < n; ) {
        int ch = getCodePointAt(buffer, i, throwOnInvalid);
        i += Character.charCount(ch);
        encodeUtf8(bytes, ch, throwOnInvalid);
      }
      return bytes.toArray();
    }

    private static int getCodePointAt(char[] buffer, int pos, boolean throwOnInvalid) {
      char ch = buffer[pos];
      // If it's not a surrogate we can just return the char directly.
      if (!Character.isSurrogate(ch)) {
        return ch;
      }
      char high = ch;
      char low = pos + 1 < buffer.length ? buffer[pos + 1] : 0xFF;
      if (!Character.isSurrogatePair(high, low)) {
        if (throwOnInvalid) {
          throw new IllegalArgumentException("Invalid surrogate pair");
        } else {
          return '?';
        }
      }
      return Character.toCodePoint(high, low);
    }

    @Override
    public byte[] getBytes(String str, boolean throwOnInvalid) {
      // TODO(jat): consider using unescape(encodeURIComponent(bytes)) instead
      int n = str.length();
      PrimitiveLists.Byte bytes = PrimitiveLists.createForByte();
      for (int i = 0; i < n;) {
        int ch = getCodePointAt(str, i, n, throwOnInvalid);
        i += Character.charCount(ch);
        encodeUtf8(bytes, ch, throwOnInvalid);
      }
      return bytes.toArray();
    }

    private static int getCodePointAt(String str, int pos, int length, boolean throwOnInvalid) {
      char ch = str.charAt(pos);
      // If it's not a surrogate we can just return the char directly.
      if (!Character.isSurrogate(ch)) {
        return ch;
      }
      char high = ch;
      char low = pos + 1 < length ? str.charAt(pos + 1) : 0xFF;
      if (!Character.isSurrogatePair(high, low)) {
        if (throwOnInvalid) {
          throw new IllegalArgumentException("Invalid surrogate pair");
        } else {
          return '?';
        }
      }
      return Character.toCodePoint(high, low);
    }

    /**
     * Encode a single character in UTF8.
     *
     * @param bytes byte array to store character in
     * @param codePoint character to encode
     * @throws IllegalArgumentException if codepoint >= 2^26
     */
    private void encodeUtf8(PrimitiveLists.Byte bytes, int codePoint, boolean throwOnInvalid) {
      if (codePoint >= (1 << 26)) {
        if (throwOnInvalid) {
          throw new IllegalArgumentException("Character out of range: " + codePoint);
        } else {
          codePoint = REPLACEMENT_CHAR;
        }
      }
      if (codePoint < (1 << 7)) {
        bytes.push((byte) (codePoint & 127));
      } else if (codePoint < (1 << 11)) {
        // 110xxxxx 10xxxxxx
        bytes.push((byte) (((codePoint >> 6) & 31) | 0xC0));
        bytes.push((byte) ((codePoint & 63) | 0x80));
      } else if (codePoint < (1 << 16)) {
        // 1110xxxx 10xxxxxx 10xxxxxx
        bytes.push((byte) (((codePoint >> 12) & 15) | 0xE0));
        bytes.push((byte) (((codePoint >> 6) & 63) | 0x80));
        bytes.push((byte) ((codePoint & 63) | 0x80));
      } else if (codePoint < (1 << 21)) {
        // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        bytes.push((byte) (((codePoint >> 18) & 7) | 0xF0));
        bytes.push((byte) (((codePoint >> 12) & 63) | 0x80));
        bytes.push((byte) (((codePoint >> 6) & 63) | 0x80));
        bytes.push((byte) ((codePoint & 63) | 0x80));
      } else { // codePoint < (1 << 26)
        // 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
        bytes.push((byte) (((codePoint >> 24) & 3) | 0xF8));
        bytes.push((byte) (((codePoint >> 18) & 63) | 0x80));
        bytes.push((byte) (((codePoint >> 12) & 63) | 0x80));
        bytes.push((byte) (((codePoint >> 6) & 63) | 0x80));
        bytes.push((byte) ((codePoint & 63) | 0x80));
      }
    }
  }

  public EmulatedCharset(String name) {
    super(name, null);
  }

  public final byte[] getBytes(String string) {
    return getBytes(string, /* throwOnInvalid= */ false);
  }

  public abstract byte[] getBytes(String string, boolean throwOnInvalid);

  public final byte[] getBytes(char[] buffer, int offset, int count) {
    return getBytes(buffer, offset, count, /* throwOnInvalid= */ false);
  }

  public abstract byte[] getBytes(char[] buffer, int offset, int count, boolean throwOnInvalid);

  public final char[] decodeString(byte[] bytes, int ofs, int len) {
    return decodeString(bytes, ofs, len, /* throwOnInvalid= */ false);
  }

  public abstract char[] decodeString(byte[] bytes, int ofs, int len, boolean throwOnInvalid);
}
