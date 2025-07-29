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

// Direct subclasses of sealed classes and interfaces must be declared in the same package.
// class SubClass: SealedClassInAnotherPackage()

sealed interface Animal

sealed class Mammal : Animal {
  // Unlike normal class, the default visibility for sealed class constructors is protected
  constructor() {}
  private constructor(name: String) : this() {}

  fun retrieveName(): String {
    return "Animal"
  }

  abstract fun changeName(updatedName: String)

  // Subclasses can have any visibility
  protected class Dolphin() : Mammal() {
    override fun changeName(updatedName: String) {}
  }
}

class Dog() : Mammal() {
  override fun changeName(updatedName: String) {}
}

internal class Cat() : Mammal() {
  override fun changeName(updatedName: String) {}
}

private class Whale() : Mammal() {
  override fun changeName(updatedName: String) {}
}

object Deer : Animal

// Enum classes cannot extend a sealed class, but they can implement sealed interfaces.
enum class EnumImplementsSealedInterface : Animal {
  V1,
  V2
}

sealed class Shape {
  class Circle() : Shape()

  // Object in a sealed class
  object Rectangle : Shape()
}

// The else branch is not mandatory in when expression if when has a subject of sealed type
private fun checkShape(e: Shape) =
  when (e) {
    is Shape.Circle -> "Circle"
    Shape.Rectangle -> "Rectangle"
  }
