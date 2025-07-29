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
package primitivespecialization

interface IntGetter {
  fun get(): Int
}

interface IntSetter {
  fun set(i: Int)
}

open class Holder<T>(var content: T) {
  fun get(): T {
    return content
  }

  fun set(t: T) {
    content = t
  }
}

open class IntHolder : Holder<Int>(1), IntGetter, IntSetter {
  fun test(): Int {
    return get()
  }
}

class SubIntHolder : IntHolder()

open class NumberHolder<T : Number>(t: T) : Holder<T>(t)

class SubNumberHolder : NumberHolder<Int>(2)

// This has accidental overrides for Holder.get() by IntGetter.get() and Holder.set(Object)
// by IntSetter.set(int).
class IntHolderExtendsGetter : Holder<Int>(1), IntGetter, IntSetter

open class Overloads {
  open fun set(i: Int) {}

  open fun set(i: Int?) {}
}

class IndependentOverrides : Overloads() {
  override fun set(i: Int) {}

  override fun set(i: Int?) {}
}

interface Setter<T : Int?> {
  fun set(t: T)
}

class JoiningOverrides : Overloads(), Setter<Int> {
  // Since Int is a subclass of Int? and setter specializes set(Int?) to set(Int)
  // this is the only method that is possible to override.
  override fun set(i: Int) {}
}

class NonJoiningOverrides : Overloads(), Setter<Int?> {
  override fun set(i: Int) {}

  override fun set(i: Int?) {}
}

interface IntHolderInterface {
  fun set(i: Int)

  fun get(): Int
}

object NumberByIntMap : Map<Int, Number> {
  override val entries: Set<Map.Entry<Int, Number>>
    get() = throw RuntimeException()

  override val keys: Set<Int>
    get() = throw RuntimeException()

  override val size: Int
    get() = throw RuntimeException()

  override val values: Collection<Number>
    get() = throw RuntimeException()

  override fun containsKey(key: Int): Boolean {
    throw RuntimeException()
  }

  override fun containsValue(value: Number): Boolean {
    throw RuntimeException()
  }

  override fun get(key: Int): Number {
    throw RuntimeException()
  }

  override fun isEmpty(): Boolean {
    throw RuntimeException()
  }
}

class IntList : ArrayList<Int>() {
  override fun removeAt(index: Int): Int = super.removeAt(index)

  override fun retainAll(elements: Collection<Int>): Boolean = super.retainAll(elements)

  override fun removeAll(elements: Collection<Int>): Boolean = super.removeAll(elements)

  override fun remove(element: Int): Boolean = super.remove(element)

  override fun lastIndexOf(element: Int): Int = super.lastIndexOf(element)

  override fun indexOf(element: Int): Int = super.indexOf(element)

  override fun containsAll(elements: Collection<Int>): Boolean = super.containsAll(elements)

  override fun contains(element: Int): Boolean = super.contains(element)
}

private class IntListFromScratch : MutableList<Int> {
  private val contents = ArrayList<Int>()

  override val size: Int
    get() = contents.size

  override fun clear() = contents.clear()

  override fun addAll(elements: Collection<Int>): Boolean = contents.addAll(elements)

  override fun addAll(index: Int, elements: Collection<Int>): Boolean =
    contents.addAll(index, elements)

  override fun add(index: Int, element: Int) = contents.add(index, element)

  override fun add(element: Int): Boolean = contents.add(element)

  override fun get(index: Int) = contents.get(index)

  override fun isEmpty(): Boolean = contents.isEmpty()

  override fun iterator(): MutableIterator<Int> = contents.iterator()

  override fun listIterator(): MutableListIterator<Int> = contents.listIterator()

  override fun listIterator(index: Int): MutableListIterator<Int> = contents.listIterator(index)

  override fun removeAll(elements: Collection<Int>): Boolean = contents.removeAll(elements)

  override fun remove(element: Int): Boolean = contents.remove(element)

  override fun lastIndexOf(element: Int): Int = contents.lastIndexOf(element)

  override fun removeAt(element: Int): Int = contents.removeAt(element)

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<Int> =
    contents.subList(fromIndex, toIndex)

  override fun set(index: Int, element: Int): Int = contents.set(index, element)

  override fun retainAll(elements: Collection<Int>): Boolean = contents.retainAll(elements)

  override fun indexOf(element: Int): Int = contents.indexOf(element)

  override fun containsAll(elements: Collection<Int>): Boolean = contents.containsAll(elements)

  override fun contains(element: Int): Boolean = contents.contains(element)
}

fun test() {
  requireInt(Holder<Int>(1).get())
  requireInt(IntHolder().get())
  requireInt(SubIntHolder().get())
  requireInt(NumberHolder(3).get())
  requireInt(SubNumberHolder().get())
  requireInt(IntHolderExtendsGetter().get())

  Holder<Int>(1).set(2)
  IntHolder().set(2)
  SubIntHolder().set(2)
  NumberHolder(3).set(2)
  SubNumberHolder().set(2)
  IntHolderExtendsGetter().set(2)
  var s: IntSetter = IntHolderExtendsGetter()
  s.set(2)

  var l = ArrayList<Int>()
  l.remove(2)
  l.removeAt(1)

  val il = IntList()
  il.remove(2)
  il.removeAt(1)

  val ba =
    object : AbstractList<Byte>() {
      override val size: Int = 0

      override fun isEmpty(): Boolean = true

      override fun contains(element: Byte): Boolean = true

      override fun get(index: Int): Byte = 0

      override fun indexOf(element: Byte): Int = 0

      override fun lastIndexOf(element: Byte): Int = 0
    }

  class ByteList : AbstractList<Byte>() {
    override val size: Int = 0

    override fun isEmpty(): Boolean = true

    override fun contains(element: Byte): Boolean = true

    override fun get(index: Int): Byte = 0

    override fun indexOf(element: Byte): Int = 0

    override fun lastIndexOf(element: Byte): Int = 0
  }
}

fun requireInt(i: Int) {}
