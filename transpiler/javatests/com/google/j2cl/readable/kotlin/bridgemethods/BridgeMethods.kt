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
package bridgemethods

import jsinterop.annotations.JsType

interface SomeInterface<T, S> {
  fun foo(t: T, s: S)

  fun foo(t: T, n: Number?)
}

open class SuperParent<T, S> {
  open fun foo(t: T, s: S) {}
}

open class Parent<T : Error?> : SuperParent<T, Number?>() {
  // foo__Error__Number
  // there should be a bridge method foo__Object__Object for SuperParent.foo(T,S),
  // which delegates to foo(Error, Number)
  override fun foo(t: T, s: Number?) {}

  open fun <T : Number?> bar(t: T) {} // bar__Number

  fun <T> `fun`(t: T) {}
}

class BridgeMethod : Parent<AssertionError?>(), SomeInterface<AssertionError?, Number?> {
  // foo__AssertionError__Number, there should be a bridge method
  // foo__Error__Number for Parent.foo(T, Number), and a bridge method
  // foo__Object__Object for SuperParent.foo(T, S) and for SomeInterface.foo(T, S),
  // and a bridge method foo__Object__Number for SomeInterface.foo(T, Number).
  override fun foo(a: AssertionError?, n: Number?) {}

  // Not allowed in kotlin: accidental override error
  // fun bar(t: Number?) {} // no bridge method for Parent.bar(T), same signature.
  fun `fun`(t: Number?) {} // not override, no bridge method.
}

internal interface SomeOtherFooInterface<T> {
  fun foo(t: T, s: Double?)
}

internal class DualUnrelatedBridges :
  SomeInterface<String?, Double?>, SomeOtherFooInterface<String?> {
  // foo__Object__Object should be a bridge method
  // foo__Object__Double should be a bridge method
  override fun foo(s: String?, n: Double?) {}

  // foo__Object__Number should be a bridge method
  override fun foo(s: String?, n: Number?) {}
}

internal open class SuperDualUnrelatedAccidentalBridges {
  fun foo(s: String?, n: Double?) {}

  fun foo(s: String?, n: Number?) {}
}

internal class DualUnrelatedAccidentalBridges :
  SuperDualUnrelatedAccidentalBridges(),
  SomeInterface<String?, Double?>,
  SomeOtherFooInterface<String?> {
  // foo__Object__Object should be a bridge method
  // foo__Object__Double should be a bridge method
  // foo__Object__Number should be a bridge method
}

internal interface Getter {
  fun get(s: String?): String?
}

internal open class ClassWithParameterizedGet<T> {
  fun get(t: T): T? {
    return null
  }
}

internal class AccidentalOverrideBridge : ClassWithParameterizedGet<String?>(), Getter {
  // get__String will be a bridge method delegating to (super) get_Object
  // get__Object should be redefined to bridge to get__String
  fun test() {
    val g: Getter = AccidentalOverrideBridge()
    val a = g.get("")
  }
}

internal class TestCase10036 {
  @JsType
  internal interface BI3 {
    fun get(value: String?): String? {
      return "BI3 get String"
    }
  }

  internal interface BI2 {
    fun get(value: String?): String?
  }

  internal interface BI1 : BI3 {
    override fun get(value: String?): String?
  }

  internal abstract class B<B1 : Comparable<*>?> : BI1, BI2 {
    // The abstract method here is unrelated.
    abstract fun get(value: B1): String?
    // get__String should be a bridge to JsMethod this.get()
    // get should be a default method implemented here calling BI3.get.
  }
}

internal class TestCase102 {
  @JsType
  internal interface BI2 {
    fun set(value: String?)
  }

  internal interface BI1 {
    fun set(value: String?)
  }

  internal abstract class B<B1> : BI1, BI2 {
    abstract fun set(value: B1)
    // get__String should be a bridge to (JsMethod) this.get.
  }

  internal inner class C<B1> : B<B1>() {
    override fun set(value: String?) {}

    override fun set(value: B1) {}
  }
}

internal open class ParameterizedParent<T : ParameterizedParent<T>?> {
  open fun <Q : T?> m(t: Q): Q? {
    return null
  }
}

internal open class ReparametrerizedChild<E : ReparametrerizedChild<E>?> :
  ParameterizedParent<E>() {
  override fun <S : E?> m(t: S): S? {
    return null
  }
}

internal class LeafChild : ReparametrerizedChild<LeafChild?>()

// Bridges that override final methods. Repro for b/280123980.
interface StringConsumer {
  fun accept(s: String)
}

abstract class Consumer<T> {
  fun accept(t: T) {}
}

// Needs a specializing bridge from accept(String) to super.accept(Object).
class StringConsumerImpl : Consumer<String>(), StringConsumer

// Repro for b/357041082
internal abstract class SpecializingReturnAbstractClass {
  abstract fun foo(): Any?
}

internal interface SpecializingReturnInterface {
  fun foo(): String?
}

internal abstract class SpecializingReturnAbstractSubClass :
  SpecializingReturnAbstractClass(), SpecializingReturnInterface

// Repro for b/357043910
interface InterfaceWithDefaultMethod {
  fun foo(): Any {
    return "A"
  }
}

interface InterfaceOverridingDefaultMethod : InterfaceWithDefaultMethod {
  override fun foo(): String
}

internal abstract class DoesNotInheritDefaultMethod1 :
  InterfaceWithDefaultMethod, InterfaceOverridingDefaultMethod

internal abstract class DoesNotInheritDefaultMethod2 :
  InterfaceOverridingDefaultMethod, InterfaceWithDefaultMethod

internal abstract class SpecializingReturnAbstractSubclass :
  SpecializingReturnAbstractClass(), SpecializingReturnInterface {
  // foo(Object) should be a bridge method.
}
