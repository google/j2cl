/*
 * Copyright 2017 Google Inc.
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
@file:Suppress("KotlinConstantConditions")

package nativejstypes

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

fun testNativeJsTypeWithNamespace() {
  val foo = Foo()
  assertTrue(foo.sum() == 42)
  foo.x = 50
  foo.y = 5
  assertTrue(foo.sum() == 55)
}

fun testNativeJsTypeWithoutNamespace() {
  val bar = Bar(6, 7)
  assertTrue(bar.product() == 42)
  bar.x = 50
  bar.y = 5
  assertTrue(bar.product() == 250)
  Bar.f = 10
  assertTrue(Bar.f == 10)
}

fun testGlobalNativeJsType() {
  val number22 = Number(2.2)
  val number10base2 = Number.parseInt("10", 2)
  assertTrue(number22.toFixed().equals(number10base2.toString()))
}

fun testNativeEquality() {
  val n1 = Number(1.0)
  val n2 = Number(1.0)
  assertTrue(n1 === n1)
  assertTrue(n1 !== n2)
  assertTrue(n1 !== null)

  val n3 = getUndefined()
  assertTrue(n3 === null)
  assertTrue(n3 !== n1)

  val o1: Any? = n1
  val o2: Any? = n2
  assertTrue(n1 === o1)
  assertTrue(o1 === n1)
  assertTrue(n1 !== o2)
  assertTrue(o2 !== n1)
  assertTrue(n3 === (null as Any?))
  assertTrue(n1 !== Any())

  // Comparisons involving a native type use JavaScript semantics.
  val hello = ("hello" as Any) as Wildcard
  assertTrue(hello === ("hello" as Any))
  val onePointFive = (1.5 as Any) as Wildcard
  assertTrue(onePointFive === (1.5 as Any))
  val boxedTrue: Any = true
  val trueValue = boxedTrue as Wildcard
  assertTrue(trueValue === (true as Any))

  // In Wasm, each conversion of a native object to Any? creates a new wrapper instance
  // (b/540448377), and === between two Any? references compares the wrappers rather than
  // unwrapping to the underlying JS objects.
  // assertTrue((n1 as Any?) === o1)
}

@JsProperty(namespace = JsPackage.GLOBAL) private external fun getUndefined(): Number

@JsType(isNative = true, namespace = "test.foo") internal interface MyNativeJsTypeInterface {}

@JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
internal open class HTMLElementConcreteNativeJsType {}

@JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
internal class HTMLElementAnotherConcreteNativeJsType {}

private open class NonNativeClass

private class IntersectionImpl : NonNativeClass(), MyNativeJsTypeInterface

private fun <
  NI : MyNativeJsTypeInterface,
  NC : HTMLElementConcreteNativeJsType,
  M : NonNativeClass,
  IT,
> testCasts() where IT : NonNativeClass, IT : MyNativeJsTypeInterface {
  var myClass: Any?
  myClass = createFoo() as ElementLikeNativeInterface
  assertNotNull(myClass)
  myClass = createFoo() as MyNativeJsTypeInterface
  assertNotNull(myClass)
  myClass = createFoo() as NI
  assertNotNull(myClass)
  myClass = createNativeButton() as HTMLElementConcreteNativeJsType
  assertNotNull(myClass)
  myClass = createNativeButton() as NC
  assertNotNull(myClass)

  assertThrowsClassCastException {
    val unused = createFoo() as HTMLElementConcreteNativeJsType
  }

  // Test cross cast for native types
  val nativeButton1: Any? = createNativeButton() as HTMLElementConcreteNativeJsType
  val nativeButton2: Any? = nativeButton1 as HTMLElementAnotherConcreteNativeJsType

  val nonNative = NonNativeClass()
  val nativeNonNative = nonNative as MyNativeJsTypeInterface
  assertTrue(nativeNonNative as NonNativeClass === nonNative)
  assertTrue(nativeNonNative as M === nonNative)
  assertTrue(nativeNonNative === nonNative)
  assertTrue(passThrough(nativeNonNative) === nonNative)
  if (nativeNonNative is NonNativeClass) {
    assertTrue(nativeNonNative === nonNative)
  }
  assertTrue(passThroughWithNativeBound(nonNative as IT) === nonNative)
  assertTrue(passThroughWithNativeBound(nonNative as IT) !== NonNativeClass())
  val genericNativeNonNative = nonNative as NI
  assertTrue(genericNativeNonNative as NonNativeClass === nonNative)
  assertTrue(genericNativeNonNative as M === nonNative)
  assertTrue(genericNativeNonNative === nonNative)
  assertTrue(passThrough(genericNativeNonNative) === nonNative)
  if (genericNativeNonNative is NonNativeClass) {
    assertTrue(genericNativeNonNative === nonNative)
  }

  val foo = passThrough(Foo())
  assertTrue(foo.sum() == 42)
  val foos = passThrough(arrayOf(Foo()))
  assertTrue(foos[0].sum() == 42)

  /*
   * If the optimizations are turned on, it is possible for the compiler to dead-strip the
   * variables since they are not used. Therefore the casts could potentially be stripped.
   */
  assertNotNull(myClass)
  assertNotNull(nativeButton1)
  assertNotNull(nativeButton2)
}

private fun <T> passThrough(t: T): T = t

// Returns an expression with a native declared type and a potentially non-native inferred type.
private fun <T : MyNativeJsTypeInterface> passThroughWithNativeBound(t: T): T = t

private fun createFoo(): Any? = Foo()

@JsMethod(namespace = "nativejstypes.helper") external fun createNativeButton(): Any?

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*") interface Star

private fun testStar() {
  var star = Any() as Star
  assertNotNull(star)

  star = (3.0 as Any) as Star
  assertNotNull(star)
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?") interface Wildcard

private fun testWildcard() {
  var wildcard = Any() as Wildcard
  assertNotNull(wildcard)

  wildcard = (3.0 as Any) as Wildcard
  assertNotNull(wildcard)
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
fun interface NativeFunctionalInterface<T> {
  fun f(t: T): Int
}

private fun testNativeFunctionalInterface() {
  val nativeFunctionalInterface = NativeFunctionalInterface<String> { s -> 10 }
  assertEquals(10, nativeFunctionalInterface.f(""))
}

fun main(vararg unused: String) {
  testNativeJsTypeWithNamespace()
  testNativeJsTypeWithoutNamespace()
  testGlobalNativeJsType()
  testNativeEquality()
  testCasts<
    MyNativeJsTypeInterface,
    HTMLElementConcreteNativeJsType,
    NonNativeClass,
    IntersectionImpl,
  >()
  testStar()
  testWildcard()
  testNativeFunctionalInterface()
}
