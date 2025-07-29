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
package jvmstatic

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty

open class ClassWithCompanion {
  // Needed to avoid compilation error: "Classes that contain only a companion object should be
  // replaced with a named object declaration"
  val instanceProperty = 0

  companion object {
    @JvmStatic val foo = f(this)

    @JvmStatic
    val bar =
      object {
        fun foo(): Any {
          return object {
            fun foo() = nonJvmStatic()
          }
        }
      }

    @JvmStatic val staticProperty = 1

    @JvmStatic fun staticFunction() = 2

    @JvmStatic
    internal fun staticInternalFunction() {
      val foo =
        object {
          fun foo() {
            nonJvmStatic()
          }
        }
    }

    @JvmStatic protected fun staticProtectedFunction() {}

    @JsProperty @JvmStatic val staticJsProperty = 2

    @JvmStatic
    val staticJsPropertyWithGetter = 2
      @JsProperty get

    @JsProperty @JvmStatic fun getStaticJsPropertyMethod() = 4

    @JsMethod @JvmStatic fun staticJsMethod() = 3

    fun nonJvmStatic() = 4

    @JvmStatic
    fun <T, V> staticMethodWithTypeParameter(): Foo<T, V>? {
      return null
    }
  }
}

interface Foo<T, V> {
  fun f(o: V): T
}

fun f(o: Any): Any = o

object KotlinObjectWithStaticMembers {
  @JvmStatic val staticProperty = 1

  @JvmStatic fun staticFunction() = 2
}

interface InterfaceWithCompanion {
  companion object {
    @JvmStatic fun staticMethod() = 3
  }
}

fun testJvmStaticCalls() {
  val a = ClassWithCompanion.staticProperty
  val b = ClassWithCompanion.staticFunction()
  ClassWithCompanion.staticInternalFunction()

  val c = ClassWithCompanion.staticJsProperty
  val d = ClassWithCompanion.staticJsPropertyWithGetter
  val e = ClassWithCompanion.getStaticJsPropertyMethod()
  val f = ClassWithCompanion.staticJsMethod()

  val g = KotlinObjectWithStaticMembers.staticProperty
  val h = KotlinObjectWithStaticMembers.staticFunction()

  val i = InterfaceWithCompanion.staticMethod()

  class Foo : ClassWithCompanion() {
    fun f() {
      ClassWithCompanion.staticProtectedFunction()
    }
  }
}
