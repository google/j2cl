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

fun main() {
  val (foo) = BasicDataClass(1)
  val (a, b, c) = PolymorphicDataType(1, 2, 3)
  IntValueHolder(10).backingValue
}
