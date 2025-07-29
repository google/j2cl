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
package interfaceabstractimpl

import com.google.j2cl.integration.testing.Asserts.assertTrue

/**
 * Multiple inheritance. A -> I, J, B I -> W J -> W, X, Z B -> W, X, Y Z -> X All funIn**()
 * functions are unimplemented in A, barIn**() functions are implemented in A's super classes.
 */
internal interface W {
  fun funInW(): Int

  fun barInW(): Int
}

internal interface X {
  fun funInX(): Int

  fun barInX(): Int
}

internal interface Y {
  fun funInY(): Int

  fun barInY(): Int
}

internal interface Z : X {
  fun funInZ(): Int

  fun barInZ(): Int

  fun klmInZ(): Int
}

internal interface I : W {
  fun funInI(): Int

  fun barInI(): Int
}

internal interface J : W, Z, X {
  fun funInJ(): Int

  fun barInJ(): Int
}

open class E {
  fun klmInZ(): Int {
    return 42
  }
}

open class D : E()

abstract class C : D(), Z {
  override fun barInZ(): Int {
    return 1
  }

  override fun barInX(): Int {
    return 2
  }

  abstract fun barInI(): Int
  // klmInZ from grandparent is accidentally overridden
}

abstract class B : C(), W, X, Y {
  override fun barInW(): Int {
    return 3
  }

  abstract override fun barInY(): Int
}

abstract class A : B(), I, J {
  override fun barInI(): Int {
    return 4
  }
  // All funIn**() functions should be added otherwise JSCompiler errors.

  // barIn**() functions should be not added because they are inherited from super classes.
}

internal class SubA : A() {
  override fun funInI(): Int {
    return 5
  }

  override fun funInW(): Int {
    return 6
  }

  override fun funInJ(): Int {
    return 7
  }

  override fun barInJ(): Int {
    return 8
  }

  override fun funInZ(): Int {
    return 9
  }

  override fun funInX(): Int {
    return 10
  }

  override fun funInY(): Int {
    return 11
  }

  override fun barInY(): Int {
    return 12
  }
}

fun main(vararg unused: String) {
  val subA = SubA()
  assertTrue(subA.barInI() == 4)
  assertTrue(subA.barInJ() == 8)
  assertTrue(subA.barInW() == 3)
  assertTrue(subA.barInX() == 2)
  assertTrue(subA.barInY() == 12)
  assertTrue(subA.barInZ() == 1)
  assertTrue(subA.funInI() == 5)
  assertTrue(subA.funInJ() == 7)
  assertTrue(subA.funInW() == 6)
  assertTrue(subA.funInX() == 10)
  assertTrue(subA.funInY() == 11)
  assertTrue(subA.funInZ() == 9)
  assertTrue(subA.klmInZ() == 42)
}
