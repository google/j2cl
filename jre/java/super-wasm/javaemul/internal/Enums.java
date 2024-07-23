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

import java.util.HashMap;
import java.util.Map;

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
}
