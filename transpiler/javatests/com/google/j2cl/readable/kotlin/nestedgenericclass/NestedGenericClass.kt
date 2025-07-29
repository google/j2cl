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
package nestedgenericclass

class NestedGenericClass<T> {
  // nested generic classes with shadowing type parameters.
  inner class A<T> {
    inner class B<T>()
  }

  // nested non-generic classes that refers to the type parameters declared in outer class.
  inner class C {
    var c: T? = null

    inner class D {
      var d: T? = null
    }
  }

  // local classes
  fun <S> func(t: S) {
    class E<S> // generic class
    class F { // non-generic class
      var f: S? = null
    }
    E<Number>()
    F()
  }

  // multiple nesting levels
  fun <T> bar() {
    class G<T> {
      fun <T> bar() {
        class H<T>
        H<Number>()
      }
    }
    G<Error?>().bar<Any>()
  }

  fun test() {
    val n = NestedGenericClass<Number?>()
    val a = n.A<Error?>()
    val b = a.B<Exception?>()

    n.C()
    n.C().D()
  }
}

open class RecursiveTypeVariable<T : RecursiveTypeVariable<T>> {
  inner class Inner {}
}

fun <T : RecursiveTypeVariable<T>> test(t: T) {
  t.Inner()
}
