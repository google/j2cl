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
package builtins

enum class Foo {
  FOO
}

fun testBuiltinPropertiesWithDifferentNameOnJvm() {
  val a = Foo.FOO.name // name()
  val b = Foo.FOO.ordinal // ordinal()

  val collection: Collection<String> = emptyList()
  val c = collection.size // size()
  val list = ArrayList<String>()
  val d = list.size // size()

  val map: Map<String, String> = emptyMap()
  val e = map.size // size()
  val f = map.keys // keySet()
  val g = map.values // values()
  val h = map.entries // entrySet()
  val hashMap = HashMap<String, String>()
  val i = hashMap.size // size()
  val j = hashMap.keys // keySet()
  val k = hashMap.values // values()
  val l = hashMap.entries // entrySet()

  val charSequence: CharSequence = "abcd"
  val m = charSequence.length // length()
  val string = ""
  val n = string.length // length()
}

fun testBuiltinFunctionWithDifferentJvmName() {
  val list: MutableList<String> = mutableListOf()
  list.removeAt(0) // remove(0)
  val arrayList = ArrayList<String>()
  arrayList.removeAt(0) // remove(0)

  val number: Number = 1
  val a = number.toByte() // byteValue()
  val b = number.toShort() // shortValue()
  val c = number.toInt() // intValue()
  val d = number.toLong() // longValue()
  val e = number.toFloat() // floatValue()
  val f = number.toDouble() // doubleValue()
  val int = 1
  val g = int.toByte() // byteValue()
  val h = int.toShort() // shortValue()
  val i = int.toInt() // intValue()
  val j = int.toLong() // longValue()
  val k = int.toFloat() // floatValue()
  val l = int.toDouble() // doubleValue()

  val charSequence: CharSequence = "abcd"
  val m = charSequence.get(0) // charAt(0)
  val n = charSequence[0] // charAt(0)
  val string = ""
  val o = string.get(0) // charAt(0)
  val p = string[0] // charAt(0)
}

class OverrideBuiltMembersWithDifferentJvmName() : CharSequence {
  // property getter should be named length()
  override val length: Int = 0

  // will be renamed charAt()
  override fun get(index: Int): Char {
    return 'a'
  }

  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
    return "a"
  }
}

class SomeList<T>(override val size: Int) : AbstractMutableList<T>() {
  override fun add(index: Int, element: T) {
    TODO("Not yet implemented")
  }

  override fun get(index: Int): T {
    TODO("Not yet implemented")
  }

  override fun removeAt(index: Int): T {
    TODO("Not yet implemented")
  }

  override fun set(index: Int, element: T): T {
    TODO("Not yet implemented")
  }

  override fun toArray(): Array<Any?> {
    TODO("Not yet implemented")
  }
}

// MutableListIterator will be resolved as java.util.ListIterator, but there's mismatch in the type
// variable names between the two. Kotlin uses <T> whereas Java uses <E>.
class SomeMutableIterator<V> : MutableListIterator<V> {
  override fun add(element: V) {
    TODO("Not yet implemented")
  }

  override fun hasNext(): Boolean {
    TODO("Not yet implemented")
  }

  override fun hasPrevious(): Boolean {
    TODO("Not yet implemented")
  }

  override fun next(): V {
    TODO("Not yet implemented")
  }

  override fun nextIndex(): Int {
    TODO("Not yet implemented")
  }

  override fun previous(): V {
    TODO("Not yet implemented")
  }

  override fun previousIndex(): Int {
    TODO("Not yet implemented")
  }

  override fun remove() {
    TODO("Not yet implemented")
  }

  override fun set(element: V) {
    TODO("Not yet implemented")
  }
}
