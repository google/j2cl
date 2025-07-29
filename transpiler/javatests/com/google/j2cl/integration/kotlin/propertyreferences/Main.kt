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
package propertyreferences

import com.google.j2cl.integration.testing.Asserts.assertEquals

fun main(vararg unused: String) {
  testValuePropertyReferences()
  Child().testValuePropertyReferenceThroughThis()
  testTypePropertyReferences()
  testTypePropertyReferencesTroughSuperTypes()
  testTopLevelPropertyReferences()
  testExtensionPropertyReferences()
  testPropertyReferenceFromObjectExpression()
  testPropertyReferenceFromObjectDeclarations()
  testGenericPropertyReferences()
  testNullablePropertReferences()
}

interface InterfaceWithProperty {
  var interfaceProperty: String
}

open class Parent(
  var parentMutableProp: String = "parentMutablePropInitValue",
  val parentImmutableProp: String = "parentImmutablePropInitValue",
)

class Child : Parent(), InterfaceWithProperty {
  override var interfaceProperty: String = "interfacePropertyInitValue"

  var mutableProp: String = "childMutablePropInitValue"
  val immutableProp: String = "childImmutablePropInitValue"

  var customAccessorsMutableProp: String = "initValue"
    get() = "customGet:$field"
    set(value) {
      field = "customSet:$value"
    }

  private var getterCallCounter = 0
  val customGetterImmutableProp: String
    get() = "#calls:${getterCallCounter++}"

  fun testValuePropertyReferenceThroughThis() {
    // `this` qualifier is optional.
    assertEquals("childMutablePropInitValue", this::mutableProp.get())
    assertEquals("childMutablePropInitValue", ::mutableProp.get())
    assertEquals("childImmutablePropInitValue", this::immutableProp.get())
    assertEquals("childImmutablePropInitValue", ::immutableProp.get())
    assertEquals("customGet:initValue", this::customAccessorsMutableProp.get())
    assertEquals("customGet:initValue", ::customAccessorsMutableProp.get())
    assertEquals("#calls:0", this::customGetterImmutableProp.get())
    assertEquals("#calls:1", ::customGetterImmutableProp.get())
    assertEquals("interfacePropertyInitValue", this::interfaceProperty.get())
    assertEquals("interfacePropertyInitValue", ::interfaceProperty.get())
    assertEquals("parentMutablePropInitValue", this::parentMutableProp.get())
    assertEquals("parentMutablePropInitValue", ::parentMutableProp.get())
    assertEquals("parentImmutablePropInitValue", this::parentImmutableProp.get())
    assertEquals("parentImmutablePropInitValue", ::parentImmutableProp.get())

    this::mutableProp.set("childMutablePropNewValue")
    assertEquals("childMutablePropNewValue", mutableProp)
    ::mutableProp.set("childMutablePropNewValueWithoutThis")
    assertEquals("childMutablePropNewValueWithoutThis", mutableProp)
    this::customAccessorsMutableProp.set("newValue")
    assertEquals("customGet:customSet:newValue", customAccessorsMutableProp)
    ::customAccessorsMutableProp.set("newValueWithoutThis")
    assertEquals("customGet:customSet:newValueWithoutThis", customAccessorsMutableProp)
    this::interfaceProperty.set("interfacePropertyNewValue")
    assertEquals("interfacePropertyNewValue", interfaceProperty)
    ::interfaceProperty.set("interfacePropertyNewValueWithoutThis")
    assertEquals("interfacePropertyNewValueWithoutThis", interfaceProperty)
    this::parentMutableProp.set("parentMutablePropNewValue")
    assertEquals("parentMutablePropNewValue", parentMutableProp)
    ::parentMutableProp.set("parentMutablePropNewValueWithoutThis")
    assertEquals("parentMutablePropNewValueWithoutThis", parentMutableProp)
  }
}

