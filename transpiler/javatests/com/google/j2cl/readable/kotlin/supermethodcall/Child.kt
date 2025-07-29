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
package supermethodcall

interface GrandParentInterface {
  fun defaultGrandParent() {}
}

interface ParentInterface {
  fun defaultParent() {}
}

open class GrandParent {
  open fun grandParentSimplest() {}
  open fun grandParentWithParams(foo: Int) {}
  open fun grandParentWithChangingReturn(): Any? {
    return null
  }
  open fun defaultParent() {}
  open fun defaultGrandParent() {}
}

open class Parent : GrandParent(), ParentInterface {
  open fun parentSimplest() {}
  open fun parentWithParams(foo: Int) {}
  open fun parentWithChangingReturn(): Any? {
    return null
  }

  override fun defaultParent() {
    super<GrandParent>.defaultParent()
    super<ParentInterface>.defaultParent()
  }
}

class Child : Parent(), GrandParentInterface {
  override fun parentSimplest() {
    super.parentSimplest()
  }

  override fun parentWithParams(foo: Int) {
    super.parentWithParams(foo)
  }

  override fun parentWithChangingReturn(): Child {
    super.parentWithChangingReturn()
    return this
  }

  override fun grandParentSimplest() {
    super.grandParentSimplest()
  }

  override fun grandParentWithParams(foo: Int) {
    super.grandParentWithParams(foo)
  }

  override fun grandParentWithChangingReturn(): Child {
    super.grandParentWithChangingReturn()
    return this
  }

  override fun defaultGrandParent() {
    super<Parent>.defaultGrandParent()
    super<GrandParentInterface>.defaultGrandParent()
  }
}

interface I1 {
  fun m() {}
}

interface I2 {
  fun m()
}

interface I3 : I1

open class Super {
  open fun m() {}
}

class Sub : Super(), I2, I3 {
  override fun m() {
    super<I3>.m()
  }
}

class SuperToStringTest : I1 {
  override fun toString() = super.toString()
}
