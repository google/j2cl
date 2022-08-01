// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package javaemul.internal;

import jsinterop.annotations.JsType;

/** Provides devirtualized method implementations for CharSequence. */
@JsType(namespace = "vmbootstrap")
class CharSequences {

  // Note that the casts needs to follow instanceof immediately for RemoveUnneededCasts to kick in.
  // If this pattern changes, we should use an unchecked cast instead.

  static int length(CharSequence obj) {
    if (obj instanceof String) {
      String str = (String) obj;
      return str.length();
    }
    return obj.length();
  }

  static char charAt(CharSequence obj, int index) {
    if (obj instanceof String) {
      String str = (String) obj;
      return str.charAt(index);
    }
    return obj.charAt(index);
  }

  static String toString(CharSequence obj) {
    return obj.toString();
  }

  static CharSequence subSequence(CharSequence obj, int start, int end) {
    if (obj instanceof String) {
      String str = (String) obj;
      return str.subSequence(start, end);
    }
    return obj.subSequence(start, end);
  }
}
