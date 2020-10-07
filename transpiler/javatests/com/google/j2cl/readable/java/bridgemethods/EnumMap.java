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
package bridgemethods;

interface Map<K, V> {
  V put(K key, V value);
}

class AbstractMap<K, V> implements Map<K, V> {
  @Override
  public V put(K key, V value) {
    return value;
  }
}

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> {
  @Override
  public V put(K key, V value) {
    return value;
  }
}
