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

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testSpecialization_returnType_fromGeneralizedSupertype()
  testSpecialization_returnType_fromSpecializedSupertype()
  testSpecialization_parameterType_fromGeneralizedSupertype()
  testSpecialization_parameterType_fromSpecializedSupertype()
  testSpeciialization_specialOverrides()
}

fun testSpecialization_returnType_fromGeneralizedSupertype() {
  open class Holder<T>(val content: T) {
    fun get(): T {
      return content
    }
  }
  // Use a parameterized class.
  assertIsIntOne(Holder(1).get())

  // Use a subclass of a parameterized class, to see that the parameterization is preserved.
  open class IntHolder : Holder<Int>(1)
  assertIsIntOne(IntHolder().get())

  // One more level to see that the parameterization is preserved transitively.
  class SubIntHolder : IntHolder()
  assertIsIntOne(SubIntHolder().get())

  // Parameterize using a type variable.
  open class NumberHolder<T : Number>(t: T) : Holder<T>(t)
  assertIsIntOne(NumberHolder(1).get())

  // Extend to show that substitutions happen recursively as well.
  class SubNumberHolder : NumberHolder<Int>(1)
  assertIsIntOne(SubNumberHolder().get())

  // Add an accidental override of "T Holder.get()" by "int IntGetter.get()".
  class IntHolderExtendsGetter : Holder<Int>(1), IntGetter
  assertIsIntOne(IntHolderExtendsGetter().get())

  var i: IntGetter = IntHolderExtendsGetter()
  assertIsIntOne(i.get())
}

fun testSpecialization_returnType_fromSpecializedSupertype() {
  open class IntHolder(val content: Int) {
    fun get(): Int {
      return content
    }
  }
  // Use the specialized class.
  assertIsIntOne(IntHolder(1).get())

  // Implement a generalized interface. Here we will have an accidental override.
  open class IntHolderImplementsGetter : IntHolder(1), Getter<Int>
  assertIsIntOne(IntHolderImplementsGetter().get())

  var getter: Getter<Int> = IntHolderImplementsGetter()
  // Call through the general getter.
  assertIsIntOne(getter.get())
}

fun testSpecialization_parameterType_fromGeneralizedSupertype() {
  open class Holder<T> {
    var content: T = null as T

    fun get(): T {
      return content
    }

    fun set(t: T) {
      content = t
    }
  }
  // Use a parameterized class.
  var holderInt = Holder<Int>()
  holderInt.set(1)
  assertIsIntOne(holderInt.get())

  // Use a subclass of a parameterized class, to see that the parameterization is preserved.
  open class IntHolder : Holder<Int>()
  var intHolder = IntHolder()
  intHolder.set(1)
  assertIsIntOne(intHolder.get())

  // One more level to see that the parameterization is preserved transitively.
  class SubIntHolder : IntHolder()
  var subIntHolder = SubIntHolder()
  subIntHolder.set(1)
  assertIsIntOne(subIntHolder.get())

  // Parameterize using a type variable.
  open class NumberHolder<T : Number>() : Holder<T>()
  var numberHolder = NumberHolder<Int>()
  numberHolder.set(1)
  assertIsIntOne(numberHolder.get())

  // Extend to show that substitutions happen recursively as well.
  class SubNumberHolder : NumberHolder<Int>()
  var subNumberHolder = SubNumberHolder()
  subNumberHolder.set(1)
  assertIsIntOne(subNumberHolder.get())

  // Add an accidental override of "T Holder.get()" by "int IntGetter.get()".
  class IntHolderExtendsSetter : Holder<Int>(), IntSetter
  var intHolderExtendsSetter = IntHolderExtendsSetter()
  intHolderExtendsSetter.set(1)
  assertIsIntOne(intHolderExtendsSetter.get())

  var i: IntSetter = IntHolderExtendsSetter()
  i.set(1)
  assertIsIntOne((i as Holder<Int>).get())
}

fun testSpecialization_parameterType_fromSpecializedSupertype() {
  open class IntHolder {
    var content: Int = 0

    fun get(): Int {
      return content
    }

    fun set(i: Int) {
      this.content = i
    }
  }
  // Use the specialized class.
  val intHolder = IntHolder()
  intHolder.set(1)
  assertIsIntOne(intHolder.get())

  // Implement a generalized interface. Here we will have an accidental override.
  open class IntHolderImplementsSetter : IntHolder(), Setter<Int>
  var intHolderImplementsSetter = IntHolderImplementsSetter()
  intHolderImplementsSetter.set(1)
  assertIsIntOne(intHolderImplementsSetter.get())

  var setter: Setter<Any> = intHolderImplementsSetter as Setter<Any>
  assertThrowsClassCastException { setter.set(Any()) }
}

interface Setter<T> {
  fun set(t: T)
}

interface Getter<T> {
  fun get(): T
}

interface IntSetter {
  fun set(i: Int)
}

interface IntGetter {
  fun get(): Int
}

fun assertIsIntOne(i: Int) {
  // Use arithmetic so that when if operation is performed and the class arrived boxed
  // the operation will fail.
  assertTrue(2 == i + 1)
}

private class TestIntArrayIntList : ArrayList<Int>() {
  override fun lastIndexOf(element: Int): Int = super.lastIndexOf(element)

  override fun indexOf(element: Int): Int = super.indexOf(element)

  override fun containsAll(elements: Collection<Int>): Boolean = super.containsAll(elements)

  override fun contains(element: Int): Boolean = super.contains(element)

  override fun removeAt(element: Int): Int = super.removeAt(element)

  override fun get(index: Int): Int = super.get(index)
}

private class TestIntListFromScratch : MutableList<Int> {
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

private fun testSpeciialization_specialOverrides() {
  val intList = TestIntArrayIntList()
  intList.add(1)
  intList.add(2)
  intList.add(1)
  assertEquals(1, intList.indexOf(2))
  assertEquals(2, intList.lastIndexOf(1))
  assertTrue(intList.containsAll(listOf(1, 2)))
  assertFalse(intList.containsAll(listOf(3)))
  assertTrue(1 == intList[0])

  var oList = intList as java.util.List<Any>
  // Check that the parameterized versions don't throw but return the right result.
  assertEquals(false, oList.contains(Any()))
  assertEquals(true, oList.contains(2))
  assertEquals(-1, oList.indexOf(Any()))
  assertEquals(-1, oList.indexOf(5))
  assertEquals(1, oList.indexOf(2))

  val intListFromScratch = TestIntListFromScratch()
  intListFromScratch.add(1)
  intListFromScratch.add(2)
  intListFromScratch.add(1)
  assertEquals(1, intListFromScratch.indexOf(2))
  assertEquals(2, intListFromScratch.lastIndexOf(1))
  assertTrue(intListFromScratch.containsAll(listOf(1, 2)))
  assertFalse(intListFromScratch.containsAll(listOf(3)))
  assertTrue(1 == intListFromScratch[0])

  // Check that the parameterized versions don't throw but return the right result.
  oList = intListFromScratch as java.util.List<Any>
  assertEquals(false, oList.contains(Any()))
  assertEquals(true, oList.contains(2))
  assertEquals(-1, oList.indexOf(Any()))
  assertEquals(-1, oList.indexOf(5))
  assertEquals(1, oList.indexOf(2))
}
