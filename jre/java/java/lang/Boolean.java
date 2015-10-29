/*
 * Copyright 2015 Google Inc.
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

import jsinterop.annotations.JsMethod;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Boolean.html">the
 * official Java API doc</a> for details.
 */
public class Boolean implements Comparable<Boolean> {
  
  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public int hashCode() {
    return this ? 1231 : 1237;
  }

  @Override
  public String toString() {
    return unsafeCast(checkNotNull(this)) ? "true" : "false";
  }

  public static Boolean valueOf(boolean b) {
    return null;
  }

  public Boolean(boolean value) {
    // Super-source replaced.
  }

  public boolean booleanValue() {
    return unsafeCast(checkNotNull(this));
  }

  public static int compare(boolean a, boolean b) {
    return (a == b) ? 0 : (a ? 1 : -1);
  }

  @Override
  public int compareTo(Boolean b) {
    return compare(booleanValue(), b.booleanValue());
  }

  private static native boolean unsafeCast(Object instance) /*-{
    return instance;
  }-*/;

  private static Boolean checkNotNull(Boolean b) {
    if (b == null) {
      throw new NullPointerException();
    }
    return b;
  }

  @JsMethod(name = "$create__boolean")
  public static Boolean create(boolean b) {
    return nativeCreate(b);
  }

  private static native boolean nativeCreate(boolean b) /*-{Boolean.$clinit(); return b}-*/;

  @JsMethod(name = "$isInstance")
  public static boolean isInstance(Object instance) {
    return nativeIsInstance(instance);
  }

  private static native boolean nativeIsInstance(Object instance) /*-{
    return typeof instance == 'boolean';
  }-*/;
}
