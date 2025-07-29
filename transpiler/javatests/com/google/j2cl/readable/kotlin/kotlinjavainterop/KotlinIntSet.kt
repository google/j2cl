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

class KotlinIntSet : JavaIntSet() {
  override fun add(element: Int?): Boolean {
    return super.add(element)
  }

  override fun addAll(elements: Collection<Int>): Boolean {
    return super.addAll(elements)
  }

  override fun clear() {
    super.clear()
  }

  override fun iterator(): MutableIterator<Int> {
    return super.iterator()
  }

  override fun remove(o: Int?): Boolean {
    return super.remove(o)
  }

  override fun removeAll(elements: Collection<Int>): Boolean {
    return super.removeAll(elements)
  }

  override fun retainAll(elements: Collection<Int>): Boolean {
    return super.retainAll(elements)
  }

  override fun contains(o: Int?): Boolean {
    return super.contains(o)
  }

  override fun containsAll(elements: Collection<Int>): Boolean {
    return super.containsAll(elements)
  }

  override fun isEmpty(): Boolean {
    return super.isEmpty()
  }

  override fun toArray(): Array<Any> {
    return super.toArray()
  }

  override fun <T : Any?> toArray(a: Array<out T & Any>?): Array<T & Any> {
    return super.toArray(a)
  }

  override val size: Int
    get() = super.size
}
