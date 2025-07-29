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
package anonymousclass

import jsinterop.annotations.JsConstructor

abstract class SomeClass {
  abstract fun foo(): String

  constructor(i: Int)
}

abstract class SomeClassWithStaticMembers {
  abstract fun foo(): String

  constructor(i: Int)

  companion object {
    fun staticMethod() {}
  }
}

class AnonymousClass {
  var i = 0
  var o: Any

  constructor(a: Any) {
    o =
      object : SomeClass(0) {
        val outer = this@AnonymousClass
        val other = a

        override fun foo(): String {
          return "" + i
        }
      }
  }

  fun main() {
    var capturedVar = 1

    val instance =
      object : SomeClass(i) {
        val o = this
        val outer = this@AnonymousClass

        override fun foo(): String {
          capturedVar = 2
          return "a"
        }
      }

    capturedVar++

    val instanceWithStaticMembers =
      object : SomeClassWithStaticMembers(this@AnonymousClass.i) {
        val o = this
        val outer = this@AnonymousClass

        override fun foo(): String {
          return "a"
        }
      }
  }
}

fun staticFunc() {
  var a = 1
  val implicitlyStatic: SomeClass =
    object : SomeClass(++a) {
      override fun foo(): String {
        a = 2
        return "a"
      }
    }
}

open class ParametrizedClass<T>

class ParametrizedAnonymousClass<T> {
  fun <S> main(s: S) {
    val instance =
      object : ParametrizedClass<S>() {
        fun f(t: T): S {
          return s
        }
      }
  }
}

fun <S> main(s: S) {
  object : Iterable<S> {
    override fun iterator(): Iterator<S> {
      return object : Iterator<S> {
        override fun hasNext(): Boolean {
          return false
        }

        override fun next(): S {
          return null!!
        }
      }
    }
  }
}

class AnonymousClassInProperty<K> {
  private val someProp: ParametrizedClass<K>
    get() =
      object : ParametrizedClass<K>() {
        fun whatever(): K? = null
      }
}

interface Foo {
  fun returnAny(): Any
}

// Test case for b/280515233
fun createFoo(): Foo =
  object : Foo {
    // the class of the returned anonymous object should have be assigned a name in the IR tree,
    // otherwise the `BridgeLowering` pass try to compute the function signature.
    override fun returnAny() = object {}
  }

interface SomeInterface {
  companion object {
    val implicitlyStatic: SomeClass =
      object : SomeClass(1) {
        override fun foo(): String {
          return "a"
        }
      }
  }
}

// Test case for b/321755877
var trueVar = false

open class JsConstructorClass {
  @JsConstructor constructor(o: Any?)

  constructor() :
    this(
      if (trueVar) {
        object {}
      } else {
        null
      }
    )
}

class JsConstructorSubclass : JsConstructorClass {
  @JsConstructor
  constructor() :
    super(
      if (trueVar) {
        object {}
      } else {
        null
      }
    )
}
