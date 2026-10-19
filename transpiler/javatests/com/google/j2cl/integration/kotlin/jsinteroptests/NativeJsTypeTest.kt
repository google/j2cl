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
package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

/** Tests native JsType functionality. */
object NativeJsTypeTest {
  fun testAll() {
    testBridgesNativeSubclass()
    testAccessNamespaceAsProperty()
  }

  internal interface InterfaceWithDefaultMethods {
    fun method(): String

    fun defaultMethod(): String = "default-method"
  }

  private abstract class AbstractSuperWithPackagePrivateMethod
  @JsConstructor
  internal constructor() {
    internal open fun packagePrivateMethod() = "package-private"
  }

  @JsType(namespace = "woo.NativeJsTypeTest", name = "AbstractJavaSuperclass")
  private abstract class AbstractSuperClassForJs :
    AbstractSuperWithPackagePrivateMethod(), InterfaceWithDefaultMethods {
    public abstract override fun packagePrivateMethod(): String

    abstract override fun method(): String
  }

  fun testBridgesNativeSubclass() {
    val nativeSubclass: AbstractSuperClassForJs = createBridgesNativeSubclass()
    // Tests that the default method is implemented.
    assertEquals("default-method", nativeSubclass.defaultMethod())

    assertEquals("package-private-override", nativeSubclass.packagePrivateMethod())
    // Tests package private bridge.
    assertEquals(
      "package-private-override",
      ((nativeSubclass as Any) as AbstractSuperWithPackagePrivateMethod).packagePrivateMethod(),
    )

    assertEquals("method", nativeSubclass.method())
    // Tests JsMethod bridge.
    assertEquals("method", ((nativeSubclass as Any) as InterfaceWithDefaultMethods).method())
  }

  @JsMethod(namespace = "woo.NativeJsTypeTest.NativeBridgesSubclass", name = "create")
  @JvmStatic
  private external fun createBridgesNativeSubclass(): AbstractSuperClassForJs

  @JsProperty(namespace = "woo.NativeJsTypeTest.PropertyNamespace", name = "")
  @JvmStatic
  private external fun getPropertyNamespace(): String

  fun testAccessNamespaceAsProperty() {
    assertEquals("foo", getPropertyNamespace())
  }
}