fun testValuePropertyReferences() {
  val aChild = Child()

  assertEquals("childMutablePropInitValue", aChild::mutableProp.get())
  assertEquals("childImmutablePropInitValue", aChild::immutableProp.get())
  assertEquals("customGet:initValue", aChild::customAccessorsMutableProp.get())
  assertEquals("#calls:0", aChild::customGetterImmutableProp.get())
  assertEquals("#calls:1", aChild::customGetterImmutableProp.get())
  assertEquals("interfacePropertyInitValue", aChild::interfaceProperty.get())
  assertEquals("parentMutablePropInitValue", aChild::parentMutableProp.get())
  assertEquals("parentImmutablePropInitValue", aChild::parentImmutableProp.get())

  aChild::mutableProp.set("childMutablePropNewValue")
  assertEquals("childMutablePropNewValue", aChild.mutableProp)
  aChild::customAccessorsMutableProp.set("newValue")
  assertEquals("customGet:customSet:newValue", aChild.customAccessorsMutableProp)
  aChild::interfaceProperty.set("interfacePropertyNewValue")
  assertEquals("interfacePropertyNewValue", aChild.interfaceProperty)
  aChild::parentMutableProp.set("parentMutablePropNewValue")
  assertEquals("parentMutablePropNewValue", aChild.parentMutableProp)

  // property reference can be invoked as a function.
  assertEquals("childMutablePropNewValue", (aChild::mutableProp)())
  assertEquals("childImmutablePropInitValue", (aChild::immutableProp)())
  assertEquals("customGet:customSet:newValue", (aChild::customAccessorsMutableProp)())
  assertEquals("#calls:2", (aChild::customGetterImmutableProp)())
  assertEquals("interfacePropertyNewValue", (aChild::interfaceProperty)())
  assertEquals("parentMutablePropNewValue", (aChild::parentMutableProp)())
  assertEquals("parentImmutablePropInitValue", (aChild::parentImmutableProp)())
}

fun testTypePropertyReferences() {
  val aChild = Child()
  val anotherChild = Child()

  assertEquals("childMutablePropInitValue", Child::mutableProp.get(aChild))
  assertEquals("childImmutablePropInitValue", Child::immutableProp.get(aChild))
  assertEquals("customGet:initValue", Child::customAccessorsMutableProp.get(aChild))
  assertEquals("#calls:0", Child::customGetterImmutableProp.get(aChild))
  assertEquals("#calls:1", Child::customGetterImmutableProp.get(aChild))
  assertEquals("#calls:0", Child::customGetterImmutableProp.get(anotherChild))
  assertEquals("interfacePropertyInitValue", Child::interfaceProperty.get(aChild))
  assertEquals("parentMutablePropInitValue", Child::parentMutableProp.get(aChild))
  assertEquals("parentImmutablePropInitValue", Child::parentImmutableProp.get(aChild))

  Child::mutableProp.set(aChild, "childMutablePropNewValue")
  assertEquals("childMutablePropNewValue", aChild.mutableProp)
  Child::customAccessorsMutableProp.set(aChild, "newValue")
  assertEquals("customGet:customSet:newValue", aChild.customAccessorsMutableProp)
  Child::interfaceProperty.set(aChild, "interfacePropertyNewValue")
  assertEquals("interfacePropertyNewValue", aChild.interfaceProperty)

  Child::parentMutableProp.set(aChild, "parentMutablePropNewValue")
  assertEquals("parentMutablePropNewValue", aChild.parentMutableProp)

  // property reference can be invoked as a function.
  assertEquals("childMutablePropNewValue", (Child::mutableProp)(aChild))
  assertEquals("childImmutablePropInitValue", (Child::immutableProp)(aChild))
  assertEquals("customGet:customSet:newValue", (Child::customAccessorsMutableProp)(aChild))
  assertEquals("#calls:2", (Child::customGetterImmutableProp)(aChild))
  assertEquals("interfacePropertyNewValue", (Child::interfaceProperty)(aChild))
  assertEquals("parentMutablePropNewValue", (Child::parentMutableProp)(aChild))
  assertEquals("parentImmutablePropInitValue", (Child::parentImmutableProp)(aChild))
}

