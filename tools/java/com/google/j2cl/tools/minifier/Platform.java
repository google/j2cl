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
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

final class Platform {

  public static class CharBuffer {
    private final StringBuilder delegate = new StringBuilder();

    void setLength(int length) {
      delegate.setLength(length);
    }

    int length() {
      return delegate.length();
    }

    void append(char c) {
      delegate.append(c);
    }

    char charAt(int index) {
      return delegate.charAt(index);
    }

    void replaceTail(int start, String s) {
      delegate.replace(start, delegate.length(), s);
    }

    int indexOf(String s, int start) {
      return delegate.indexOf(s, start);
    }

    String substring(int start) {
      return delegate.substring(start);
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  record Pattern(java.util.regex.Pattern delegate) {
    public static Pattern compile(String pattern) {
      return new Pattern(java.util.regex.Pattern.compile(pattern));
    }

    @Nullable
    public String match(CharBuffer input, int startIndex) {
      var matcher = delegate.matcher(input.delegate).region(startIndex, input.length());
      if (!matcher.matches()) {
        return null;
      }
      return matcher.groupCount() > 0 ? matcher.group(1) : "";
    }
  }

  public static CodeRemovalInfo readCodeRemovalInfoFile(String codeRemovalInfoFilePath)
      throws IOException {
    try (InputStream inputStream =
        new BufferedInputStream(new FileInputStream(codeRemovalInfoFilePath))) {
      return CodeRemovalInfo.parseFrom(inputStream);
    }
  }

  private Platform() {}
}
