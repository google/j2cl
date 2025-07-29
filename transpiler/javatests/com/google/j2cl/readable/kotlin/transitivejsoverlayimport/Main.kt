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
package transitivejsoverlayimport

import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
open internal class Immediate internal constructor() : Transitive() {

  @JsOverlay fun doImmediateInstanceMethod() {}
}

open internal class NonNativeUpper @JsConstructor constructor() : Immediate() {
  fun doNonNativeUpperInstanceMethod() {}
}

open internal class NonNativeLower @JsConstructor constructor() : NonNativeUpper() {
  fun doNonNativeLowerInstanceMethod() {}
}

@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
open class Transitive {

  @JsProperty(name = "constructor") external fun getJsProperty(): Any

  @JsOverlay fun doTransitiveInstanceMethod(arg1: String?) {}
}

fun main() {
  val transitive = Transitive()
  transitive.doTransitiveInstanceMethod("arg1")
  transitive.getJsProperty()

  val immediate = Immediate()
  immediate.doTransitiveInstanceMethod("arg1")
  immediate.getJsProperty()
  immediate.doImmediateInstanceMethod()

  val nonNativeUpper = NonNativeUpper()
  nonNativeUpper.doTransitiveInstanceMethod("arg1")
  nonNativeUpper.getJsProperty()
  nonNativeUpper.doImmediateInstanceMethod()
  nonNativeUpper.doNonNativeUpperInstanceMethod()

  val nonNativeLower = NonNativeLower()
  nonNativeLower.doTransitiveInstanceMethod("arg1")
  nonNativeLower.getJsProperty()
  nonNativeLower.doImmediateInstanceMethod()
  nonNativeLower.doNonNativeUpperInstanceMethod()
  nonNativeLower.doNonNativeLowerInstanceMethod()
}