fun testTypePropertyReferencesTroughSuperTypes() {
  val aChild = Child()

  assertEquals("interfacePropertyInitValue", InterfaceWithProperty::interfaceProperty.get(aChild))
  assertEquals("parentMutablePropInitValue", Parent::parentMutableProp.get(aChild))
  assertEquals("parentImmutablePropInitValue", Parent::parentImmutableProp.get(aChild))

  InterfaceWithProperty::interfaceProperty.set(aChild, "interfacePropertyNewValue")
  assertEquals("interfacePropertyNewValue", aChild.interfaceProperty)
  Parent::parentMutableProp.set(aChild, "parentMutablePropNewValue")
  assertEquals("parentMutablePropNewValue", aChild.parentMutableProp)
}

var mutableTopLevelProperty = "mutableTopLevelPropertyInitValue"
// TODO(b/271186580): turn this property to a const val property.
val immutableTopLevelProperty = "immutableTopLevelPropertyInitValue"
var customAccessorsMutableTopLevelProperty: String = "initValue"
  get() = "customGet:$field"
  set(value) {
    field = "customSet:$value"
  }

var topLevelGetterCallCounter = 0
val customGetterImmutableTopLevelProperty: String
  get() = "#calls:${topLevelGetterCallCounter++}"

fun testTopLevelPropertyReferences() {
  assertEquals("mutableTopLevelPropertyInitValue", ::mutableTopLevelProperty.get())
  assertEquals("immutableTopLevelPropertyInitValue", ::immutableTopLevelProperty.get())
  assertEquals("customGet:initValue", ::customAccessorsMutableTopLevelProperty.get())
  assertEquals("#calls:0", ::customGetterImmutableTopLevelProperty.get())
  assertEquals("#calls:1", ::customGetterImmutableTopLevelProperty.get())

  ::mutableTopLevelProperty.set("mutableTopLevelPropertyNewValue")
  assertEquals("mutableTopLevelPropertyNewValue", mutableTopLevelProperty)
  ::customAccessorsMutableTopLevelProperty.set("newValue")
  assertEquals("customGet:customSet:newValue", customAccessorsMutableTopLevelProperty)

  // property reference can be invoked as a function.
  assertEquals("mutableTopLevelPropertyNewValue", (::mutableTopLevelProperty)())
  assertEquals("immutableTopLevelPropertyInitValue", (::immutableTopLevelProperty)())
  assertEquals("customGet:customSet:newValue", (::customAccessorsMutableTopLevelProperty)())
  assertEquals("#calls:2", (::customGetterImmutableTopLevelProperty)())
}

var Parent.parentExtensionProperty: String
  get() = "Extension:$parentMutableProp"
  set(value) {
    parentMutableProp = value
  }

var Child.childExtensionMutableProperty: String
  get() = "Extension:$mutableProp"
  set(value) {
    mutableProp = value
  }

val Child.childExtensionImmutableProperty: String
  get() = "Extension:$immutableProp"

fun testExtensionPropertyReferences() {
  var aChild = Child()

  assertEquals("Extension:parentMutablePropInitValue", aChild::parentExtensionProperty.get())
  assertEquals("Extension:childMutablePropInitValue", aChild::childExtensionMutableProperty.get())
  assertEquals(
    "Extension:childImmutablePropInitValue",
    aChild::childExtensionImmutableProperty.get(),
  )
  assertEquals("Extension:parentMutablePropInitValue", Child::parentExtensionProperty.get(aChild))
  assertEquals("Extension:parentMutablePropInitValue", Parent::parentExtensionProperty.get(aChild))
  assertEquals(
    "Extension:childMutablePropInitValue",
    Child::childExtensionMutableProperty.get(aChild),
  )
  assertEquals(
    "Extension:childImmutablePropInitValue",
    Child::childExtensionImmutableProperty.get(aChild),
  )

  aChild::parentExtensionProperty.set("parentMutablePropNewValue")
  assertEquals("Extension:parentMutablePropNewValue", aChild.parentExtensionProperty)
  aChild::childExtensionMutableProperty.set("childMutablePropNewValue")
  assertEquals("Extension:childMutablePropNewValue", aChild.childExtensionMutableProperty)

  Child::parentExtensionProperty.set(aChild, "parentMutablePropNewValueFromChildType")
  assertEquals("Extension:parentMutablePropNewValueFromChildType", aChild.parentExtensionProperty)
  Parent::parentExtensionProperty.set(aChild, "parentMutablePropNewValueFromParentType")
  assertEquals("Extension:parentMutablePropNewValueFromParentType", aChild.parentExtensionProperty)
  Child::childExtensionMutableProperty.set(aChild, "childMutablePropNewValueFromChildType")
  assertEquals(
    "Extension:childMutablePropNewValueFromChildType",
    aChild.childExtensionMutableProperty,
  )

  aChild = Child()
  // property reference can be invoked as a function.
  assertEquals("Extension:parentMutablePropInitValue", (aChild::parentExtensionProperty)())
  assertEquals("Extension:childMutablePropInitValue", (aChild::childExtensionMutableProperty)())
  assertEquals("Extension:childImmutablePropInitValue", (aChild::childExtensionImmutableProperty)())
  assertEquals("Extension:parentMutablePropInitValue", (Child::parentExtensionProperty)(aChild))
  assertEquals("Extension:parentMutablePropInitValue", (Parent::parentExtensionProperty)(aChild))
  assertEquals(
    "Extension:childMutablePropInitValue",
    (Child::childExtensionMutableProperty)(aChild),
  )
  assertEquals(
    "Extension:childImmutablePropInitValue",
    (Child::childExtensionImmutableProperty)(aChild),
  )
}

