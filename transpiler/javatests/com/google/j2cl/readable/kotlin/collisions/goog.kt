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
package collisions

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

class goog

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
class Blah

@JsMethod(namespace = JsPackage.GLOBAL, name = "Math.random")
external fun m()


@JsProperty(name = "String.prototype.length", namespace = JsPackage.GLOBAL)
external fun getN(): Double

class foo

class bar {
  init {
    var foo: Int
    var bar: Int
    var goog: Int
    var flip: Int
    var window: Int
    m()
    getN()
    Blah()
    goog()
    foo()

    // Variable colliding with reserved JavaScript names.
    val delete = 1
    val undefined = 3
  }
}

class Other<T> {
  init {
    val T = 3
    val o = null as Other<T>?
  }

  fun <T> m(): T? {
    val T = 5
    val o = null as Other<T>?
    return null
  }
}

class T<T : Number?> {
  fun m(): T? {
    return null
  }

  fun n(): T? {
    val t: T? = collisions.T<T>().m()
    t!!.toByte()
    return t
  }
}
