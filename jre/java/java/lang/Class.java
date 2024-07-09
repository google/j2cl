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

import java.io.Serializable;
import java.lang.reflect.Type;
import javaemul.internal.Constructor;
import javaemul.internal.JsUtils;
import javaemul.internal.annotations.HasNoSideEffects;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;

/**
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Class.html">the official Java API
 * doc</a> for details.
 */
public final class Class<T> implements Type, Serializable {

  /**
   * Returns the Class instance that corresponds to provided constructor and optional dimension
   * count in the case of an array. This method is used by transpiler to implement the class
   * literals.
   */
  @JsMethod
  @HasNoSideEffects
  private static Class<?> $get(Object obj, @JsOptional Double opt_dimensionCount) {
    // TODO(b/79389970): Change the argument to Constructor.
    Constructor ctor = JsUtils.uncheckedCast(obj);
    int dimensionCount = JsUtils.coerceToInt(opt_dimensionCount);
    return ctor.cache("$$class/" + dimensionCount, () -> new Class(ctor, dimensionCount));
  }

  // Native overloads provided so passing dimensionCount is not required from Java call-sites.

  @JsMethod
  public static native Class<?> $get(Constructor ctor, int dimensionCount);

  @JsMethod
  public static native Class<?> $get(Constructor ctor);

  /**
   * The JavaScript constructor for the underlying Java type that this Class instance belongs to.
   */
  private final Constructor ctor;

  /**
   * Dimension count for the underlying array type; or {@code 0} if this is not for an array.
   */
  private final int dimensionCount;

  @JsConstructor
  private Class(Constructor ctor, int dimensionCount) {
    this.ctor = ctor;
    this.dimensionCount = dimensionCount;
  }

  public String getName() {
    if (isArray()) {
      String className =
          ctor.isPrimitive() ? ctor.getPrimitiveShortName() : "L" + ctor.getClassName() + ";";
      return repeatString("[", dimensionCount) + className;
    }
    return ctor.getClassName();
  }

  // J2CL doesn't follow JLS strictly here and provides an approximation that is good enough for
  // debugging and testing uses.
  public String getCanonicalName() {
    return ctor.getClassName() + repeatString("[]", dimensionCount);
  }

  // J2CL doesn't follow JLS strictly here and provides an approximation that is good enough for
  // debugging and testing uses.
  public String getSimpleName() {
    return stripToLastOccurrenceOf(stripToLastOccurrenceOf(getCanonicalName(), "."), "$");
  }

  private static String stripToLastOccurrenceOf(String str, String token) {
    return str.substring(str.lastIndexOf(token) + 1);
  }

  public Class<?> getComponentType() {
    return isArray() ? Class.$get(ctor, dimensionCount - 1) : null;
  }

  public boolean isArray() {
    return dimensionCount != 0;
  }

  public boolean isEnum() {
    return !isArray() && ctor.isEnum();
  }

  public boolean isInterface() {
    return !isArray() && ctor.isInterface();
  }

  public boolean isPrimitive() {
    return !isArray() && ctor.isPrimitive();
  }

  public T[] getEnumConstants() {
    // TODO(b/30745420): implement
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  public Class<? super T> getSuperclass() {
    Constructor superCtor = ctor.getSuperConstructor();
    return superCtor == null ? null : (Class<? super T>) Class.$get(superCtor, 0);
  }

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

  public Class<?> $getBoxedClass() {
    return isPrimitive() ? $get(ctor.getBoxedConstructor()) : this;
  }

  public Class<?> $getPrimitiveClass() {
    if (isArray()) {
      return null;
    }
    if (isPrimitive()) {
      return this;
    }
    Constructor primitiveCtor = ctor.getPrimitiveConstructor();
    if (primitiveCtor == null) {
      return null;
    }
    return $get(primitiveCtor);
  }
}