fun testPropertyReferenceFromObjectExpression() {
  val mutablePropertyReference =
    object {
      var mutableProperty = "mutablePropertyInitValue"
    }::mutableProperty

  assertEquals("mutablePropertyInitValue", mutablePropertyReference.get())
  mutablePropertyReference.set("mutablePropertyNewValue")
  assertEquals("mutablePropertyNewValue", mutablePropertyReference.get())
  assertEquals("mutablePropertyNewValue", mutablePropertyReference())
}

class WithCompanionObject {
  val unused = ""

  companion object {
    var mutablePropertyOnCompanion = "mutablePropertyOnCompanionInitValue"
  }
}

object ObjectDeclaration {
  var mutableProperty = "mutablePropertyInitValue"
}

fun testPropertyReferenceFromObjectDeclarations() {
  assertEquals(
    "mutablePropertyOnCompanionInitValue",
    WithCompanionObject.Companion::mutablePropertyOnCompanion.get(),
  )
  WithCompanionObject.Companion::mutablePropertyOnCompanion.set(
    "mutablePropertyOnCompanionNewValue"
  )
  assertEquals(
    "mutablePropertyOnCompanionNewValue",
    WithCompanionObject.Companion::mutablePropertyOnCompanion.get(),
  )
  assertEquals(
    "mutablePropertyOnCompanionNewValue",
    (WithCompanionObject.Companion::mutablePropertyOnCompanion)(),
  )

  assertEquals("mutablePropertyInitValue", ObjectDeclaration::mutableProperty.get())
  ObjectDeclaration::mutableProperty.set("mutablePropertyNewValue")
  assertEquals("mutablePropertyNewValue", ObjectDeclaration.mutableProperty)
  assertEquals("mutablePropertyNewValue", (ObjectDeclaration::mutableProperty)())
}

open class ParentWithGenerics<T, V>(var parentGenericTProperty: T, var parentGenericVProperty: V)

class WithGenerics<T>(var childGenericProperty: T, parentGenericTPropertyValue: T) :
  ParentWithGenerics<T, String>(parentGenericTPropertyValue, "parentGenericVPropertyInitValue")

