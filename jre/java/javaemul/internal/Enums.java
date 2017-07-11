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

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(namespace = "vmbootstrap")
class Enums {

  @JsType(isNative = true, name = "Map", namespace = JsPackage.GLOBAL)
  private static class NativeMap<K, V> {
    // TODO(b/38182645): replace Enum with V
    public native Enum get(K key);

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

  // TODO(b/38182645): replace NativeMap<String, ?> with NativeMap<String, T>
  public static <T> T getValueFromNameAndMap(String name, NativeMap<String, ?> map) {
    if (name == null) {
      throw new IllegalArgumentException();
    }
    T enumValue = (T) map.get(name);
    if (enumValue == null) {
      throw new IllegalArgumentException();
    }
    return enumValue;
  }
}
