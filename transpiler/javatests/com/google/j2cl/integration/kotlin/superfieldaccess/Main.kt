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
package superfieldaccess

import com.google.j2cl.integration.testing.Asserts.assertEquals

/** Test field access using the `super` keyword. */
fun main(vararg unused: String) {
  testSuperFieldAccess()
}

private fun testSuperFieldAccess() {
  open class GrandParent {
    open var name = "GrandParent"
  }

  open class Parent(from: String) : GrandParent() {
    override var name = "Parent (from " + from + ")"

    open fun getParentName(): String {
      return super.name
    }
  }

  class Child(from: String) : Parent(from) {
    constructor() : this("Child")

    override var name = "Child (from " + from + ")"

    override fun getParentName(): String {
      return super.name
    }

    fun getGrandParentName(): String {
      return super.getParentName()
    }

    inner class Inner : Parent("Inner") {
      fun getOuterParentName(): String {
        return super@Child.name
      }

      inner class InnerInner {
        fun getOuterParentNameQualified(): String {
          // In here to keep in sync with the Java version. Kotlin does not allow to use
          // a qualified name to qualify a super reference.
          return super@Inner.name
        }
      }
    }
  }
  assertEquals("Parent (from Child)", Child().getParentName())
  assertEquals("GrandParent", Child().getGrandParentName())
  assertEquals("Parent (from Child)", Child().Inner().getOuterParentName())
  assertEquals("Parent (from Inner)", Child().Inner().InnerInner().getOuterParentNameQualified())
}
