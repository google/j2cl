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

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Class.html">the official Java API
 * doc</a> for details.
 */
public final class Class<T> implements Type, Serializable {

  /**
   * Returns the Class instance that corresponds to provided constructor and optional dimension
   * count in the case of an array.
   * This method is used by transpiler to implement the class literals.
   */
  @JsMethod
  static native Class<?> $get(Object ctor, int dimensionCount);

  /**
   * The JavaScript constructor for the underlying Java type that this Class instance belongs to.
   */
  private final Object ctor;

  /**
   * Dimension count for the underlying array type; or {@code 0} if this is not for an array.
   */
  private final int dimensionCount;

  @JsConstructor
  private Class(Object ctor, int dimensionCount) {
    this.ctor = ctor;
    this.dimensionCount = dimensionCount;
  }

  public String getName() {
    String className = Util.$extractClassName(ctor);
    if (isArray() && Util.$extractClassType(ctor) != Util.TYPE_PRIMITIVE) {
      className = "L" + className + ";";
    }
    return repeatString("[", dimensionCount) + className;
  }

  // J2CL doesn't follow JLS strictly here and provides an approximation that is good enough for
  // debugging and testing uses.
  public String getCanonicalName() {
    return Util.$extractClassName(ctor) + repeatString("[]", dimensionCount);
  }

  // J2CL doesn't follow JLS strictly here and provides an approximation that is good enough for
  // debugging and testing uses.
  public String getSimpleName() {
    String canonicalName = getCanonicalName();
    return canonicalName.substring(canonicalName.lastIndexOf(".") + 1);
  }

  public Class<?> getComponentType() {
    return isArray() ? Class.$get(ctor, dimensionCount - 1) : null;
  }

  public boolean isArray() {
    return dimensionCount != 0;
  }

  public boolean isEnum() {
    return isOfType(Util.TYPE_ENUM);
  }

  public boolean isInterface() {
    return isOfType(Util.TYPE_INTERFACE);
  }

  public boolean isPrimitive() {
    return isOfType(Util.TYPE_PRIMITIVE);
  }

  private boolean isOfType(int type) {
    return !isArray() && Util.$extractClassType(ctor) == type;
  }

  // TODO: implement
  public T[] getEnumConstants() {
    return null;
  }

  @SuppressWarnings("unchecked")
  public Class<? super T> getSuperclass() {
    Object superCtor = getSuperCtor(ctor);
    return superCtor == null ? null : (Class<? super T>) Class.$get(superCtor, 0);
  }

  @JsMethod
  private static native Object getSuperCtor(Object ctor);

  public boolean desiredAssertionStatus() {
    return false;
  }

  @Override
  public String toString() {
    return (isInterface() ? "interface " : isPrimitive() ? "" : "class ") + getName();
  }

  private static String repeatString(String str, int count) {
    String rv = "";
    for (int i = 0; i < count; i++) {
      rv += str;
    }
    return rv;
  }

  @JsType(isNative = true, namespace = "nativebootstrap")
  private static class Util {
    public static int TYPE_ENUM;
    public static int TYPE_INTERFACE;
    public static int TYPE_PRIMITIVE;

    public static native String $extractClassName(Object ctor);

    public static native int $extractClassType(Object ctor);
  }
}
