/*
 * Copyright 2026 Google Inc.
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
package morebridgemethods

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

object BridgeNativeReturnMain {
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object") class NativeObject

  open class Container<T> {
    open fun get(): T? = null
  }

  class NativeObjectContainer : Container<NativeObject>() {
    val value = NativeObject()

    // Specializes the return type of Container.get() to a native type.
    override fun get(): NativeObject = value
  }

  fun callGetByContainer(c: Container<*>): Any? = c.get()

  fun test() {
    val c = NativeObjectContainer()
    val container: Container<NativeObject> = c
    assertTrue(container.get() === c.value)
    assertTrue(callGetByContainer(c) === c.value)
  }
}
