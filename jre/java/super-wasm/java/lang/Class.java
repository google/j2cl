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
import javaemul.internal.annotations.HasNoSideEffects;

/**
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Class.html">the official Java API
 * doc</a> for details.
 */
public final class Class<T> implements Type, Serializable {
  // TODO(b/183548819): Unify this with the closure version so that it does not need supersourcing.

  private final String name;
  private final String primitiveShortName;
  private final boolean isEnum;
  private final boolean isInterface;
  private final boolean isPrimitive;
  /** Dimension count for the underlying array type; or {@code 0} if this is not for an array. */
  private final int dimensionCount;

  /** The class literal for the super class. */
  private final Class<? super T> superClass;
  /** The leaf type for an array. {@code null} if not an array. */
  private final Class<?> leafType;
  /** * Class objects for arrays of this type, created lazily. */
  private Class<?>[] arrayTypes;

  private Class(
      boolean isEnum,
      boolean isInterface,
      boolean isPrimitive,
      int dimensionCount,
      String name,
      String primitiveShortName,
      Class<? super T> superClass,
      Class<?> leafType) {
    this.isEnum = isEnum;
    this.isInterface = isInterface;
    this.isPrimitive = isPrimitive;
    this.dimensionCount = dimensionCount;
    this.name = name;
    this.primitiveShortName = primitiveShortName;
    this.superClass = superClass;
    this.leafType = leafType;
  }

  public String getName() {
    if (isArray()) {
      String className = isPrimitive ? primitiveShortName : "L" + name + ";";
      return repeatString("[", dimensionCount) + className;
    }
    return name;
  }

  // J2CL doesn't follow JLS strictly here and provides an approximation that is good enough for
  // debugging and testing uses.
  public String getCanonicalName() {
    return name + repeatString("[]", dimensionCount);
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
    if (dimensionCount <= 1) {
      return leafType;
    }
    return leafType.getArrayType(dimensionCount - 1);
  }

  @HasNoSideEffects
  Class<?> getArrayType(int dimensions) {
    if (arrayTypes == null || arrayTypes.length < dimensions) {
      Class<?>[] newArray = new Class<?>[dimensions];
      if (arrayTypes != null) {
        System.arraycopy(arrayTypes, 0, newArray, 0, arrayTypes.length - 1);
      }
      arrayTypes = newArray;
    }

    Class<?> result = arrayTypes[dimensions - 1];
    if (result == null) {
      result =
          new Class(
              isEnum,
              isInterface,
              isPrimitive,
              dimensions,
              name,
              primitiveShortName,
              Object.class,
              this);
      arrayTypes[dimensions - 1] = result;
    }

    return result;
  }

  public boolean isArray() {
    return dimensionCount != 0;
  }

  public boolean isEnum() {
    return !isArray() && isEnum;
  }

  public boolean isInterface() {
    return !isArray() && isInterface;
  }

  public boolean isPrimitive() {
    return !isArray() && isPrimitive;
  }

  public T[] getEnumConstants() {
    // TODO(b/30745420): implement
    throw new UnsupportedOperationException();
  }

  public Class<? super T> getSuperclass() {
    return superClass;
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

  static Class<?> createForPrimitive(String name, String primitiveName) {
    return new Class(false, false, true, 0, name, primitiveName, null, null);
  }

  static Class<?> createForClass(String name, Class<?> superClassLiteral) {
    return new Class(false, false, false, 0, name, null, superClassLiteral, null);
  }

  static Class<?> createForEnum(String name) {
    return new Class(true, false, false, 0, name, null, Enum.class, null);
  }

  static Class<?> createForInterface(String name) {
    return new Class(false, true, false, 0, name, null, null, null);
  }
}
