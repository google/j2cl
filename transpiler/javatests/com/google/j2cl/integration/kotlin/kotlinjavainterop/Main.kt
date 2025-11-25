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
package kotlinjavainterop

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.assertUnderlyingTypeEquals
import com.google.j2cl.integration.testing.TestUtils
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

private class ExtendingJavaClass : JavaClass() {
  var fieldUsingSuper: Int
    get() = super.instanceField + 2
    set(value) {
      super.instanceField = value + 2
    }

  override fun instanceMethod(value: String): String {
    return "${super.instanceMethod(value)} delegated from ExtendingJavaClass.instanceMethod"
  }
}

/** Test kotlin calls java interop */
fun main(vararg unused: String) {
  testFieldAccess()
  testMethodCall()
  testMemberReferenceThroughSuper()
  testFakeOverrideSpecialization()
  testNullMarkedCode()
  testJsInterop()
}

fun testFieldAccess() {
  val javaClass = JavaClass()
  assertTrue(javaClass.instanceField == 1)
  javaClass.instanceField = 11
  assertTrue(javaClass.instanceField == 11)

  assertTrue(javaClass.privateFieldWithAccessors == 2)
  javaClass.privateFieldWithAccessors = 12
  assertEquals(12, javaClass.privateFieldWithAccessors)

  assertTrue(JavaClass.staticField == 0)
  JavaClass.staticField = 10
  assertTrue(JavaClass.staticField == 10)

  assertEquals(arrayOf("a", "b", "c"), NativeJavaClass.SOME_STRS)
}

fun testMethodCall() {
  assertEquals("JavaClass.staticMethod(foo)", JavaClass.staticMethod("foo"))
  assertEquals("JavaClass.instanceMethod(bar)", JavaClass().instanceMethod("bar"))
}

fun testMemberReferenceThroughSuper() {
  val extendingJavaClass = ExtendingJavaClass()

  assertEquals(3, extendingJavaClass.fieldUsingSuper)
  extendingJavaClass.fieldUsingSuper = 4
  assertTrue(extendingJavaClass.fieldUsingSuper == 8)
  assertEquals(
    "JavaClass.instanceMethod(baz) delegated from ExtendingJavaClass.instanceMethod",
    extendingJavaClass.instanceMethod("baz"),
  )
}

// Note that the <T> here collides with the <T> declared on JavaClass#specializedMethod
private class GenericExtendingClass<T> : JavaClass()

fun testFakeOverrideSpecialization() {
  val kotlinClass = GenericExtendingClass<Any>()

  // TODO(b/254148464): J2CL is more strict about erasure casts than JVM.
  if (!TestUtils.isJvm()) {
    assertThrowsClassCastException {
      val v: Any = kotlinClass.genericMethod<Any, Throwable, Any>(Any())
    }
  }
}

fun testNullMarkedCode() {
  assertUnderlyingTypeEquals(Int::class.javaObjectType, NullMarkedClass.getNonNullInteger())
  assertUnderlyingTypeEquals(Boolean::class.javaObjectType, NullMarkedClass.getNonNullBoolean())
  assertUnderlyingTypeEquals(Char::class.javaObjectType, NullMarkedClass.getNonNullCharacter())
  assertUnderlyingTypeEquals(Short::class.javaObjectType, NullMarkedClass.getNonNullShort())
  assertUnderlyingTypeEquals(Byte::class.javaObjectType, NullMarkedClass.getNonNullByte())
  assertUnderlyingTypeEquals(Long::class.javaObjectType, NullMarkedClass.getNonNullLong())
  assertUnderlyingTypeEquals(Double::class.javaObjectType, NullMarkedClass.getNonNullDouble())
  assertUnderlyingTypeEquals(Float::class.javaObjectType, NullMarkedClass.getNonNullFloat())
}

@JsType class KtJsType : SubJsTypeClass()

@JsType class KtJsTypeShadowedFieldInParent : JavaJsTypeShadowsField()

@JsType
class KtJsTypeShadowsField : SubJsTypeClass() {
  @JvmField @JsProperty(name = "foo2") var foo = "tuv"
}

class SpecializedPrimitiveParameters : ParametricJsTypeJavaClass<Double>() {
  override fun parametericJsMethod(value: Double): String? =
    "SpecializedPrimitiveParameters#parametericJsMethod"

  override fun boxedDoubleJsMethod(value: Double): String? =
    "SpecializedPrimitiveParameters#boxedDoubleJsMethod"
}

fun testJsInterop() {
  val ktJsType = KtJsType()

  assertEquals("abc", ktJsType.foo)
  assertEquals("get:abc", ktJsType.getFoo())

  ktJsType.setFoo("def")
  assertEquals("was_set:def", ktJsType.foo)
  assertEquals("get:was_set:def", ktJsType.getFoo())

  assertEquals("xyz", ktJsType.bar)
  assertEquals("get:xyz", ktJsType.getBar())

  ktJsType.setBar("uvw")
  assertEquals("was_set:uvw", ktJsType.bar)
  assertEquals("get:was_set:uvw", ktJsType.getBar())

  val shadowedFieldInParent = KtJsTypeShadowedFieldInParent()
  assertEquals("ijk", shadowedFieldInParent.foo)
  assertEquals("get:abc", shadowedFieldInParent.getFoo())
  shadowedFieldInParent.setFoo("def")
  assertEquals("ijk", shadowedFieldInParent.foo)
  assertEquals("get:was_set:def", shadowedFieldInParent.getFoo())

  val overridesField = KtJsTypeShadowsField()
  assertEquals("tuv", overridesField.foo)
  assertEquals("get:abc", overridesField.getFoo())
  overridesField.setFoo("def")
  assertEquals("tuv", overridesField.foo)
  assertEquals("get:was_set:def", overridesField.getFoo())

  // Use the supertype type to ensure correctness ofthe bridges.
  val specializesPrimitiveParameters: ParametricJsTypeJavaClass<Double> =
    SpecializedPrimitiveParameters()

  assertEquals(
    "SpecializedPrimitiveParameters#parametericJsMethod",
    specializesPrimitiveParameters.parametericJsMethod(1.0),
  )

  // TODO(b/463463843): Enable once the bug is fixed.
  // assertEquals(
  //   "SpecializedPrimitiveParameters#boxedDoubleJsMethod",
  //   specializesPrimitiveParameters.boxedDoubleJsMethod(1.0),
  // )
}
