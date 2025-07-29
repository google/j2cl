/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package lambdas

import javaemul.internal.annotations.Wasm
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

class Lambdas {
  var field = 100

  fun interface FunctionalInterface {
    fun m(i: Int): Int
  }

  fun call(intf: FunctionalInterface, n: Int): Int {
    return this.field + intf.m(n)
  }

  fun testLambdaExpressionStyle() {
    call({ i -> i + 1 }, 10)
  }

  fun testLambdaExpressionImplicitParameterStyle() {
    call({ it + 1 }, 10)
  }

  fun testLambdaFunctionStyle() {
    call(fun(i: Int): Int = i + 1, 10)
    call(
      fun(i: Int): Int {
        return i + 1
      },
      10,
    )
  }

  fun testLambdaCaptureField() {
    call({ i -> field + i + 1 }, 10)
  }

  fun testLambdaCaptureLocal() {
    var x = 1
    call(
      { i ->
        x = 10
        x + i + 1
      },
      10,
    )
  }

  fun testLambdaCaptureFieldAndLocal() {
    var x = 1
    call(
      { i ->
        val y = 1
        x + y + this.field + i + 1
      },
      10,
    )
  }

  fun funOuter() {}

  fun testLambdaCallOuterFunction() {
    call(
      fun(i: Int): Int {
        funOuter()
        this.funOuter()
        this@Lambdas.funOuter()
        return i + 2
      },
      10,
    )
  }

  fun testLambdaInStaticContext() {
    val f: FunctionalInterface = FunctionalInterface { i -> i }
  }

  fun interface Functional<T> {
    fun wrap(f: Functional<T>): Functional<T>?
  }

  fun <T> testInstanceMethodTypeVariableThroughLambda() {
    val wrapped: Functional<T> =
      Functional<T> { f ->
        object : Functional<T> {
          override fun wrap(f: Functional<T>): Functional<T>? {
            return null
          }
        }
      }
  }

  fun interface GenericFunctionalInterface<T> {
    fun m(i: T?): T?
  }

  fun <T> callWithTypeVariable(intf: GenericFunctionalInterface<T>, e: T?): T? {
    return intf.m(e)
  }

  class A

  fun callParameterized(intf: GenericFunctionalInterface<A>, e: A?): A? {
    return intf.m(e)
  }

  fun <T : Enum<T>> callTypeVariableWithBounds(
    intf: GenericFunctionalInterface<Enum<T>?>,
    e: Enum<T>?,
  ): Enum<T>? {
    return intf.m(e)
  }

  fun <T> callWithBiFunction(fn: BiFunction<T, String, Double>): GenericFunctionalInterface<T>? {
    return null
  }

  // Lambdas with default methods.
  fun interface BiFunction<T, U, R> {
    fun apply(t: T, u: U): R

    fun <V> andThen(after: Function<in R, out V>): BiFunction<T, U, V> {
      return BiFunction<T, U, V> { t, u -> after.apply(this.apply(t, u)) }
    }
  }

  fun interface Function<U, V> {
    fun apply(u: U): V
  }

  private class Wrapper<T> {
    val wrapped: T? = null
  }

  fun <T : Enum<T>?> testLambdaWithGenerics() {
    callWithTypeVariable({ i -> i }, A())
    callParameterized({ i -> i }, A())
    callTypeVariableWithBounds({ i -> null }, null)
    callWithBiFunction(BiFunction<T, String, Double> { x, y -> throw RuntimeException() })
    callWithBiFunction { x: Any, y: String -> 3.0 }
    val f = Function<T, Long> { item -> 1L }
    val f2 = Function<Wrapper<String>, String?> { item -> item.wrapped }
  }

  fun interface FunctionalInterfaceWithMethodReturningVoid {
    fun run()
  }

  fun testLambdaReturningVoidAsExpression() {
    val runner = FunctionalInterfaceWithMethodReturningVoid { A() }
  }

  fun testAnonymousInsideLambda() {
    val runner = FunctionalInterfaceWithMethodReturningVoid { -> A() }
  }

  open class Parent {
    val fieldInParent: Int = 0

    fun funInParent() {}
  }

  class LambdaInSubClass : Parent() {
    fun testLambdaInSubclass() {
      val f = FunctionalInterface {
        // call to outer class's inherited function
        funInParent() // this.outer.funInParent()
        this.funInParent() // this.outer.funInParent()
        this@LambdaInSubClass.funInParent() // this.outer.funInParent()

        // access to outer class's inherited fields
        var a = fieldInParent
        a = this.fieldInParent
        a = this@LambdaInSubClass.fieldInParent
        1
      }
    }
  }