fun testGenericPropertyReferences() {
  val withGenerics =
    WithGenerics("childGenericPropertyInitValue", "parentGenericTPropertyInitValue")

  assertEquals("childGenericPropertyInitValue", withGenerics::childGenericProperty.get())
  assertEquals("parentGenericTPropertyInitValue", withGenerics::parentGenericTProperty.get())
  assertEquals("parentGenericVPropertyInitValue", withGenerics::parentGenericVProperty.get())
  assertEquals(
    "childGenericPropertyInitValue",
    WithGenerics<String>::childGenericProperty.get(withGenerics),
  )
  assertEquals(
    "parentGenericTPropertyInitValue",
    WithGenerics<String>::parentGenericTProperty.get(withGenerics),
  )
  assertEquals(
    "parentGenericVPropertyInitValue",
    WithGenerics<String>::parentGenericVProperty.get(withGenerics),
  )
  assertEquals(
    "childGenericPropertyInitValue",
    WithGenerics<*>::childGenericProperty.get(withGenerics),
  )
  assertEquals(
    "parentGenericTPropertyInitValue",
    WithGenerics<*>::parentGenericTProperty.get(withGenerics),
  )
  assertEquals(
    "parentGenericVPropertyInitValue",
    WithGenerics<*>::parentGenericVProperty.get(withGenerics),
  )
  assertEquals(
    "parentGenericTPropertyInitValue",
    ParentWithGenerics<*, *>::parentGenericTProperty.get(withGenerics),
  )
  assertEquals(
    "parentGenericVPropertyInitValue",
    ParentWithGenerics<*, *>::parentGenericVProperty.get(withGenerics),
  )

  withGenerics::childGenericProperty.set("childGenericPropertyNewValue")
  assertEquals("childGenericPropertyNewValue", withGenerics.childGenericProperty)
  withGenerics::parentGenericTProperty.set("parentGenericTPropertyNewValue")
  assertEquals("parentGenericTPropertyNewValue", withGenerics.parentGenericTProperty)
  withGenerics::parentGenericVProperty.set("parentGenericVPropertyNewValue")
  assertEquals("parentGenericVPropertyNewValue", withGenerics.parentGenericVProperty)
  WithGenerics<String>::childGenericProperty.set(
    withGenerics,
    "childGenericPropertyNewValueFromChild<String>",
  )
  assertEquals("childGenericPropertyNewValueFromChild<String>", withGenerics.childGenericProperty)
  WithGenerics<String>::parentGenericTProperty.set(
    withGenerics,
    "parentGenericTPropertyNewValueFromChild<String>",
  )
  assertEquals(
    "parentGenericTPropertyNewValueFromChild<String>",
    withGenerics.parentGenericTProperty,
  )
  WithGenerics<String>::parentGenericVProperty.set(
    withGenerics,
    "parentGenericVPropertyNewValueFromChild<String>",
  )
  assertEquals(
    "parentGenericVPropertyNewValueFromChild<String>",
    withGenerics.parentGenericVProperty,
  )
  WithGenerics<*>::childGenericProperty.set(
    withGenerics,
    "childGenericPropertyNewValueFromChild<*>",
  )
  assertEquals("childGenericPropertyNewValueFromChild<*>", withGenerics.childGenericProperty)
  WithGenerics<*>::parentGenericTProperty.set(
    withGenerics,
    "parentGenericTPropertyNewValueFromChild<*>",
  )
  assertEquals("parentGenericTPropertyNewValueFromChild<*>", withGenerics.parentGenericTProperty)
  WithGenerics<*>::parentGenericVProperty.set(
    withGenerics,
    "parentGenericVPropertyNewValueFromChild<*>",
  )
  assertEquals("parentGenericVPropertyNewValueFromChild<*>", withGenerics.parentGenericVProperty)
  ParentWithGenerics<*, *>::parentGenericTProperty.set(
    withGenerics,
    "parentGenericTPropertyNewValueFromParent<*,*>",
  )
  assertEquals("parentGenericTPropertyNewValueFromParent<*,*>", withGenerics.parentGenericTProperty)
  ParentWithGenerics<*, *>::parentGenericVProperty.set(
    withGenerics,
    "parentGenericVPropertyNewValueFromParent<*,*>",
  )
  assertEquals("parentGenericVPropertyNewValueFromParent<*,*>", withGenerics.parentGenericVProperty)
}

class WithNullableProperty {
  var nullableProperty: String? = null
}

fun testNullablePropertReferences() {
  val withNullableProperty = WithNullableProperty()

  assertEquals(null, withNullableProperty::nullableProperty.get())
  assertEquals(null, WithNullableProperty::nullableProperty.get(withNullableProperty))
  withNullableProperty::nullableProperty.set("notNull")
  assertEquals("notNull", withNullableProperty.nullableProperty)
  WithNullableProperty::nullableProperty.set(withNullableProperty, null)
  assertEquals(null, withNullableProperty.nullableProperty)
}
