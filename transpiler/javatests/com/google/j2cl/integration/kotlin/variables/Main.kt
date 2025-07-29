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
package variables

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** A class that has the same simple name of the runtime class Asserts */
internal class Asserts

fun testJSKeywords(`in`: Int): Int {
  val let = 100
  return let + `in`
}

private fun testClassLocalVarCollision() {
  val MainKt = true
  val a = Asserts()
  val Asserts = 1
  val `$Asserts` = 2
  val l_Asserts = 3
  assertTrue(MainKt)
  assertTrue(a is Asserts)
  assertTrue(Asserts == 1)
  assertTrue(`$Asserts` == 2)
  assertTrue(l_Asserts == 3)
}

private fun testClassParameterCollision(
  MainKt: Boolean,
  Asserts: Any,
  `$Asserts`: Any,
  l_Asserts: Int,
) {
  assertTrue(MainKt)
  assertTrue(Asserts is Asserts)
  assertTrue(`$Asserts` is CollisionInConstructor)
  assertTrue(l_Asserts == 1)
}

private class CollisionInConstructor() {
  constructor(
    CollisionInConstructor: Boolean,
    Asserts: Any?,
    `$Asserts`: Any?,
    l_Asserts: Int,
  ) : this() {
    assertTrue(CollisionInConstructor)
    assertTrue(Asserts is Asserts)
    assertTrue(`$Asserts` is CollisionInConstructor)
    assertTrue(l_Asserts == 1)
  }
}

private fun testClassParameterCollisionInConstructor() {
  CollisionInConstructor(true, Asserts(), CollisionInConstructor(), 1)
}

private fun testVariableInference() {
  val str = "String"
  assertTrue(str is String)

  // The type of anonymous is not Runnable but the actual anonymous class which is non-denotable.
  val anonymous =
    object : Runnable {
      override fun run() {}

      fun exposedMethod(): String {
        return "Hello"
      }
    }
  assertTrue("Hello" == anonymous.exposedMethod())
}

fun main(vararg unused: String) {
  assertTrue(testJSKeywords(10) == 110)
  testClassLocalVarCollision()
  testClassParameterCollision(true, Asserts(), CollisionInConstructor(), 1)
  testClassParameterCollisionInConstructor()
  testVariableInference()
}
