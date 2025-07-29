/*
 * Copyright 2023 Google Inc.
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
package unreachablecode

var i: Int = 0

fun alwaysThrows(): Nothing = throw Error()

fun noReturnRequired() {
  i = 1
  alwaysThrows()
  i += 5
}

fun requiresPrimitiveReturn(): Int {
  i = 1
  alwaysThrows()
  return i
}

fun requiresNullablePrimitiveReturn(): Int? {
  i = 1
  alwaysThrows()
  return i
}

fun requiresNonPrimitiveReturn(): Any {
  alwaysThrows()
  return object {}
}

fun requiresNullableNonPrimitiveReturn(): Any? {
  alwaysThrows()
  return object {}
}

fun returnsNothingCall(): Any {
  return alwaysThrows()
}

fun trailingThrow(): Any {
  throw Error()
}

fun lacksReturnStatement(): Int {
  i = 1
  alwaysThrows()
  i += 2
}

fun branchingAlwaysReturns(b: Boolean): Any {
  if (b) {
    alwaysThrows()
    i += 2
  } else {
    i += 3
  }
  return i
}

fun branchingSometimesReturns(b: Boolean): Any {
  if (b) {
    alwaysThrows()
    i += 2
  } else {
    i += 3
    return i
  }
}

class UnreachableCodeInConstructor(val x: Int) {
  init {
    alwaysThrows()
  }

  var y: Int = x + 1
}

fun nullBangBang() {
  var str = null
  var nonNullStr: String = str!!
  nonNullStr += "abc"
}

fun codeAfterContinue() {
  var x = 1
  while (true) {
    continue
    x++
  }
  x * 2
}

fun codeAfterBreak() {
  var x = 1
  while (true) {
    break
    x++
  }
  x * 2
}

private fun interface SomeProvider<T> {
  fun provide(): T
}

// See b/377685454.
fun nestedLambdaWithNothingReturn() {
  val x =
    SomeProvider<String> {
      return@SomeProvider SomeProvider<String> {
          return@SomeProvider null!!
        }
        .provide()
    }
}
