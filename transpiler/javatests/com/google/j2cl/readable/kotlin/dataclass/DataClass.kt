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
package dataclass

data class BasicDataClass(val foo: Int, val bar: Int = 10)

interface InterfaceType {
  val a: Int
  val b: Int

  fun doSomething()

  fun doSomethingElse()
}

abstract class AbstractType {
  abstract val a: Int
  abstract val c: Int
  val d: Int = 4

  fun doSomething() {}

  abstract fun oneOtherThing()
}

data class PolymorphicDataType(override val a: Int, override val b: Int, override val c: Int) :
  InterfaceType, AbstractType() {
  override fun doSomethingElse() {}

  override fun oneOtherThing() {}
}

interface ValueHolder<T> {
  val backingValue: T
}

data class IntValueHolder(override val backingValue: Int) : ValueHolder<Int>

data class ArrayMembers(private val a: IntArray, private val b: Array<String>)

// Both actual component properties and computed properties are special treated in Kotlin to follow
// record naming convention (e.g. f()).
// However they still override parent properties which have regular getter name pattern (e.g.
// getF()) as a result they require a bridge.
// Note that generally in Kotlin explicit getter functions (e.g. f() or getF()) are not overridable
// by subclass properties even if the generated getter name match - will be rejected as
// "declarations having the same JVM signature".
interface DataClassInterface {
  val f: Int // overridden by component which requires getF() bridge.
  val other: String // overridden by computed property which requires getOther() bridge.
  val isBar: Any // overridden by computed property but "is" methods doesn't require bridge.

  fun getZ(): Int = 42 // Note that this is NOT overridden by "val z: String" component.

  fun component2(): String // overridden by synthetic Data.component2() method.
}

@kotlin.jvm.JvmRecord
data class JvmRecordDataClass(override val f: Int, val z: String) : DataClassInterface {
  override val other: String
    get() = "other"

  override val isBar: String
    get() = "bar"

  val other2: String
    get() = "other2"

  companion object {
    @kotlin.jvm.JvmField val staticField = 1
  }
}

@kotlin.jvm.JvmRecord
data class JvmRecordDataClassOverridingEquals(val s: String) {
  override fun equals(other: Any?): Boolean {
    return false
  }
}

fun testDataClass() {
  val (foo) = BasicDataClass(1)
  val (a, b, c) = PolymorphicDataType(1, 2, 3)
  IntValueHolder(10).backingValue
  val (f, z) = JvmRecordDataClass(1, "a")
}
