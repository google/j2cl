/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

open class ParentOuter {
  var fieldInParentOuter: Int = 0

  open fun funInParentOuter() {}
}

open class ParentInner {
  var fieldInParentInner: Int = 0

  fun funInParentInner() {}
}

open class ChildClass : ParentOuter() {
  var fieldInOuter: Int = 0

  fun funInOuter() {}

  inner class InnerClass : ParentInner() {
    var fieldInInner: Int = 0

    fun funInInner() {}

    fun testInnerClass(): Int {
      // call to outer class's function inherited from ParentOuter
      funInParentOuter() // this.outer.funInParentOuter()
      this@ChildClass.funInParentOuter() // this.outer.funInParentOuter()

      // call to outer class's own declared function.
      funInOuter() // this.outer.funInOuter()
      this@ChildClass.funInOuter() // this.outer.funInOuter()

      // call to parent class's function.
      funInParentInner() // this.funInParentIner()
      this.funInParentInner() // this.funInParentInner()

      // call to own declared function.
      funInInner() // this.funInInner()
      this.funInInner() // this.funInInner()

      var a: Int

      // accesses to outer class's own or inherited fields.
      a = this@ChildClass.fieldInParentOuter
      a = fieldInParentOuter

      a = this@ChildClass.fieldInOuter
      a = fieldInOuter

      // accesses to its own or inherited fields.
      a = this.fieldInParentInner
      a = fieldInParentInner
      a = this.fieldInInner
      a = fieldInInner
      return a
    }
  }

  fun testLocalClass() {
    class LocalClass : ChildClass() {
      var o = this

      override fun funInParentOuter() {}

      fun test() {
        funInOuter() // this.funInOuter
        this@ChildClass.funInOuter() // $outer.funInOuter
        funInParentOuter() // this.funInParentOuter
        this.funInParentOuter() // this.funInParentOuter
        this@ChildClass.funInParentOuter() // $outer.funInParentOuter()
      }
    }
    LocalClass().test()
  }
}
