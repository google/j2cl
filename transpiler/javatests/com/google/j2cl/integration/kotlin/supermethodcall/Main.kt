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
package supermethodcall

import com.google.j2cl.integration.testing.Asserts.assertEquals

/** Test non-constructor super method calls. */
fun main(vararg unused: String) {
  testSuperMethodCalls()
  testSuperMethodCalls_returnSpecialization()
  testSuperMethodCalls_parameterSpecialization()
  testSuperMethodCalls_defaultMethods()
}

private fun testSuperMethodCalls() {
  open class GrandParent {
    open fun getName(): String? {
      return "GrandParent"
    }
  }

  open class Parent : GrandParent() {
    override fun getName(): String? {
      return "Parent"
    }

    open fun getParentName(): String? {
      return super.getName()
    }
  }

  class Child : Parent() {
    override fun getName(): String? {
      return "Child"
    }

    override fun getParentName(): String? {
      return super.getName()
    }

    fun getGrandParentName(): String? {
      return super.getParentName()
    }

    inner class Inner {
      fun getOuterParentName(): String? {
        return super@Child.getName()
      }
    }
  }
  assertEquals("Parent", Child().getParentName())
  assertEquals("GrandParent", Child().getGrandParentName())
  assertEquals("Parent", Child().Inner().getOuterParentName())
}

private fun testSuperMethodCalls_returnSpecialization() {
  open class GrandParent {
    open fun getName(): CharSequence? {
      return "GrandParent"
    }
  }

  open class Parent : GrandParent() {
    override fun getName(): String? {
      return "Parent"
    }

    open fun getParentName(): String? {
      return super.getName() as String
    }
  }

  class Child : Parent() {
    override fun getName(): String? {
      return "Child"
    }

    override fun getParentName(): String? {
      return super.getName()
    }

    fun getGrandParentName(): String? {
      return super.getParentName()
    }
  }

  assertEquals("Parent", Child().getParentName())
  assertEquals("GrandParent", Child().getGrandParentName())
}

private fun testSuperMethodCalls_parameterSpecialization() {
  open class GrandParent<T> {
    open fun getName(o: T): T {
      return "GrandParent" as T
    }
  }

  open class Parent<T : CharSequence?> : GrandParent<T>() {
    override fun getName(o: T): T {
      return "Parent" as T
    }

    open fun getParentName(o: T): T {
      // Dispatches to an override.
      return super.getName(o)
    }
  }

  class Child : Parent<String?>() {
    // Parameter Specialization
    override fun getName(o: String?): String? {
      return "Child"
    }

    override fun getParentName(o: String?): String? {
      return super.getName(o)
    }

    fun getGrandParentName(o: String?): String? {
      return super.getParentName(o)
    }
  }

  assertEquals("Parent", Child().getParentName(null))
  assertEquals("GrandParent", Child().getGrandParentName(null))
}

interface TopInterface {
  fun getName(): String {
    return "Top"
  }

  fun getDiamondName(): String {
    return "Diamond"
  }
}

interface LeftInterface : TopInterface {
  override fun getName(): String {
    return "Left"
  }

  fun getSuperName(): String {
    return super.getName()
  }
}

interface RightInterface : TopInterface {
  override fun getName(): String {
    return "Right"
  }

  fun getSuperName(): String {
    return super.getName()
  }
}

interface BottomInterface : LeftInterface, RightInterface {
  override fun getName(): String {
    return "Bottom"
  }

  override fun getSuperName(): String {
    return super<LeftInterface>.getName() + "+" + super<RightInterface>.getName()
  }

  fun getSuperSuperName(): String {
    return super<LeftInterface>.getSuperName() + "+" + super<RightInterface>.getSuperName()
  }
}

interface GrandBottomInterface : BottomInterface {
  override fun getDiamondName(): String {
    return super.getDiamondName()
  }
}

private fun testSuperMethodCalls_defaultMethods() {
  assertEquals("Top", object : TopInterface {}.getName())
  assertEquals("Top", object : LeftInterface {}.getSuperName())
  assertEquals("Left", object : LeftInterface {}.getName())
  assertEquals("Top", object : RightInterface {}.getSuperName())
  assertEquals("Right", object : RightInterface {}.getName())
  assertEquals("Top+Top", object : BottomInterface {}.getSuperSuperName())
  assertEquals("Left+Right", object : BottomInterface {}.getSuperName())
  assertEquals("Bottom", object : BottomInterface {}.getName())
  assertEquals("Diamond", object : GrandBottomInterface {}.getDiamondName())
}
