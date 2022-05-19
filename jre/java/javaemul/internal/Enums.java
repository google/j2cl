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

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkCriticalNotNull;

import java.io.Serializable;
import javaemul.internal.annotations.UncheckedCast;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "vmbootstrap")
class Enums {

  @JsType(isNative = true, name = "Map", namespace = JsPackage.GLOBAL)
  private static class NativeMap<K, V> {
    public native V get(K key);

    public native void set(K key, V value);
  }

  /**
   * @param values An array containing all instances of a particular enum type.
   * @return A map from enum name to enum instance.
   */
  public static <T extends Enum<T>> NativeMap<String, T> createMapFromValues(T[] values) {
    NativeMap<String, T> map = new NativeMap<>();
    for (int i = 0; i < values.length; i++) {
      String name = values[i].name();
      map.set(name, values[i]);
    }
    return map;
  }

  @UncheckedCast
  public static <V> V getValueFromNameAndMap(String name, NativeMap<String, V> map) {
    checkCriticalNotNull(name);
    V enumValue = map.get(name);
    if (enumValue == null) {
      throw new IllegalArgumentException();
    }
    return enumValue;
  }

  /** Boxes a JsEnum value that does not support comparable. */
  public static <T> BoxedLightEnum<T> box(T value, Constructor ctor) {
    if (value == null) {
      return null;
    }
    checkArgument(!(value instanceof BoxedLightEnum));
    return cache(ctor, "$$enumValues/" + value, () -> new BoxedLightEnum<T>(value, ctor));
  }

  private static class BoxedLightEnum<T> implements Serializable {
    @JsConstructor
    private BoxedLightEnum(T value, Constructor ctor) {
      this.value = value;
      // Set the constructor property so that code that relies on it (e.g. class metadata) gets
      // the correct underlying constructor.
      this.constructor = ctor;
    }

    final T value;
    @JsProperty final Constructor constructor;

    public String toString() {
      return value.toString();
    }
  }

  /** Boxes a JsEnum value that supports {@link Enum#compareTo} and {@link Enum#ordinal}. */
  public static <T> BoxedComparableLightEnum<T> boxComparable(T value, Constructor ctor) {
    if (value == null) {
      return null;
    }
    checkArgument(!(value instanceof BoxedLightEnum));
    return cache(ctor, "$$enumValues/" + value, () -> new BoxedComparableLightEnum<T>(value, ctor));
  }

  private static class BoxedComparableLightEnum<T> extends BoxedLightEnum<T>
      implements Comparable<BoxedComparableLightEnum<T>> {
    @JsConstructor
    private BoxedComparableLightEnum(T value, Constructor ctor) {
      super(value, ctor);
    }

    @Override
    public int compareTo(BoxedComparableLightEnum<T> o) {
      if (constructor != o.constructor) {
        throw new ClassCastException();
      }
      // Comparable enums are always @enum{number} in closure, and because the values at runtime
      // are numbers it is safe to compare them as Doubles.
      return ((Double) value).compareTo((Double) o.value);
    }
  }

  public static Object unbox(Object object) {
    if (object == null) {
      return null;
    }
    BoxedLightEnum<?> boxedEnum = (BoxedLightEnum<?>) object;
    return boxedEnum.value;
  }

  public static boolean isInstanceOf(Object instance, Constructor ctor) {
    return instance instanceof BoxedLightEnum && ((BoxedLightEnum) instance).constructor == ctor;
  }

  @JsFunction
  private interface Supplier<T> {
    T get();
  }

  @JsMethod(namespace = "goog.reflect")
  private static native <T> T cache(Constructor constructor, String key, Supplier<T> supplier);
}
