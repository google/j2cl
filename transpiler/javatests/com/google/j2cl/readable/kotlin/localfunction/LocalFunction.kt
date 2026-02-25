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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.§
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package localfunction

fun retrieveArea(width: Int, height: Int): Int {
  val offset = 1
  fun localFunction(): Int {
    fun nestedLocalFunction(): Int {
      return 2
    }
    return width * height + nestedLocalFunction() + offset
  }
  return localFunction()
}

class LocalFunInsideConstructor {
  val index: Int
  val message: String

  constructor(index: Int, data: String) {
    this.index = index
    fun localFunctionInCtor(): String {
      return data + "[" + this.index + "]"
    }
    this.message = localFunctionInCtor()
  }
}

fun localFunInForLoop(start: Int, end: Int): String {
  var message = ""
  for (k in start..end) {
    fun localFunctionInLoop(): String = "Hello"
    message += localFunctionInLoop()
  }
  return message
}

class ParametrizedClass<T>(val classT: T) {
  fun <S> outerfunction(s: S): String {
    fun <U> localFunction(u: U, t: T): String {
      return s.toString() + u.toString() + t.toString() + classT.toString()
    }
    return localFunction(classT, classT)
  }
}

private fun outerFunction(): String {
  var outerFunctionVar = "outerFunctionVar init"
  fun localFunctionChangingOuterScope() {
    outerFunctionVar = "outerFunctionVar modified"
  }

  localFunctionChangingOuterScope()

  return outerFunctionVar
}

private fun localExtensionFunction(): String {
  fun String.localFunction(prefix: String): String {
    fun String.nestedLocalFunction(prefix: String): String {
      return prefix + this@localFunction + this + "".nestedLocalFunction("")
    }
    return prefix + "bar".nestedLocalFunction("baz")
  }

  return "foo".localFunction("")
}

private fun localFunctionInInlinedLambda(): String {
  return StringBuilder()
    .apply {
      fun Appendable.appendTwoDigits(number: Int) {
        if (number < 10) append('0')
        append(number)
      }
      appendTwoDigits(1)
    }
    .toString()
}

class LocalExtensionFunctionInClass {
  fun m(): String {
    fun LocalExtensionFunctionInClass.localFunctionWithImplicitQualifier(): String {
      return "LocalExtensionFunctionInClass" + this
    }
    return localFunctionWithImplicitQualifier()
  }
}

private fun <T> localFunctionWithGenericType(t: T): String {
  fun <T : CharSequence> localFunction(t: T): String {
    fun <T : Number> nestedLocalFunction(t: T): String {
      return t.toInt().toString()
    }
    return t.toString() + nestedLocalFunction(1.2)
  }
  return t.toString() + localFunction("Foo")
}

private fun testConflictingLocalFunction() {
  fun localFunction1() {
    fun conflictingInnerLocalFunction() {}
    conflictingInnerLocalFunction()
  }

  fun localFunction2() {
    fun conflictingInnerLocalFunction() {}
    conflictingInnerLocalFunction()
  }

  localFunction1()
  localFunction2()
}

private fun testLocalFunctionOverloads(): Int {
  fun localFunction(i: Int): Int {
    return i
  }
  fun localFunction(i: Int, j: Int): Int {
    return i + j
  }
  return localFunction(1) + localFunction(1, 2)
}

private fun testBoxingUnboxing() {
  fun unboxedBoolean(): Boolean {
    fun boxedBoolean(): Boolean? {
      // The boxing pass should insert a boxing operation here.
      return true
    }
    return true
  }
}

private fun testSameNameLocalFunctions() {
  class LocalCall {
    // Not a local function
    fun local() {}
  }
  if (true) {
    fun local(): String = "foo"
    local()
  }
  if (true) {
    fun local(): String = "bar"
    local()
  }

  var f = {
    fun local(): String = "baz"
    local()
  }

  f = {
    fun local(): String = "zoo"
    local()
  }
}

// TODO(b/428046269): Re-enable when default arguments are supported for local functions.
// private fun testLocalFunctionWithDefaultArgument(): Int {
//   fun localFunction(i: Int, j: Int = 0): Int {
//     return i + j
//   }
//   return localFunction(1) + localFunction(1, 2)
// }
