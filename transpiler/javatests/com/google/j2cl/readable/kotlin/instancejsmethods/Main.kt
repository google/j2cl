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
package instancejsmethods

import jsinterop.annotations.JsMethod

internal interface MyInterface {
  fun intfFoo(): Int
}

internal open class SuperParent {
  open fun f(a: Int, b: Int): Int {
    return a + b + 111
  }

  open fun bar(a: Int, b: Int): Int {
    return a * b + 222
  }
}

internal open class Parent : SuperParent() {
  /**
   * JsMethod that exposes non-JsMethod in parent, with renaming.
   *
   * A bridge method m_fun__int__int should be generated.
   */
  @JsMethod(name = "sum")
  override fun f(a: Int, b: Int): Int {
    return a + b
  }

  /**
   * JsMethod that exposes non-JsMethod in parent, without renaming.
   *
   * A bridge method m_bar__int__int should be generated.
   */
  @JsMethod
  override fun bar(a: Int, b: Int): Int {
    return a * b
  }

  /**
   * JsMethod that does not expose non-JsMethod in parent.
   *
   * No bridge method should be generated.
   */
  @JsMethod(name = "myFoo")
  open fun foo(a: Int): Int {
    return a
  }
}

internal class Child : Parent(), MyInterface {
  /**
   * Non-JsMethod that overrides a JsMethod with renaming.
   *
   * It should be generated as sum() (non-mangled name), no bridge method should be generated
   * because the bridge method is already generated in its parent.
   */
  override fun f(a: Int, b: Int): Int {
    return a + b + 1
  }

  /**
   * Non-JsMethod that overrides a JsMethod without renaming.
   *
   * It should be generated as bar() (non-mangled name), no bridge method should be generated
   * because the bridge method is already generated in its parent.
   */
  override fun bar(a: Int, b: Int): Int {
    return a * b + 1
  }

  /**
   * JsMethod that overrides a JsMethod.
   *
   * No bridge method should be generated.
   */
  @JsMethod(name = "myFoo")
  override fun foo(a: Int): Int {
    return a
  }

  /**
   * JsMethod that overrides a non-JsMethod from its interface.
   *
   * A bridge method should be generated.
   */
  @JsMethod
  override fun intfFoo(): Int {
    return 1
  }
}

fun testCallBySuperParent() {
  val sp = SuperParent()
  val p: SuperParent = Parent()
  val c: SuperParent = Child()
  val pp = Parent()
  val cc: Parent = Child()
  val intf: MyInterface = Child()

  sp.f(12, 35)
  sp.bar(6, 7)
  p.f(12, 35)
  p.bar(6, 7)
  c.f(12, 35)
  c.bar(6, 7)
  pp.foo(1)
  cc.foo(1)
  intf.intfFoo()
}

fun testCallByConcreteType() {
  val sp = SuperParent()
  val p = Parent()
  val c = Child()

  sp.f(12, 35)
  sp.bar(6, 7)
  p.f(12, 35)
  p.bar(6, 7)
  p.foo(1)
  c.f(12, 35)
  c.bar(6, 7)
  c.foo(1)
  c.intfFoo()
}
