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
package specialoverrides

abstract class MyList : MutableList<String> {
  override val size: Int
    get() = 0

  // overrides java.util.Collection.contains(Object)
  override fun contains(element: String): Boolean {
    return false
  }

  // overrides java.util.Collection.containsAll(Collection<?>)
  override fun containsAll(elements: Collection<String>): Boolean {
    return false
  }

  // overrides java.util.List.indexOf(Object)
  override fun indexOf(element: String): Int {
    return 0
  }

  // overrides java.util.List.lastIndexOf(Object)
  override fun lastIndexOf(element: String): Int {
    return 0
  }

  // overrides java.util.Collection.remove(Object)
  override fun remove(element: String): Boolean {
    return false
  }
}

abstract class MyIntList : MutableList<Int> {
  // overrides java.util.Collection.contains(Object)
  override fun contains(element: Int): Boolean {
    return false
  }

  // overrides java.util.Collection.containsAll(Collection<?>)
  override fun containsAll(elements: Collection<Int>): Boolean {
    return false
  }
}

abstract class MyTExtendsStringList<T : String> : MutableList<T> {
  // overrides java.util.Collection.contains(Object)
  override fun contains(element: T): Boolean {
    return false
  }

  // overrides java.util.Collection.containsAll(Collection<?>)
  override fun containsAll(elements: Collection<T>): Boolean {
    return false
  }
}

abstract class MyMap : MutableMap<String, Int?> {
  override val size: Int
    get() = 1

  override fun containsKey(key: String): Boolean {
    return false
  }

  override fun containsValue(value: Int?): Boolean {
    return false
  }

  override fun get(key: String): Int? {
    return null
  }

  override fun getOrDefault(key: String, default: Int?): Int? {
    return null
  }

  override fun remove(key: String): Int? {
    return null
  }
}

abstract class MyIntIntMap : MutableMap<Int, Int> {
  override fun containsKey(key: Int): Boolean {
    return false
  }

  override fun getOrDefault(key: Int, default: Int): Int {
    return null!!
  }
}

abstract class MyTExtendsStringTExtendsStringMap<T : String> : MutableMap<T, T> {
  override fun containsKey(key: T): Boolean {
    return false
  }

  override fun getOrDefault(key: T, default: T): T {
    return null!!
  }
}

abstract class MyGenericList<E> : MutableList<E> {
  override val size: Int
    get() = 0

  // overrides java.util.Collection.contains(Object)
  override fun contains(element: E): Boolean {
    return false
  }

  // overrides java.util.Collection.containsAll(Collection<?>)
  override fun containsAll(elements: Collection<E>): Boolean {
    return false
  }

  // overrides java.util.List.indexOf(Object)
  override fun indexOf(element: E): Int {
    return 0
  }

  // overrides java.util.List.lastIndexOf(Object)
  override fun lastIndexOf(element: E): Int {
    return 0
  }

  // overrides java.util.Collection.remove(Object)
  override fun remove(element: E): Boolean {
    return false
  }
}

abstract class SpecializesMyGeneric : MyGenericList<String>() {
  override val size: Int
    get() = 0

  // overrides java.util.Collection.contains(Object)
  override fun contains(element: String): Boolean {
    return false
  }

  // overrides java.util.Collection.containsAll(Collection<?>)
  override fun containsAll(elements: Collection<String>): Boolean {
    return false
  }

  // overrides java.util.List.indexOf(Object)
  override fun indexOf(element: String): Int {
    return 0
  }

  // overrides java.util.List.lastIndexOf(Object)
  override fun lastIndexOf(element: String): Int {
    return 0
  }

  // overrides java.util.Collection.remove(Object)
  override fun remove(element: String): Boolean {
    return false
  }
}
