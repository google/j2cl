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
package voidtypes

import jsinterop.annotations.JsFunction

fun f(): Unit {}

val w = f()

fun g(): Unit {
  return Unit
}

val x = g()

fun h(x: Any): Any = x

val y = h(Unit)

fun i(): Nothing {
  throw Error()
}

val z: Int = i()

fun boxing() {
  1.0 == i()
  i() == 1.0
  i() == null
  null == i()
}

fun assignmentToLocal() {
  val local = f()
}

fun assignAndReturn() {
  val a = g()
  return g()
}

fun returnNothingForUnit(): Unit {
  return i()
}

fun returnNothingForAny(): Any {
  return i()
}

fun returnUnitBlock(a: Int): Unit =
  when (a) {
    1 -> f()
    else -> g()
  }

fun unitLambdas() {
  val a: () -> Unit = { f() }
  val b: () -> Unit = ::f
  val c: () -> Unit = ::w

  consumeGenericJsFunction { f() }
  consumeGenericJsFunction(::f)
  consumeGenericJsFunction(::w)
  consumeGenericJsFunction {
    when {
      a == b -> return@consumeGenericJsFunction
    }
    return@consumeGenericJsFunction
  }

  consumeUnitJsFunction { f() }
  consumeUnitJsFunction(::f)
  consumeUnitJsFunction(::w)
  consumeUnitJsFunction {
    when {
      a == b -> return@consumeUnitJsFunction
    }
    return@consumeUnitJsFunction
  }
}

fun unitSpecialization() {
  val unitProducer: Producer<Unit> = UnitProducer()
  val unitConsumer: Consumer<Unit> = UnitConsumer()

  unitConsumer.consume(unitProducer.provide())
}

@JsFunction
fun interface GenericJsFunction<T> {
  fun execute(): T
}

fun <T> consumeGenericJsFunction(f: GenericJsFunction<T>) {}

@JsFunction
fun interface UnitJsFunction {
  fun execute()
}

fun consumeUnitJsFunction(f: UnitJsFunction) = f()

class UnitProducer : Producer<Unit> {
  override fun provide() {}
}

class UnitConsumer : Consumer<Unit> {
  override fun consume(value: Unit) {}
}

class UnitProducerWithMultipleExits(private val i: Int) : Producer<Unit> {
  override fun provide() {
    when {
      i == 10 -> return
    }
  }
}

fun nothingTests() {
  val a = 2
  if (a is Nothing) {
    return
  }
  val b: Int = a as Nothing
  if (b == 3) {
    return
  }
}

object NothingIterator : ListIterator<Nothing> {
  override fun hasNext(): Boolean = throw RuntimeException()

  override fun next(): Nothing = throw RuntimeException()

  override fun hasPrevious(): Boolean = false

  override fun nextIndex() = -1

  override fun previous(): Nothing = throw RuntimeException()

  override fun previousIndex(): Int = -1
}

fun emptyStringIterator(): Iterator<String> = NothingIterator

object NullableNothingIterator : Iterator<Nothing?> {
  override fun hasNext(): Boolean = throw RuntimeException()

  override fun next(): Nothing? = null
}

fun nullableNothingIterator(): Iterator<Nothing?> = NullableNothingIterator

interface Consumer<in T> {
  fun consume(value: T)
}

class NothingConsumer : Consumer<Nothing> {
  override fun consume(value: Nothing) {}
}

interface Producer<out T> {
  fun provide(): T
}

class NothingProducer : Producer<Nothing> {
  override fun provide(): Nothing = throw RuntimeException()
}

object EmptyList : List<Nothing> {
  override val size: Int = 0

  override fun contains(element: Nothing): Boolean = false

  override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

  override fun get(index: Int): Nothing = throw RuntimeException()

  override fun indexOf(element: Nothing): Int = -1

  override fun isEmpty(): Boolean = true

  override fun lastIndexOf(element: Nothing): Int = -1

  override fun iterator(): Iterator<Nothing> = NothingIterator

  override fun listIterator(): ListIterator<Nothing> = NothingIterator

  override fun listIterator(index: Int): ListIterator<Nothing> =
    if (index == 0) NothingIterator else throw RuntimeException()

  override fun subList(fromIndex: Int, toIndex: Int): List<Nothing> =
    if (fromIndex == 0 && toIndex == 0) this else throw RuntimeException()
}

object NothingByStringMap : Map<String, Nothing> {
  override val entries: Set<Map.Entry<String, Nothing>>
    get() = throw RuntimeException()

  override val keys: Set<String>
    get() = throw RuntimeException()

  override val size: Int
    get() = throw RuntimeException()

  override val values: Collection<Nothing>
    get() = throw RuntimeException()

  override fun containsKey(key: String): Boolean {
    throw RuntimeException()
  }

  override fun containsValue(value: Nothing): Boolean {
    throw RuntimeException()
  }

  override fun get(key: String): Nothing? {
    throw RuntimeException()
  }

  override fun isEmpty(): Boolean {
    throw RuntimeException()
  }
}
