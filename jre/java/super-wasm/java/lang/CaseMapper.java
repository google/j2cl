/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package java.lang;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/** Performs case operations as described by http://unicode.org/reports/tr21/tr21-5.html. */
class CaseMapper {

  private CaseMapper() {}

  public static int codePointToLowerCase(int value) {
    if (value < 128) {
      if ('A' <= value && value <= 'Z') {
        return value + ('a' - 'A');
      }
      return value;
    }
    return nativeCodePointToLowerCase(value);
  }

  @JsMethod(name = "Character.codePointToLowerCase", namespace = JsPackage.GLOBAL)
  private static native int nativeCodePointToLowerCase(int value);

  public static int codePointToUpperCase(int value) {
    if (value < 128) {
      if ('a' <= value && value <= 'z') {
        return value - ('a' - 'A');
      }
      return value;
    }
    return nativeCodePointToUpperCase(value);
  }

  @JsMethod(name = "Character.codePointToUpperCase", namespace = JsPackage.GLOBAL)
  private static native int nativeCodePointToUpperCase(int value);

  public static char charToLowerCase(char value) {
    if (value < 128) {
      if ('A' <= value && value <= 'Z') {
        return (char) (value + ('a' - 'A'));
      }
      return value;
    }
    return nativeCharToLowerCase(value);
  }

  @JsMethod(name = "Character.charToLowerCase", namespace = JsPackage.GLOBAL)
  private static native char nativeCharToLowerCase(char value);

  public static char charToUpperCase(char value) {
    if (value < 128) {
      if ('a' <= value && value <= 'z') {
        return (char) (value - ('a' - 'A'));
      }
      return value;
    }
    return nativeCharToUpperCase(value);
  }

  @JsMethod(name = "Character.charToUpperCase", namespace = JsPackage.GLOBAL)
  private static native char nativeCharToUpperCase(char value);
}
