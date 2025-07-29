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

package readonlycollections

class ListImpl<T> : List<T> {
  override val size: Int
    get() = throw RuntimeException("not implemented")

  override fun contains(element: T): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun containsAll(elements: Collection<T>): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun get(index: Int): T {
    throw RuntimeException("not implemented")
  }

  override fun indexOf(element: T): Int {
    throw RuntimeException("not implemented")
  }

  override fun isEmpty(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun iterator(): Iterator<T> {
    throw RuntimeException("not implemented")
  }

  override fun lastIndexOf(element: T): Int {
    throw RuntimeException("not implemented")
  }

  override fun listIterator(): ListIterator<T> {
    throw RuntimeException("not implemented")
  }

  override fun listIterator(index: Int): ListIterator<T> {
    throw RuntimeException("not implemented")
  }

  override fun subList(fromIndex: Int, toIndex: Int): List<T> {
    throw RuntimeException("not implemented")
  }
}

class MapImpl<K, V> : Map<K, V> {
  override val entries: Set<Map.Entry<K, V>>
    get() = throw RuntimeException("not implemented")
  override val keys: Set<K>
    get() = throw RuntimeException("not implemented")
  override val size: Int
    get() = throw RuntimeException("not implemented")
  override val values: Collection<V>
    get() = throw RuntimeException("not implemented")

  override fun containsKey(key: K): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun containsValue(value: V): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun get(key: K): V? {
    throw RuntimeException("not implemented")
  }

  override fun isEmpty(): Boolean {
    throw RuntimeException("not implemented")
  }
}

class SetImpl<T> : Set<T> {
  override val size: Int
    get() = throw RuntimeException("not implemented")

  override fun contains(element: T): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun containsAll(elements: Collection<T>): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun isEmpty(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun iterator(): Iterator<T> {
    throw RuntimeException("not implemented")
  }
}

class IteratorImpl<T> : Iterator<T> {
  override fun hasNext(): Boolean {
    throw RuntimeException("not implemented")
  }

  override fun next(): T {
    throw RuntimeException("not implemented")
  }
}
