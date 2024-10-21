/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaemul.internal;

import java.util.Arrays;
import java.util.StringJoiner;
import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/**
 * A base type that provides 'value' type semantics for equals/hashcode/toString via reflection.
 *
 * <p>Note that even this is base type, it is not necessarily need to used in an 'extends' clause;
 * instead it can be used via {@code ValueType.mixin}) helper as well.
 *
 * <p>Note that for the properties that is never read by the app this may change the behavior
 * compared to handwritten versions. Such that instances would considered equal even if never read
 * properties are set to different values.
 */
public abstract class ValueType {

  @Override
  public boolean equals(Object o) {
    return equals(this, o);
  }

  @Override
  public int hashCode() {
    return hashCode(this);
  }

  @Override
  public String toString() {
    return toString(this);
  }

  // Package private to ensure clinit is run when called from types that use this class as a mixin.
  static boolean equals(ValueType thisObject, Object o) {
    if (o == null || o.getClass() != thisObject.getClass()) {
      return false;
    }

    ValueType thatObject = JsUtils.uncheckedCast(o);

    String[] thisKeys = filteredkeys(thisObject);

    // Number of properties should match. This might not be true for a generic object comparator;
    // (e.g. property set to 'null' or 'undefined' might be considered as non-existent for
    // comparison) however based on our code generation, the properties will be always set for value
    // type hence this should never happen.
    if (thisKeys.length != filteredkeys(thatObject).length) {
      return false;
    }

    for (String p : thisKeys) {
      Object p1 = JsUtils.getProperty(thisObject, p);
      Object p2 = JsUtils.getProperty(thatObject, p);
      if (!isAutoValueEqual(p1, p2)) {
        return false;
      }
    }

    return true;
  }

  private static boolean isAutoValueEqual(Object a, Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }

    if (ArrayStamper.isStamped(a) && ArrayStamper.isStamped(b)) {
      // AutoValue only supports primitive arrays so if the array is stamped we can safely assume
      // this is a primitive array. So here we just check that they have same types and compare the
      // contents.
      return a.getClass() == b.getClass()
          && Arrays.equals(JsUtils.<Object[]>uncheckedCast(a), JsUtils.<Object[]>uncheckedCast(b));
    }

    return a.equals(b);
  }

  // Package private to ensure clinit is run when called from types that use this class as a mixin.
  static int hashCode(ValueType thisObject) {
    int hashCode = 1;
    for (String key : filteredkeys(thisObject)) {
      Object value = JsUtils.getProperty(thisObject, key);
      if (value == null) {
        continue;
      }
      // AutoValue supports primitives arrays, that needs special handling for hashCode.
      int h =
          ArrayStamper.isStamped(value)
              ? Arrays.hashCode(JsUtils.<Object[]>uncheckedCast(value))
              : value.hashCode();
      hashCode = (1000003 * hashCode) ^ h;
    }
    return hashCode;
  }

  // Package private to ensure clinit is run when called from types that use this class as a mixin.
  static String toString(ValueType thisObject) {
    String className = thisObject.getClass().getSimpleName();
    // Remove AutoValue_ prefix used by the APT if it exists.
    className = className.substring(className.lastIndexOf("AutoValue_") + 1);
    StringJoiner joiner = new StringJoiner(", ", className + "{", "}");
    for (String p : filteredkeys(thisObject)) {
      Object value = JsUtils.getProperty(thisObject, p);
      if (ArrayHelper.isArray(value)) {
        // Relying on JS toString behavior avoids pulling in extra deps through Arrays.toString.
        value = "[" + value + "]";
      }
      // Best effort to strip down of mangled names.
      p = cleanMangledPropertyName(p);
      joiner.add(p + "=" + value);
    }
    return joiner.toString();
  }

  private static boolean COMPILED = "true".equals(System.getProperty("COMPILED", "false"));

  private static String cleanMangledPropertyName(String name) {
    if (COMPILED) {
      // There is no point in the trying to clean up for compiled mode add to the code size since
      // names are usually obfuscated.
      return name;
    }
    // In the absense of minification, propery name is followed by __
    int index = name.startsWith("f_") ? name.indexOf("__", 2) : -1;
    if (index != -1) {
      return name.substring(2, index);
    }
    // In the case of minification, the propery name will follow with _$<counter>
    index = name.lastIndexOf("_$");
    return index == -1 ? name : name.substring(0, index);
  }

  @JsMethod
  private static native String[] filteredkeys(ValueType type);

  @JsMethod
  static native void mixin(Constructor target, Constructor source, int mask, String... excludes);

  @JsMethod(namespace = "goog.reflect")
  static native String objectProperty(String name, Object type);

  @JsMethod(name = "$J2CL_PRESERVE$", namespace = JsPackage.GLOBAL)
  static native void preserve(@DoNotAutobox Object... type);
}
