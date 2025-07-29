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
package innerclassinheritance

class MultipleNestings {
  open inner class Parent {
    fun `fun`() {}
  }

  fun funInM() {}

  inner class InnerClass1 : Parent() {
    fun funInI1() {}

    /** Inner class that extends the same class as its enclosing class extends. */
    inner class InnerClass2 : Parent() {
      fun funInI2() {}

      fun test() {
        // call to inherited fun() with different qualifier
        this.`fun`() // this.fun()
        `fun`() // this.fun()
        this@InnerClass1.`fun`() // this.outer.fun()

        // call to function of outer classes
        funInM() // this.outer.outer.funInM()
        this@MultipleNestings.funInM() // this.outer.outer.funInM()
        funInI1() // this.outer.funInI1()
        this@InnerClass1.funInI1() // this.outer.funInI1()

        funInI2() // this.funInI2()
      }
    }
  }
}
