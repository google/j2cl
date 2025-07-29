/*
 * Copyright 2017 Google Inc.
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
package foreachstatement

class ForEachStatement {
  fun test(iterable: Iterable<Throwable?>) {
    for (t in iterable) {
      t.toString()
    }

    for (t in Array<Throwable?>(10) { null }) {
      t.toString()
    }
  }

  // Kotlin doesn't have multicatch
  //
  // class Exception1 : Exception(), Iterable<String?> {
  //   override operator fun iterator(): Iterator<String?> = TODO()
  // }
  //
  // class Exception2 : Exception(), Iterable<Any?> {
  //   override operator fun iterator(): Iterator<Any?> = TODO()
  // }
  //
  // private fun testMulticatch() {
  //   try {
  //     throw Exception();
  //   } catch (e: Exception1 | Exception2) {
  //     for (o in e) {}
  //   }
  // }

  private fun <T, U : T, V> testTypeVariable() where
  T : Any?,
  T : Iterable<String?>,
  V : Iterable<Int?> {
    val iterable: U? = null
    for (s in iterable!!) {}

    val anotherIterable: IterableReturningTypeVariable<*, *>? = null
    for (s in anotherIterable!!) {}

    // Kotlin doesn't support auto unboxing or widening in foreach statements.
    //
    // This is an auto-unboxing via an intersection iterable type.
    // val integerIterable: V? = null
    // for (i: Int in integerIterable!!)
    // This is an auto-unboxing and widening via an intersection iterable type.
    // for (i: Long in integerIterable!!)
  }
}

class IterableReturningTypeVariable<U, T : Iterator<Int?>> : Iterable<Int?> {
  override operator fun iterator(): T = TODO()
}

internal interface StringIterable : Iterable<String?> {
  override fun iterator(): StringIterator
}

internal interface StringIterator : MutableIterator<String?>

private fun testOverriddenIterator(i: StringIterable) {
  for (s in i) {}
}
