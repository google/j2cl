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
package dataclass

import jsinterop.annotations.JsIgnore
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@JsType data class JsTypeDataClass(val foo: Int, val bar: Int = 10)

@JsType data class JsTypeDataClassWithPrivateProperty(private val foo: Int)

@JsType data class JsTypeDataClassWithJsIgnore(@JsIgnore val foo: Int)

data class DataClassWithJsProperty(@JsProperty(name = "renamedFoo") val foo: Int)

fun main() {
  val foo1 = JsTypeDataClass(1).foo
  val (foo2) = JsTypeDataClassWithJsIgnore(1)
  val foo3 = DataClassWithJsProperty(1).foo
  val (foo4) = DataClassWithJsProperty(1)
}
