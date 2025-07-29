/*
 * Copyright 2023 Google Inc.
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
package delegatedproperties

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import kotlin.reflect.KProperty

fun main(vararg unused: String) {
  testDelegation()
  testProvideDelegateMethod()
  testDelegateWithExtensionFunction()
  testPropertyTypeInferenceThroughDelegate()
  testDelegationToPropertyReference()
  testDelegationWithGenericType()
  testLocalPropertyDelegation()
}

interface WithId {
  val id: String
}

var initCounter = 0

class PropertyDelegatedToAnObject(override val id: String) : WithId {
  val initCounterBefore = initCounter
  var delegatedProperty: String by DelegateObject()
  val initCounterAfter = initCounter
}

var PropertyDelegatedToAnObject.delegatedExtensionProperty: String by DelegateObject()

var delegatedtopLevelProperty: String by DelegateObject()

open class DelegateObject(val noQualifierPrefix: String = "topLevel") {
  init {
    initCounter++
  }

  private var value: String? = null

  open operator fun getValue(thisRef: WithId?, property: KProperty<*>): String {
    val internalValue = value ?: "<not-set>"
    return "${thisRef?.id ?: noQualifierPrefix}-$internalValue"
  }

  open operator fun setValue(thisRef: WithId?, property: KProperty<*>, s: String) {
    value = s
  }
}

fun testDelegation() {
  // we expect to have initialized two DelegateObject for the top level property and the extension
  // property.
  assertTrue(initCounter == 2)

  val a = PropertyDelegatedToAnObject("a")
  assertTrue(initCounter == 3)
  assertTrue(a.initCounterBefore == 2)
  assertTrue(a.initCounterAfter == 3)

  assertEquals("a-<not-set>", a.delegatedProperty)
  a.delegatedProperty = "value"
  assertEquals("a-value", a.delegatedProperty)

  assertEquals("a-<not-set>", a.delegatedExtensionProperty)
  a.delegatedExtensionProperty = "value0"
  assertEquals("a-value0", a.delegatedExtensionProperty)

  assertEquals("topLevel-<not-set>", delegatedtopLevelProperty)
  delegatedtopLevelProperty = "value1"
  assertEquals("topLevel-value1", delegatedtopLevelProperty)
}

class DelegateProvider {
  operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): DelegateObject =
    DelegateObject()
}

class UsingDelegateProvider(override val id: String) : WithId {
  var property: String by DelegateProvider()
}

fun testProvideDelegateMethod() {
  val a = UsingDelegateProvider("propertyDelegate")

  assertEquals("propertyDelegate-<not-set>", a.property)
  a.property = "value0"
  assertEquals("propertyDelegate-value0", a.property)
}

class DelegateTroughExtensionFunction(var value: String)

// We intentionally marked the getValue() function as inline function to test that property
// delegation works with the inliner.
@Suppress("NOTHING_TO_INLINE")
operator inline fun DelegateTroughExtensionFunction.getValue(
  thisRef: Any?,
  property: KProperty<*>,
): String {
  return "From DelegateTroughExtensionFunction: $value"
}

operator fun DelegateTroughExtensionFunction.setValue(
  thisRef: Any?,
  property: KProperty<*>,
  s: String,
) {
  value = s
}

class UsingDelegateTroughExtensionFunction {
  var property by DelegateTroughExtensionFunction("<default>")
}

fun testDelegateWithExtensionFunction() {
  val a = UsingDelegateTroughExtensionFunction()

  assertEquals("From DelegateTroughExtensionFunction: <default>", a.property)
  a.property = "value"
  assertEquals("From DelegateTroughExtensionFunction: value", a.property)
}

class DelegatedPropertyTypeInference(override val id: String) : WithId {
  var property by DelegateObject()
}

fun testPropertyTypeInferenceThroughDelegate() {
  val a = DelegatedPropertyTypeInference("a")

  assertEquals("a-<not-set>", a.property)
  a.property = "value"
  assertEquals("a-value", a.property)
}

var topLevelProperty: String = "topLevelProperty"

class SimpleClassWithOneProperty(var property: String = "otherClassReadOnlyProperty")

class DelegationToPropertyReference(simpleClassWithOneProperty: SimpleClassWithOneProperty) {
  var delegatedToTopLevelProperty: String by ::topLevelProperty

  var stringProperty = "sameClassProperty"
  var delegatedToAnotherSameClassProperty: String by this::stringProperty

  var delegatedToAnotherClassProperty by simpleClassWithOneProperty::property
}

fun testDelegationToPropertyReference() {
  // Property references are represented by objects implementing the KMutableProperty interface.
  // The delegation is done trough inline extension functions that call the getter and setter of the
  // referenced property. Strictly speaking this case is already covered by the previous test. We
  // still want to test that our implementation of KMutableProperty still work in the context of
  // a delegated property.
  val simpleClassWithOneProperty = SimpleClassWithOneProperty()
  val a = DelegationToPropertyReference(simpleClassWithOneProperty)

  assertEquals("topLevelProperty", topLevelProperty)
  assertEquals("topLevelProperty", a.delegatedToTopLevelProperty)
  a.delegatedToTopLevelProperty = "value0"
  assertEquals("value0", a.delegatedToTopLevelProperty)
  assertEquals("value0", topLevelProperty)

  assertEquals("sameClassProperty", a.stringProperty)
  assertEquals("sameClassProperty", a.delegatedToAnotherSameClassProperty)
  a.delegatedToAnotherSameClassProperty = "value1"
  assertEquals("value1", a.delegatedToAnotherSameClassProperty)
  assertEquals("value1", a.stringProperty)

  assertEquals("otherClassReadOnlyProperty", simpleClassWithOneProperty.property)
  assertEquals("otherClassReadOnlyProperty", a.delegatedToAnotherClassProperty)
  a.delegatedToAnotherClassProperty = "value3"
  assertEquals("value3", a.delegatedToAnotherClassProperty)
  assertEquals("value3", simpleClassWithOneProperty.property)
}

class WithGenerics<T> {
  var delegatedProperty: T? by DelegateClassWithGeneric<T>()
}

class DelegateClassWithGeneric<T> {
  var value: T? = null

  operator fun getValue(aThis: WithGenerics<T>, property: KProperty<*>): T? {
    return value
  }

  operator fun setValue(withGenerics: WithGenerics<T>, property: KProperty<*>, t: T?) {
    value = t
  }
}

fun testDelegationWithGenericType() {
  val withGeneric: WithGenerics<String> = WithGenerics()

  assertEquals(null, withGeneric.delegatedProperty)

  withGeneric.delegatedProperty = "valueSetFromOwnerClass"
  assertEquals("valueSetFromOwnerClass", withGeneric.delegatedProperty)
}

fun testLocalPropertyDelegation() {
  var localProperty: String by DelegateObject("local")

  assertEquals("local-<not-set>", localProperty)
  localProperty = "value"
  assertEquals("local-value", localProperty)
}
