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
import java.util.Objects;
import java.util.StringJoiner;
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

    String[] thisKeys = keys(thisObject);

    // Number of properties should match. This might not be true for a generic object comparator;
    // (e.g. property set to 'null' or 'undefined' might be considered as non-existent for
    // comparison) however based on our code generation, the properties will be always set for value
    // type hence this should never happen.
    if (thisKeys.length != keys(thatObject).length) {
      return false;
    }

    for (String p : thisKeys) {
      if (!Objects.deepEquals(
          JsUtils.getProperty(thisObject, p), JsUtils.getProperty(thatObject, p))) {
        return false;
      }
    }

    return true;
  }

  // Package private to ensure clinit is run when called from types that use this class as a mixin.
  static int hashCode(ValueType thisObject) {
    int hashCode = 1;
    for (Object e : values(thisObject)) {
      if (e == null) {
        continue;
      }
      hashCode = 31 * hashCode + e.hashCode();
      hashCode |= 0; // make sure we don't overflow
    }
    return hashCode;
  }

  // Package private to ensure clinit is run when called from types that use this class as a mixin.
  static String toString(ValueType thisObject) {
    StringJoiner joiner = new StringJoiner(",", thisObject.getClass().getSimpleName() + "{", "}");
    for (String p : keys(thisObject)) {
      Object value = JsUtils.getProperty(thisObject, p);
      if (ArrayHelper.isArray(value)) {
        value = Arrays.toString(JsUtils.<Object[]>uncheckedCast(value));
      }
      joiner.add(p + "=" + value);
    }
    return joiner.toString();
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Object.keys")
  private static native String[] keys(Object a);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Object.values")
  private static native Object[] values(Object a);
}
