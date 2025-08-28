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
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

final class Platform {

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
    public final String match(StringBuilder input, int startIndex, int endIndex) {
      // This is performance-safe for JS since StringBuilder in J2CL backed by JS String and JS VM
      // only introduces a view on substring.
      String region = input.substring(startIndex, endIndex);
      MatchResult result = this.exec(region);
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
  public static CodeRemovalInfo readCodeRemovalInfoFile(String codeRemovalInfoFilePath) {
    // For now we won't support reading code removal info file.
    return null;
  }

  private Platform() {}
}
