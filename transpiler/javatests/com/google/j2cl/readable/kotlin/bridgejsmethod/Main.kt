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
package bridgejsmethod

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsType

class Main {
  open class A<T> {
    @JsMethod
    open fun f(t: T): T {
      return t
    }

    // not a JsMethod.
    open fun bar(t: T) {}
  }

  /** Interface with JsMethod. */
  interface I<T : Number> {
    @JsMethod(name = "fNumber") fun f(t: T): T
  }

  /** Interface with non-JsMethod. */
  interface J<T> {
    fun bar(t: T)
  }

  open class B : A<String>() {
    // It is a JsMethod itself and also overrides a JsMethod.
    // One JS bridge method for fun__Object(), but since the bridge method is a JsMethod, this
    // real implementation method is emit as non-JsMethod.
    @JsMethod
    override fun f(s: String): String {
      return s
    }

    // It is a JsMethod itself and overrides a non-JsMethod.
    // Two bridge methods, one for generic method, and one for exposing non-JsMethod, should result
    // in only one bridge method.
    @JsMethod override fun bar(s: String) {}
  }

  class C : A<Int>() {
    // It's not a directly annotated JsMethod, but overrides a JsMethod.
    // The bridge method is a JsMethod, and this real implementation method is non-JsMethod.
    override fun f(i: Int): Int {
      return i
    }
  }

  class D : A<Int>(), I<Int> {
    // Two JS bridges: fun__Object, and fun__Number, and should result in only one, and this real
    // implementation method is non-JsMethod.
    override fun f(i: Int): Int {
      return i
    }
  }

  class E : B(), J<String> // inherited B.bar() accidentally overrides J.bar().
  // Two bridges, one for generic method, the other for exposing non-JsMethod, should result in
  // only one bridge.

  class F : A<Int>(), I<Int>

  interface G<V> {
    fun enclose(value: V): V
  }

  class H<V> : G<V> {
    /**
     * The implemented enclose() has now been marked @JsMethod but mangled H.enclose() is still
     * required to execute the behavior defined here. A bridge method is needed from the mangled to
     * unmangled form. The version of the mangled form that is found in super types references type
     * variable C_G_V and must be specialized to C_H_V before being added in type H.
     */
    @JsMethod
    override fun enclose(value: V): V {
      return value
    }
  }

  open class K<K1, K2> {
    internal open fun f(k1: K1, k2: K2) {}
  }

  class L<L1> : K<String, L1>() {
    /**
     * Like the G/H example but requires type specialization across classes instead of interfaces,
     * with multiple type variables and some terminating in both concrete types and type variables.
     */
    @JsMethod override fun f(k1: String, l1: L1) {}
  }

  internal interface M {
    val b: B
  }

  @JsType
  internal abstract inner class N internal constructor() : M {
    abstract override val b: B
  }

  internal inner class O : N() {
    override val b: B = null!!
  }

  internal interface P {
    val key: String
  }

  @JsType
  internal abstract inner class Q internal constructor() : P {
    abstract override val key: String
  }

  @JsType
  internal abstract inner class R internal constructor() : Q() {
    override val key: String
      get() = ""
  }

  internal inner class S : R()

  fun test() {
    val a = A<Int>()
    a.f(1)
    a.bar(1)
    val b = B()
    b.f("abc")
    b.bar("abc")
    val c = C()
    c.f(1)
    c.bar(1)
    val d = D()
    d.f(1)
    d.bar(1)
    val e = E()
    e.f("abc")
    e.bar("abc")
    val h = H<Int>()
    h.enclose(1)
    val l = L<Int>()
    l.f("foo", 1)
  }

  // Repro for b/144844889
  internal open inner class GrandParent<T> {
    @JsMethod
    // This method would have the signature jsMethod(Object) hence it would be mangled as
    // jsMethod_Object if it was not a jsMethod.
    open fun jsMethod(t: T) {}

    open fun method(t: T) {}
  }

  internal open inner class Parent<T : Parent<T>> : GrandParent<T>() {
    @JsMethod
    // This method would have the signature jsMethod(Parent) hence it would be mangled as
    // jsMethod_Parent if it was not a jsMethod, requiring a bridge from jsMethod_Object. But since
    // they are JsMethods the bridge is not created, and jsMethod takes a Parent as a parameter.
    override fun jsMethod(t: T) {}

    override fun method(t: T) {}
  }

  // The bridge method creation scheme works independently form the JsMethod bridge creation, and
  // here it determines that it needs a two bridges one jsMethod(Parent) to super.jsMethod(Parent)
  // and one from jsMethod(Object) to super.jsMethod(Object) but since they are JsMethod and the
  // names collide only one is created resulting in an inconsistent override.
  internal inner class ChildWithoutOverrides : Parent<ChildWithoutOverrides>() {}

  internal inner class ChildWithOverrides<T : ChildWithOverrides<T>> : Parent<T>() {
    @JsMethod
    // This method would have the signature jsMethod(ChildWithOverrides) hence it would be mangled
    // as jsMethod_ChildWithOverrides if it was not a jsMethod, requiring a bridge from
    // jsMethod_Object. But since they are JsMethods the bridge is not created and jsMethod takes
    // a ChildWithOverrides as a parameter.
    override fun jsMethod(t: T) {}

    override fun method(t: T) {}
  }

  internal inner class ChildWithRenamedOverride : GrandParent<ChildWithRenamedOverride?>() {
    @JsMethod(name = "renamedJsMethod")
    // This method will be a JsMethod that is bridged from another JsMethod with a different name.
    override fun jsMethod(t: ChildWithRenamedOverride?) {}
  }
}
