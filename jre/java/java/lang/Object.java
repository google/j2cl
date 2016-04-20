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

import javaemul.internal.HashCodes;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Object.html">the
 * official Java API doc</a> for details.
 */
public class Object {

  public boolean equals(Object that) {
    return this == that;
  }

  public int hashCode() {
    return HashCodes.getObjectIdentityHashCode(this);
  }

  @JsMethod(name = "$javaToString")
  public String toString() {
    // TODO: fix this implementation. The hash code should be returned in hex
    // but can't currently depend on Integer to get access to that static
    // function because Closure doesn't yet support module circular references.
    return this.getClass().getName() + "@" + this.hashCode();
  }

  // Defined as native so that we can modify the JsDoc to change return type to non-null.
  // TODO(goktug): Use @NotNull when available.
  @JsMethod(name = "toString")
  private native String toStringBridge();

  public final Class<?> getClass() {
    return Class.$get(getConstructor());
  }

  @JsProperty
  private native Object getConstructor();

  @JsMethod
  private static boolean $isInstance(Object instance) {
    return true;
  }

  @JsMethod
  private static boolean $isAssignableFrom(Object instance) {
    return true;
  }
}
