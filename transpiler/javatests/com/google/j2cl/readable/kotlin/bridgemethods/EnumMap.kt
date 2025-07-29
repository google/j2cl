/*
 * Copyright 2022 Google Inc.
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
package bridgemethods

internal interface Map<K, V> {
  fun put(key: K, value: V): V
}

open class AbstractMap<K, V> : Map<K, V> {
  override fun put(key: K, value: V): V {
    return value
  }
}

class EnumMap<K : Enum<K>, V> : AbstractMap<K, V>() {
  override fun put(key: K, value: V): V {
    return value
  }
}
