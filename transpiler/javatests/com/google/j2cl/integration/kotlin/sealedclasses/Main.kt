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
package sealedclasses

import com.google.j2cl.integration.testing.Asserts.assertEquals

sealed interface Animal

sealed class Mammal : Animal {
  constructor() {}

  private constructor(name: String) : this() {}

  fun retrieveName(): String {
    return "Animal"
  }

  abstract fun changeName(updatedName: String): String

  class Dolphin() : Mammal() {
    override fun changeName(updatedName: String): String {
      return updatedName
    }
  }
}

class Dog() : Mammal() {
  override fun changeName(updatedName: String): String {
    return updatedName
  }
}

object Cat : Animal

// The else branch is not mandatory in when expression if when has a subject of sealed type
private fun checkKind(e: Animal) =
  when (e) {
    is Mammal.Dolphin -> "Dolphin"
    is Dog -> "Dog"
    Cat -> "Cat"
  }

private fun testSealedClasses() {
  assertEquals("Animal", Dog().retrieveName())
  assertEquals("Dog2", Dog().changeName("Dog2"))
}

private fun testSealedClassesInWhenExpression() {
  assertEquals("Dolphin", checkKind(Mammal.Dolphin()))
  assertEquals("Dog", checkKind(Dog()))
  assertEquals("Cat", checkKind(Cat))
}

fun main(vararg unused: String) {
  testSealedClasses()
  testSealedClassesInWhenExpression()
}
