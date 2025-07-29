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
package overridingmethods

interface SomeInterface {
  fun function()

  var property: Int
}

open class Parent {
  open fun nonFinal() {}

  open fun finalInChild() {}

  fun finalInParent() {}

  open var property: Int
    get() = 1
    set(value) {}
}

open class Child : Parent(), SomeInterface {

  override var property: Int
    get() = 1
    set(value) {}

  override fun nonFinal() {}

  final override fun finalInChild() {}

  override fun function() {}
}

class FinalChild : Parent(), SomeInterface {
  override fun nonFinal() {}

  override fun finalInChild() {}

  override fun function() {}
}

fun main(vararg args: String) {
  Parent().property = Child().property
}
