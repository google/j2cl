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
package interfaces

class Main {

  interface Interface<T> {

    fun interfaceMethod()

    fun defaultMethod(t: T) {
      this.privateMethod(t)
    }

    private fun privateMethod(t: T) {}

    companion object {
      val a = 1
      val b = 2

      fun staticInterfaceMethod() {}
    }

    override fun toString(): String
  }

  interface SubInterface : Interface<String?> {
    override fun defaultMethod(s: String?) {
      super<Interface>.defaultMethod(s)
    }
  }

  inner class Implementor : SubInterface, Interface<String?> {
    override fun interfaceMethod() {}

    // In Kotlin, explicit implementations of `Any` methods are required if defined
    // on the interface.
    override fun toString(): String = super.toString()
  }

  abstract inner class AbstractImplementor : SubInterface

  enum class EnumImplementor : SubInterface {
    ONE;

    override fun interfaceMethod() {}
  }

  fun testInterfaceMembers() {
    val i: Interface<String?> = Implementor()
    i.interfaceMethod()
    i.defaultMethod(null)
    i.toString()

    val impl: Implementor = Implementor()
    impl.defaultMethod(null)

    val enumImpl: EnumImplementor = EnumImplementor.ONE
    enumImpl.defaultMethod(null)

    Interface.staticInterfaceMethod()
    Interface.Companion.staticInterfaceMethod()
    val x = Interface.a + Interface.b
    val y = Interface.Companion.a + Interface.Companion.b

    val si: SubInterface = Implementor()
    si.interfaceMethod()
    si.defaultMethod(null)
  }
}
