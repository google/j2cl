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

/** Provides devirtualized method implementations for Numbers. */
@JsType(namespace = "vmbootstrap")
class Numbers {

  // Note that the casts needs to follow instanceof immediately for RemoveUnneededCasts to kick in.
  // If this pattern changes, we should use an unchecked cast instead.

  static byte byteValue(Number obj) {
    if (obj instanceof Double) {
      Double d = (Double) obj;
      return d.byteValue();
    }
    return obj.byteValue();
  }

  static double doubleValue(Number obj) {
    if (obj instanceof Double) {
      Double d = (Double) obj;
      return d.doubleValue();
    }
    return obj.doubleValue();
  }

  static float floatValue(Number obj) {
    if (obj instanceof Double) {
      Double d = (Double) obj;
      return d.floatValue();
    }
    return obj.floatValue();
  }

  static int intValue(Number obj) {
    if (obj instanceof Double) {
      Double d = (Double) obj;
      return d.intValue();
    }
    return obj.intValue();
  }

  static long longValue(Number obj) {
    if (obj instanceof Double) {
      Double d = (Double) obj;
      return d.longValue();
    }
    return obj.longValue();
  }

  static short shortValue(Number obj) {
    if (obj instanceof Double) {
      Double d = (Double) obj;
      return d.shortValue();
    }
    return obj.shortValue();
  }
}
