/*
 * Copyright 2007 Google Inc.
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
package java.lang;


import java.io.Serializable;
import javaemul.internal.JsUtils;
import javaemul.internal.Platform;
import jsinterop.annotations.JsMethod;

/**
 * Wraps native <code>boolean</code> as an object.
 */
public final class Boolean implements Comparable<Boolean>, Serializable {

  public static final Boolean FALSE = false;
  public static final Boolean TRUE = true;

  public static final Class<Boolean> TYPE = boolean.class;

  public static int compare(boolean x, boolean y) {
    return (x == y) ? 0 : (x ? 1 : -1);
  }

  public static int hashCode(boolean value) {
    // The Java API doc defines these magic numbers.
    return value ? 1231 : 1237;
  }

  public static boolean logicalAnd(boolean a, boolean b) {
    return a && b;
  }

  public static boolean logicalOr(boolean a, boolean b) {
    return a || b;
  }

  public static boolean logicalXor(boolean a, boolean b) {
    return a ^ b;
  }

  public static boolean parseBoolean(String s) {
    return s != null && "true".equals(s.toLowerCase());
  }

  public static String toString(boolean x) {
    return String.valueOf(x);
  }

  public static Boolean valueOf(boolean b) {
    // Note that we are not doing a simple new Boolean(b). For historic reasons it is possible
    // that the provided 'b' is not a real boolean value. We are also avoiding '!!b' since it is
    // safe to optimize it back to 'b' in a Java context.
    return b ? new Boolean(true) : new Boolean(false);
  }

  public static Boolean valueOf(String s) {
    return valueOf(parseBoolean(s));
  }

  // Note that 'value' field is special and used for unboxing by the J2CL transpiler.
  private final boolean value;

  public Boolean(boolean value) {
    this.value = value;
  }

  public Boolean(String s) {
    this.value = parseBoolean(s);
  }

  public boolean booleanValue() {
    return this.value;
  }

  @Override
  public int compareTo(Boolean b) {
    return compare(booleanValue(), b.booleanValue());
  }

  @Override
  public boolean equals(Object o) {
    return Platform.isEqual(this, o);
  }

  @Override
  public int hashCode() {
    return hashCode(booleanValue());
  }

  @Override
  public String toString() {
    return toString(booleanValue());
  }

  // CHECKSTYLE_OFF: Utility Methods for unboxed Boolean.
  @JsMethod
  protected static boolean $isInstance(Object instance) {
    return "boolean".equals(JsUtils.typeOf(instance));
  }
  //CHECKSTYLE_ON: End utility methods
}