  @JsFunction
  fun interface GenericJsFunction<R, T> {
    fun apply(t: T?): R?
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  fun interface Thenable<T> {
    @Wasm("nop") // Taking a non-native argument in a native method not supported in Wasm.
    fun then(f1: GenericJsFunction<Unit, T>, f2: GenericJsFunction<Unit, Throwable>)
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  fun interface AnotherThenable<T> {
    @Wasm("nop") // Taking a non-native argument in a native method not supported in Wasm.
    fun then(f1: GenericJsFunction<Unit, T>, f2: GenericJsFunction<Unit, Throwable>)
  }

  fun interface Equals<T> {
    override fun equals(o: Any?): Boolean

    fun get(): T? {
      return null
    }
  }

  // JsSupplier is not a functionnal interface in Kotlin as Kotlinc considers it defines two
  // abstract functions.
  // interface JsSupplier<T : Number> : Equals<T> {
  //   @JsMethod override fun get(): T?
  // }

  @Wasm("nop") // Converting lambdas to native types not supported in Wasm.
  fun testJsInteropLambdas() {
    val thenable = Thenable<String> { f1, f2 -> f1.apply(null) }
    val otherThenable = AnotherThenable<String> { f1, f2 -> f1.apply(null) }
    // JsSupplier is not a functionnal interface in Kotlin and cannot be implemented with a lambda
    // val stringJsSupplier = JsSupplier<Int> { -> 1 }
    // stringJsSupplier.get()
    // val equals: Equals<String> = stringJsSupplier
    val equals = Equals<String> { o -> false }
    equals.equals(null)
    equals.get()
  }

  interface JustADefaultT<T> {
    fun method(t: T) {}
  }

  interface JustADefaultS<S> {
    fun method(s: S) {}
  }

  interface MarkerWithDefaultMethod {
    fun defaultMethod() {}
  }
}

private class IdentityFunction : Lambdas.GenericJsFunction<Any, Any> {
  override fun apply(o: Any?): Any {
    return o!!
  }
}

private var identityFunction: IdentityFunction? = null

/** Returns the identity function. */
@SuppressWarnings("unchecked")
fun <E> identity(): Lambdas.GenericJsFunction<E, E> {
  if (identityFunction == null) {
    // Lazy initialize the field.
    identityFunction = IdentityFunction()
  }
  return identityFunction as Lambdas.GenericJsFunction<E, E>
}

fun testStaticMethodTypeVariableThroughLambda() {
  val wrapped: Lambdas.Functional<Int> =
    Lambdas.Functional<Int> { f ->
      object : Lambdas.Functional<Int> {
        override fun wrap(f: Lambdas.Functional<Int>): Lambdas.Functional<Int>? {
          return null
        }
      }
    }
}

fun m(): Any? {
  return null
}

fun testLambdaCallingStaticMethod() {
  val f: (Any) -> Any? = { m() }
}

fun testJavaSamInterfaces() {
  JavaInterfaces.GenericInterfaceWithSam<String> { s -> s }
  JavaInterfaces.InterfaceWithSamFoo { s -> s }
  JavaInterfaces.InterfaceWithSamBar { s -> s }
  JavaInterfaces.A { s -> s }
  JavaInterfaces.B { s -> s }
  JavaInterfaces.C { s -> s }
  JavaInterfaces.D { s -> s }
  JavaInterfaces.E { s -> s }
  JavaInterfaces.F { s -> s }
}

fun interface Runnable {
  fun run()
}

class Outer {
  fun m() {}

  fun n() {
    val r = Runnable { this.m() }
  }
}

open class Super {
  open fun m() {}
}

class Sub : Super() {
  fun n() {
    val r: Runnable =
      object : Runnable {
        override fun run() {
          super@Sub.m()
        }
      }
  }
}

class SubWithLambda : Super() {
  fun n() {
    val r = Runnable { super.m() }
  }
}

interface EmptyInterface

fun interface EmptyInterfaceProvider {
  fun provide(): EmptyInterface
}

object ProviderHolder {
  val emptyInterface = object : EmptyInterface {}
  val provideFromProperty = EmptyInterfaceProvider { emptyInterface }

  val provideFromAnonImpl = EmptyInterfaceProvider {
    return@EmptyInterfaceProvider object : EmptyInterface {}
  }
}

fun <T> testParameterizedTypeWithUnusedTypeVariable() {
  acceptsSupplier(Supplier { object : Any() {} })
}

fun interface Supplier<T> {
  fun get(): T?
}

private fun <T> acceptsSupplier(supplier: Supplier<T?>?) {}
