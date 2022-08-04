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

/** Provides devirtualized method implementations for Comparable. */
@JsType(namespace = "vmbootstrap")
class Comparables {
  static int compareTo(Comparable a, Object b) {
    // Note that we do single manual typeOf call here instead of multiple instanceofs that would
    // result in multiple typeOf calls.
    switch (JsUtils.typeOf(a)) {
      case "number":
        return JsUtils.<Double>uncheckedCast(a).compareTo((Double) b);
      case "boolean":
        return JsUtils.<Boolean>uncheckedCast(a).compareTo((Boolean) b);
      case "string":
        return JsUtils.<String>uncheckedCast(a).compareTo((String) b);
    }
    return a.compareTo(b);
  }
}
