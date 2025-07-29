/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package localfunction

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testLocalFunction()
  testLocalFunction_inConstructor()
  testLocalFunction_inForLoop()
  testLocalFunction_parameterizedType()
  testLocalFunction_changingOuterLevelScopeVariable()
  testLocalFunctionInAnonymousClass()
}

private fun testLocalFunction() {
  fun retrieveArea(width: Int, height: Int): Int {
    val offset = 1
    fun localFunction(): Int {
      fun nestedLocalFunction(): Int {
        fun f(): Int = 2
        return 2 + f()
      }
      return width * height + nestedLocalFunction() + offset
    }
    return localFunction()
  }

  assertTrue(retrieveArea(3, 5) == 20)

  fun String.localExtensionFunction(): String {
    val baz = "baz"
    fun String.nestedLocalFunction(): String {
      return this + baz
    }
    return this + "bar".nestedLocalFunction()
  }
  assertEquals("foobarbaz", "foo".localExtensionFunction())
}

class LocalFunInsideConstructor {
  val message: String

  constructor(data: String) {
    fun localFunctionInCtor(): String {
      return data + " " + "World"
    }
    this.message = localFunctionInCtor()
  }
}

private fun testLocalFunction_inConstructor() {
  assertEquals("Hello World", LocalFunInsideConstructor("Hello").message)
}

private fun testLocalFunction_inForLoop() {
  var message = ""
  for (k in 3..5) {
    fun localFunctionInLoop(): String = "Hello"
    message += localFunctionInLoop()
  }

  assertEquals("HelloHelloHello", message)
}

class ParametrizedClass<T>(val classT: T) {
  fun <S> outerfunction(s: S): String {
    fun <U> localFunction(u: U, t: T): String {
      return s.toString() + u.toString() + t.toString() + classT.toString()
    }
    return localFunction(classT, classT)
  }
}

private fun testLocalFunction_parameterizedType() {
  assertEquals("s111", ParametrizedClass<Int>(1).outerfunction("s"))
}

var topLevelProperty = "topLevelProperty init"

private fun testLocalFunction_changingOuterLevelScopeVariable() {
  var outerFunctionVar = "outerFunctionVar init"
  fun localFunctionChangingOuterScope() {
    topLevelProperty = "topLevelProperty modified"
    outerFunctionVar = "outerFunctionVar modified"
  }

  assertEquals("topLevelProperty init", topLevelProperty)
  localFunctionChangingOuterScope()
  assertEquals("outerFunctionVar modified", outerFunctionVar)
  assertEquals("topLevelProperty modified", topLevelProperty)
}

private fun testLocalFunctionInAnonymousClass() {
  var capturedVar = "initial"

  val x =
    object : Any() {
      fun funInAnonymousClass(): String {
        fun localFunctionInAnonymousClassWithSideEffect(): String {
          capturedVar = "captured"
          return "Hello"
        }
        return localFunctionInAnonymousClassWithSideEffect() + ", World!"
      }
    }
  assertEquals("Hello, World!", x.funInAnonymousClass())
  assertEquals("captured", capturedVar)
}
