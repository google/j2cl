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
package typewildcards

fun interface Function<I, O> {
  fun apply(i: I): O
}

interface List<T>

open class GenericType<T>(var field: T)

open class RecursiveType<T : RecursiveType<T>> {
  constructor(wildcardParameter: RecursiveType<*>?)
}

open class DeepRecursiveType<T : GenericType<in T>>

open class RecursiveSubtype : RecursiveType<RecursiveSubtype> {
  constructor(wildcardParameter: RecursiveType<*>) : super(wildcardParameter)
}

class Bar

class TypeWildCards {
  fun unbounded(g: GenericType<*>): Any {
    return g.field!!
  }

  fun upperBound(g: GenericType<out Bar>): Bar {
    return g.field
  }

  fun lowerBound(g: GenericType<in Bar>): Any {
    return g.field!!
  }

  fun unboundedRecursive(g: RecursiveType<*>) {}

  fun upperBoundRecursive(g: GenericType<out RecursiveType<*>>) {}

  fun lowerBoundRecursive(g: GenericType<in RecursiveType<*>>) {}

  fun deepRecursiveType(t: DeepRecursiveType<*>?) {}

  fun test() {
    unbounded(GenericType<Bar>(Bar()))
    upperBound(GenericType<Bar>(Bar()))
    lowerBound(GenericType<Bar>(Bar()))
  }

  private interface X {
    fun m()
  }

  interface Y {
    fun n()
  }

  open class A : X {
    var f = 0

    override fun m() {}
  }

  interface IntegerSupplier {
    fun get(): Int?
  }

  interface HasKey {
    fun getKey(): String?
  }

  abstract inner class Element : HasKey, IntegerSupplier

  abstract inner class OtherElement : IntegerSupplier, HasKey

  abstract inner class SubOtherElement : OtherElement(), HasKey

  fun testInferredGenericIntersection() {
    val elements: List<Element>? = null
    val otherElements: List<SubOtherElement>? = null
    // This is a rather complicated way to make sure the inference ends with a wildcard extending
    // multiple bounds. This is type of code that exposes b/67858399.
    val integers =
      transform(
        concat(elements, otherElements),
        { a ->
          a.getKey()
          a.get()
        },
      )
  }

  internal inner class Foo : GenericType<Foo>(Foo())

  fun testRecursiveGeneric() {
    takesRecursiveGeneric(Foo())
  }
}

fun <T : TypeWildCards.A> testBoundedTypeMemberAccess(t: T) {
  val i = t.f
  t.m()
}

fun <T> testIntersectionBoundedTypeMemberAccess(t: T)
  where T : TypeWildCards.A, T : TypeWildCards.Y {
  val i = t.f
  t.m()
  t.n()
}

private fun <F, V> transform(from: List<F>?, function: Function<in F, out V>): List<V>? {
  return null
}

private fun <E> concat(a: List<out E>?, b: List<out E>?): List<out E>? {
  return null
}

private fun takesRecursiveGeneric(foo: GenericType<TypeWildCards.Foo>) {}

interface RecursiveInterface<T : RecursiveInterface<T, C>, C> {
  fun m(): T
}

fun <C : RecursiveInterface<out C, C>> testInferredIntersectionWithTypeVariable(
  ri: RecursiveInterface<out C, C>
): C {
  return ri.m()
}

fun testWildcardInRecursiveVariableDeclaration() {
  var o: RecursiveType<*> = RecursiveType(null)
}

class MultipleGenerics<A, B, C>

fun <D> createMultipleGenerics(foo: List<D>): MultipleGenerics<D, String, List<D>> =
  MultipleGenerics()

var listWithWildcard: List<*>? = null
val valMultipleGenerics: MultipleGenerics<*, String, *> = createMultipleGenerics(listWithWildcard!!)

fun <T : String> concat(t1: T, t2: T): String = t1 + t2

fun <T : Int> add(t1: T, t2: T): Int = t1 + t2
