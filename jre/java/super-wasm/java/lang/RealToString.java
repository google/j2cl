/*
 * Copyright 2023 Google Inc.
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
package java.lang;

import static java.lang.System.getProperty;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

final class RealToString {

  private static final boolean JS_TOSTRING =
      String.STRINGREF_ENABLED && getProperty("jre.strictFpToString").equals("DISABLED");

  private RealToString() {}

  public static String doubleToString(double d) {
    if (JS_TOSTRING) {
      return fromNumber(d);
    }
    return RyuDouble.doubleToString(null, d);
  }

  public static void appendDouble(AbstractStringBuilder sb, double d) {
    if (JS_TOSTRING) {
      sb.append0(fromNumber(d));
      return;
    }
    var unused = RyuDouble.doubleToString(sb, d);
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Number.prototype.toString.call")
  private static native String fromNumber(double d);

  public static String floatToString(float f) {
    if (JS_TOSTRING) {
      return fromNumber(f);
    }
    return RyuFloat.floatToString(null, f);
  }

  public static void appendFloat(AbstractStringBuilder sb, float f) {
    if (JS_TOSTRING) {
      sb.append0(fromNumber(f));
      return;
    }
    var unused = RyuFloat.floatToString(sb, f);
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Number.prototype.toString.call")
  private static native String fromNumber(float f);
}
