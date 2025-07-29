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
package properties

import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testProperties_complexInitializer()
  testProperties_interfaceWithProperties()
  testProperties_overrides()
  testProperties_accidentalOverrides()
}

private fun testProperties_complexInitializer() {
  class Properties(normalParam: Int) {
    val complexPropertyInitializer =
      if (normalParam > 0) normalParam else throw NullPointerException()
  }

  assertTrue(Properties(1).complexPropertyInitializer == 1)
  assertThrowsNullPointerException { Properties(-1) }
}

interface InterfaceWithProperties {
  val readOnlyInterfaceProperty: Int
  var readWriteInterfaceProperty: Int
}

private fun testProperties_interfaceWithProperties() {
  class ImplementWithProperties : InterfaceWithProperties {
    override var readOnlyInterfaceProperty: Int = 1
    override var readWriteInterfaceProperty: Int = 2
  }
  val implementor = ImplementWithProperties()
  val i: InterfaceWithProperties = implementor

  assertTrue(1 == i.readOnlyInterfaceProperty)
  assertTrue(2 == i.readWriteInterfaceProperty)

  // Even though the property is exposed as read-only in the interface, the implementor might
  // expose it as a read-write.
  implementor.readOnlyInterfaceProperty = 3
  assertTrue(3 == i.readOnlyInterfaceProperty)
}

interface Holder<T> {
  var content: T
}

interface NumberHolder<T : Number> {
  var content: T
}

private fun testProperties_overrides() {
  open class Base : Holder<Int> {
    override var content: Int = 1
  }

  class OverridingImplementor : Base(), NumberHolder<Int> {
    override var content: Int = super.content + 1

    fun getSuperContent(): Int = super.content

    fun setSuperContent(n: Int) {
      super.content = n
    }
  }

  val implementor = OverridingImplementor()
  val holder: Holder<Int> = implementor
  val numberHolder: NumberHolder<Int> = implementor

  assertTrue(2 == implementor.content)
  assertTrue(2 == holder.content)
  assertTrue(2 == numberHolder.content)

  // Check the property on the super class, since it has a different backing field.
  assertTrue(1 == implementor.getSuperContent())

  // Change the backing field in the super type,
  implementor.setSuperContent(3)
  // ... check that it is setting it but
  assertTrue(3 == implementor.getSuperContent())
  // ... that the field in the subclass remains unaltered
  assertTrue(2 == implementor.content)

  // Finally set the propery using a super interfaces
  holder.content = 4
  assertTrue(4 == implementor.content)
  assertTrue(4 == holder.content)
  assertTrue(4 == numberHolder.content)
  // And test that it does not affect the backing field in the super class.
  assertTrue(3 == implementor.getSuperContent())
}

private fun testProperties_accidentalOverrides() {
  open class Base {
    var content: Int = 1
  }

  class Implementor : Base(), Holder<Int>, NumberHolder<Int> {}

  val implementor = Implementor()
  val holder: Holder<Int> = implementor
  val numberHolder: NumberHolder<Int> = implementor

  assertTrue(1 == implementor.content)
  assertTrue(1 == holder.content)
  assertTrue(1 == numberHolder.content)

  holder.content = 2
  assertTrue(2 == implementor.content)
  assertTrue(2 == holder.content)
  assertTrue(2 == numberHolder.content)
}
