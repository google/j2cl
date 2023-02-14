/*
 * Copyright 2016 Google Inc.
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

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/** Simple class to work with native js regular expressions. */
// TODO(b/261078322,b/193532287) Remove super source when JsOverlay & arrays are supported.
public class NativeRegExp {
  private final WasmExtern instance;

  public NativeRegExp(String regex) {
    this.instance = create(regex);
  }

  public NativeRegExp(String regex, String mode) {
    this.instance = create(regex, mode);
  }

  public void setLastIndex(int index) {
    setLastIndex(this.instance, index);
  }

  public Match exec(String value) {
    WasmExtern result = exec(this.instance, value);
    return result == null ? null : new Match(result);
  }

  public boolean test(String value) {
    return test(this.instance, value);
  }

  /**
   * This method converts Java-escaped dollar signs "\$" into JavaScript-escaped dollar signs "$$",
   * and removes all other lone backslashes, which serve as escapes in Java but are passed through
   * literally in JavaScript.
   */
  public static String translateReplaceString(String replaceStr) {
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

  public static String escapeForRegExpSearch(char c) {
    // Translate 'from' into unicode escape sequence (\\u and a four-digit hexadecimal number).
    // Escape sequence replacement is used instead of a string literal replacement
    // in order to escape regexp special characters (e.g. '.').
    String hex = Integer.toHexString(c);
    return "\\u" + "0000".substring(hex.length()) + hex;
  }

  /** Escapes the given CharSerquence such that is can be used in a RegExp search. */
  public static String escapeForRegExpSearch(CharSequence str) {
    return str.toString().replaceAll("([/\\\\\\.\\*\\+\\?\\|\\(\\)\\[\\]\\{\\}$^])", "\\\\$1");
  }

  /** Escapes the given CharSerquence such that is can be used in a RegExp repleacement. */
  public static String escapeForRegExpReplacement(CharSequence str) {
    // Escape $ since it is for match backrefs and \ since it is used to escape $.
    return str.toString().toString().replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\$");
  }

  public WasmExtern toJs() {
    return instance;
  }

  /** Contract of the this.instance returned by {@code RegExp.prototype.exec}. */
  public static class Match {
    private final WasmExtern instance;

    private Match(WasmExtern instance) {
      this.instance = instance;
    }

    public int getIndex() {
      return index(instance);
    }

    public int getLength() {
      return getLength(instance);
    }

    public String getAt(int index) {
      return getBufferAt(instance, index);
    }

    @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExpResult.index")
    private static native int index(WasmExtern match);

    @JsMethod(namespace = JsPackage.GLOBAL)
    private static native String getBufferAt(WasmExtern o, int i);

    @JsMethod(namespace = JsPackage.GLOBAL)
    private static native int getLength(WasmExtern o);
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.constructor")
  private static native WasmExtern create(String pattern);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.constructor")
  private static native WasmExtern create(String pattern, String flags);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.setLastIndex")
  private static native void setLastIndex(WasmExtern regExp, int index);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.exec")
  private static native WasmExtern exec(WasmExtern regExp, String text);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "RegExp.test")
  private static native boolean test(WasmExtern regExp, String text);
}
