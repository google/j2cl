/*
 * Copyright 2017 Google Inc.
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

import static javaemul.internal.InternalPreconditions.checkCriticalNotNull;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javaemul.internal.annotations.HasNoSideEffects;

/** Provides utilities to perform operations on enums. */
public final class Enums {

  /**
   * @param values An array containing all instances of a particular enum type.
   * @return A map from enum name to enum instance.
   */
  public static <E extends Enum<E>> Map<String, E> createMapFromValues(E[] values) {
    Map<String, E> valuesByName = new HashMap<>((int) (values.length / 0.75F + 1.0F));
    for (E enumConstant : values) {
      valuesByName.put(enumConstant.name(), enumConstant);
    }
    return valuesByName;
  }

  public static <V> V getValueFromNameAndMap(String name, Map<String, V> map) {
    checkCriticalNotNull(name);
    V enumValue = map.get(name);
    if (enumValue == null) {
      throw new IllegalArgumentException();
    }
    return enumValue;
  }

  /** Base class for boxed representation of a JsEnum. */
  private abstract static class BoxedLightEnum implements Serializable {
    // TODO(b/295235576): Model JS constructor refs or find an alternative, change
    // to allow features like `getClass()`. Use string for now.
    final Class<?> clazz;

    BoxedLightEnum(Class<?> clazz) {
      this.clazz = clazz;
    }
  }

  /** Boxes a JsEnum value that does not support comparable. */
  private static BoxedIntegerEnum boxInteger(int value, Class<?> clazz) {
    if (isNull(value)) {
      return null;
    }
    return getCachedInteger(clazz, value);
  }

  /** Boxed representation of an int JsEnum. */
  private static class BoxedIntegerEnum extends BoxedLightEnum {
    final int value;

    public BoxedIntegerEnum(int value, Class<?> clazz) {
      super(clazz);
      this.value = value;
    }

    public String toString() {
      return Integer.toString(value);
    }
  }

  /** Boxes a JsEnum value that supports {@link Enum#compareTo} and {@link Enum#ordinal}. */
  public static BoxedComparableIntegerEnum boxComparableInteger(int value, Class<?> clazz) {
    if (isNull(value)) {
      return null;
    }
    return getCachedComparableInteger(clazz, value);
  }

  /** Boxed representation of a {@code Comparable<>} int JsEnum. */
  private static class BoxedComparableIntegerEnum extends BoxedIntegerEnum
      implements Comparable<BoxedComparableIntegerEnum> {
    public BoxedComparableIntegerEnum(int value, Class<?> clazz) {
      super(value, clazz);
    }

    @Override
    public int compareTo(BoxedComparableIntegerEnum o) {
      checkType(clazz.equals(o.clazz));
      return Integer.compare(value, o.value);
    }
  }

  public static int unboxInteger(Object boxedEnum, Class<?> clazz) {
    if (boxedEnum == null) {
      return Integer.MIN_VALUE;
    }
    checkType(isInstanceOf(boxedEnum, clazz));
    return ((BoxedIntegerEnum) boxedEnum).value;
  }

  /** Boxes a JsEnum value that does not support comparable. */
  public static BoxedStringEnum boxString(String value, Class<?> clazz) {
    if (value == null) {
      return null;
    }
    return getCachedString(clazz, value);
  }

  /** Boxed representation of a String JsEnum. */
  public static class BoxedStringEnum extends BoxedLightEnum {
    final String value;

    public BoxedStringEnum(String value, Class<?> clazz) {
      super(clazz);
      this.value = value;
    }

    public String toString() {
      return value;
    }
  }

  public static String unboxString(Object boxedEnum, Class<?> clazz) {
    if (boxedEnum == null) {
      return null;
    }
    checkType(isInstanceOf(boxedEnum, clazz));
    return ((BoxedStringEnum) boxedEnum).value;
  }

  public static boolean isInstanceOf(Object instance, Class<?> clazz) {
    return instance instanceof BoxedLightEnum && ((BoxedLightEnum) instance).clazz.equals(clazz);
  }

  public static boolean equalsInteger(int instance, int other) {
    checkNotNull(!isNull(instance));
    return instance == other;
  }

  public static boolean equalsString(String instance, String other) {
    checkNotNull(instance);
    return instance == other;
  }

  public static int compareToInteger(int instance, int other) {
    checkNotNull(!isNull(instance));
    return Integer.compare(instance, other);
  }

  /** Returns {@code true} if the specified enum value is equivalent to null. */
  public static boolean isNull(int enumValue) {
    return enumValue == Integer.MIN_VALUE;
  }

  @HasNoSideEffects
  private static BoxedIntegerEnum getCachedInteger(Class<?> enumClass, int value) {
    return (BoxedIntegerEnum)
        enumClass
            .getJsEnumsCache()
            .computeIfAbsent(value, key -> new BoxedIntegerEnum(value, enumClass));
  }

  @HasNoSideEffects
  private static BoxedComparableIntegerEnum getCachedComparableInteger(
      Class<?> enumClass, int value) {
    return (BoxedComparableIntegerEnum)
        enumClass
            .getJsEnumsCache()
            .computeIfAbsent(value, key -> new BoxedComparableIntegerEnum(value, enumClass));
  }

  @HasNoSideEffects
  private static BoxedStringEnum getCachedString(Class<?> enumClass, String value) {
    return (BoxedStringEnum)
        enumClass
            .getJsEnumsCache()
            .computeIfAbsent(value, key -> new BoxedStringEnum(value, enumClass));
  }
}
