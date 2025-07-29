/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package kotlinjavainterop

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function

class KotlinIntMap : JavaIntMap() {
  override fun clear() {
    super.clear()
  }

  override fun compute(key: Int?, remappingFunction: BiFunction<in Int, in Int?, out Int?>): Int? {
    return super.compute(key, remappingFunction)
  }

  override fun computeIfPresent(
    key: Int?,
    remappingFunction: BiFunction<in Int, in Int, out Int?>,
  ): Int? {
    return super.computeIfPresent(key, remappingFunction)
  }

  override fun computeIfAbsent(key: Int?, mappingFunction: Function<in Int, out Int>): Int {
    return super.computeIfAbsent(key, mappingFunction)
  }

  override fun put(key: Int?, value: Int?): Int? {
    return super.put(key, value)
  }

  override fun putAll(from: Map<out Int, Int>) {
    super.putAll(from)
  }

  override fun remove(key: Int?): Int? {
    return super.remove(key)
  }

  override fun containsKey(key: Int?): Boolean {
    return super.containsKey(key)
  }

  override fun containsValue(value: Int?): Boolean {
    return super.containsValue(value)
  }

  override fun get(key: Int?): Int? {
    return super.get(key)
  }

  override fun isEmpty(): Boolean {
    return super.isEmpty()
  }

  override fun merge(
    key: Int?,
    value: Int,
    remappingFunction: BiFunction<in Int, in Int, out Int?>,
  ): Int? {
    return super.merge(key, value, remappingFunction)
  }

  override fun replace(key: Int?, value: Int?): Int? {
    return super.replace(key, value)
  }

  override fun replace(key: Int?, oldValue: Int?, newValue: Int?): Boolean {
    return super.replace(key, oldValue, newValue)
  }

  override fun putIfAbsent(key: Int?, value: Int?): Int? {
    return super.putIfAbsent(key, value)
  }

  override fun replaceAll(function: BiFunction<in Int, in Int, out Int>) {
    super.replaceAll(function)
  }

  override fun remove(key: Int?, value: Int?): Boolean {
    return super.remove(key, value)
  }

  override val entries: MutableSet<MutableMap.MutableEntry<Int, Int>>
    get() = super.entries

  override val keys: MutableSet<Int>
    get() = super.keys

  override val values: MutableCollection<Int>
    get() = super.values

  override fun forEach(action: BiConsumer<in Int, in Int>) {
    super.forEach(action)
  }

  override fun getOrDefault(key: Int?, defaultValue: Int?): Int {
    return super.getOrDefault(key, defaultValue)
  }

  override val size: Int
    get() = super.size
}
