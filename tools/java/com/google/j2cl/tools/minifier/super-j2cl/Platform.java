/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.tools.minifier;

import com.google.j2cl.tools.rta.CodeRemovalInfo;
import elemental2.core.Uint8Array;
import java.io.IOException;
import javax.annotation.Nullable;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

final class Platform {

  public static class CharBuffer {
    private char[] data = new char[0];
    private int length = 0;

    void setLength(int length) {
      this.length = length;
    }

    int length() {
      return length;
    }

    void append(char c) {
      data[length++] = c;
    }

    char charAt(int index) {
      return data[index];
    }

    void replaceTail(int start, String string) {
      string.getChars(0, string.length(), data, start);
      // Adjust the length to trim so we drop the characters that were not replaced.
      setLength(start + string.length());
    }

    int indexOf(String subString, int start) {
      for (int i = start; i <= length - subString.length(); i++) {
        boolean match = true;
        for (int j = 0; j < subString.length(); j++) {
          if (data[i + j] != subString.charAt(j)) {
            match = false;
            break;
          }
        }
        if (match) {
          return i;
        }
      }
      return -1;
    }

    String substring(int start) {
      return new String(data, start, length - start);
    }

    @Override
    public String toString() {
      return new String(data, 0, length);
    }
  }

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  static class Pattern {

    @JsOverlay
    public static Pattern compile(String pattern) {
      return new Pattern("^" + pattern + "$");
    }

    Pattern(String pattern) {}

    @Nullable
    native MatchResult exec(String input);

    @JsOverlay
    @Nullable
    public final String match(CharBuffer input, int startIndex) {
      MatchResult result = this.exec(input.substring(startIndex));
      if (result == null) {
        return null;
      }
      return result.getLength() > 1 ? result.at(1) : "";
    }

    @JsType(isNative = true, name = "RegExpResult", namespace = JsPackage.GLOBAL)
    private interface MatchResult {
      @JsProperty
      int getLength();

      String at(int groupIndex);
    }
  }

  @Nullable
  public static CodeRemovalInfo readCodeRemovalInfoFile(String codeRemovalInfoFilePath)
      throws IOException {
    return CodeRemovalInfo.parseFrom(readFile(codeRemovalInfoFilePath));
  }

  @JsMethod(name = "fs.readFileSync", namespace = JsPackage.GLOBAL)
  private static native Uint8Array readFile(String path);

  private Platform() {}
}
