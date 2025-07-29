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
package genericmethod

import javaemul.internal.annotations.UncheckedCast

open class GenericMethod<T> {
  fun <T, S> foo(f: T, s: S) {} // two type parameters, no bounds.

  fun f(o: Any?) {}

  fun <T : Exception> f(t: T) {} // type parameter with bounds.

  fun <T : Error> f(t: T) { // type parameter with different bounds.
    object : GenericMethod<T>() {
      // inherit method T
      fun fun2(t: T) {} // inherit method T

      fun <T> fun2(t: T) {} // redefine T
    }

    class LocalClass<T> : GenericMethod<T>() {
      fun fun2(t: T) {}

      fun <T : Number?> fun2(t: T) {}
    }
    LocalClass<T>()
  }

  fun <T> bar(): GenericMethod<T>? {
    return null
  } // return parameterized type.

  fun <T> f(array: Array<T>): Array<T> { // generic array type
    return array
  }

  fun <T> checked(): T {
    return null!!
  }

  @UncheckedCast
  fun <T> unchecked(): T {
    return null!!
  }

  fun test() {
    val g: GenericMethod<Number> = GenericMethod()
    g.foo(g, g) // call generic method without diamond.
    g.foo<Error, Exception>(Error(), Exception()) // call generic method with diamond.

    g.f(Any())
    g.f(Exception())
    g.f(Error())
    g.f(Array<String>(1) { "asdf" })

    var s = checked<String>()
    s = unchecked<String>()

    this.checked<Content>().prop
    this.unchecked<Content>().prop
  }
}

class SuperContainer<C : Container<*>> {
  fun get(): C = null!!
}

class Container<CT : Content> {
  fun get(): CT = null!!
}

class Content {
  val prop: String
    get() = null!!
}

fun acceptsContent(content: Content) {}

fun acceptsString(string: String?) {}

fun testErasureCast_wildcard() {
  val list: List<Container<*>> = ArrayList()
  val content = list[0].get()
  acceptsString(content.prop)
  acceptsContent(content)

  val nestedWildcardList: List<SuperContainer<out Container<out Content>>> = ArrayList()
  val nestedContent = nestedWildcardList[0].get().get()
  acceptsString(nestedContent.prop)
  acceptsContent(nestedContent)

  val deepWildcardList: List<SuperContainer<Container<out Content>>> = ArrayList()
  val deepContent = deepWildcardList[0].get().get()
  acceptsString(deepContent.prop)
  acceptsContent(deepContent)
}

fun <CT : Container<C>, C : Content> testErasureCast_typeVariable() {
  val list: List<Container<C>> = ArrayList()
  val content: Content = list[0].get()
  acceptsString(content.prop)
  acceptsContent(content)
  val nestedTypeVariableList: List<SuperContainer<CT>> = ArrayList()
  val nestedContent: Content = nestedTypeVariableList[0].get().get()
  acceptsString(nestedContent.prop)
  acceptsContent(nestedContent)
  val deepTypeVariableList: List<SuperContainer<Container<C>>> = ArrayList()
  val deepContent: Content = deepTypeVariableList[0].get().get()
  acceptsString(deepContent.prop)
  acceptsContent(deepContent)
}
