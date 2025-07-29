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
package objectclass

object A {
  fun add(i: Int): Int = i + 1

  var x = 1

  init {
    x += 2
  }

  val y = x

  var z: Int
    get() = x
    set(value) {
      x += value
    }
}

interface InterfaceType {
  fun doSomething(): String
}

object B : InterfaceType {
  fun add(i: Int): Int = i + 1
  override fun doSomething() = "C"
}

open class OpenClass {
  var prop: String = "buzz"
  open var abstractProp: String = "default"
  fun doSomething(): String = "D"
  open fun abstractFn(): String = "default"
}

object C : OpenClass() {
  override var abstractProp: String = ""
    get() = "${field}trailer"
    set(value) {
      field = "header$value"
    }

  fun add(i: Int): Int = i + 1

  override fun abstractFn(): String = "overridden"
}

object D : OpenClass(), InterfaceType {
  fun add(i: Int): Int = i + 1
}
