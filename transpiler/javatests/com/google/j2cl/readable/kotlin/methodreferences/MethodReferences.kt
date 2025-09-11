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
package methodreferences

import jsinterop.annotations.JsFunction

val p = "123"

fun m() = Any()

fun <T> getString(v: T): String = "" + v

fun <T> MethodReferences<T>.appendX(): String = property + "X"

val staticStringProducer = MethodReferences.Producer<String>(m()::toString)

class MethodReferences<T> {
  fun interface Producer<T> {
    fun produce(): T
  }

  fun interface Predicate<T> {
    fun apply(parameter: T): Boolean
  }

  fun interface ArrayProducer {
    fun produce(size: Int): IntArray
  }

  inner class ObjectCapturingOuter {
    fun getMain() = this@MethodReferences
  }

  fun interface Function<T, U> {
    fun apply(t: T): U
  }

  fun interface BiFunction<T, U, V> {
    fun apply(t: T, u: U): V
  }

  @JsFunction
  fun interface JsProducer<T> {
    fun produce(): T
  }

  @JsFunction
  fun interface JsFunctionInterface<T, U> {
    fun apply(t: T): U
  }

  @JsFunction
  fun interface JsBiFunction<T, U, V> {
    fun apply(t: T, u: U): V
  }

  val property = "123"

  fun isA(): Boolean = true

  fun self(): MethodReferences<T> = this

  fun t(): T? = null

  fun sameAs(n: Number): Boolean = false

  fun main() {
    // direct reference of instance function
    val a = this::sameAs
    a(1)
    a.invoke(1)

    acceptProducer(::Any)

    // Top-level method/property
    acceptProducer(::p)
    acceptProducer(::m)

    // Object methods
    acceptProducer(Obj::objectProperty)
    acceptProducer(Obj::objectMethod)

    acceptProducer(::companionObjectProperty)
    acceptProducer(::companionObjectMethod)
    acceptProducer(Companion::companionObjectProperty)
    acceptProducer(Companion::companionObjectMethod)

    // Qualified instance method/property
    acceptProducer(this::property)
    acceptProducer(this::isA)
    acceptProducer(this::self)
    acceptFunction(this::sameAs)
    acceptProducer(MethodReferences<T>()::property)
    acceptProducer(MethodReferences<T>()::isA)
    acceptProducer(MethodReferences<T>()::self)
    acceptFunction(MethodReferences<T>()::sameAs)
    val parameterizedInstance = MethodReferences<String>()
    acceptProducer(parameterizedInstance::t)

    // Unqualified instance method/property
    acceptFunction(MethodReferences<T>::property)
    acceptPredicate(MethodReferences<T>::isA)
    acceptFunction(MethodReferences<T>::self)
    acceptBiFunction(MethodReferences<T>::sameAs)

    // Extension function
    acceptProducer(this::appendX)
    acceptProducer(MethodReferences<T>()::appendX)

    // Extension function (unqualified)
    acceptFunction(MethodReferences<T>::appendX)

    acceptProducer(::ObjectCapturingOuter)

    // TODO(b/277785487): Implement intrinsic function references.
    // acceptArrayProducer(::IntArray)

    // acceptFunction(::BooleanArray)
    // acceptFunction(::CharArray)
    // acceptFunction(::ByteArray)
    // acceptFunction(::ShortArray)
    // acceptFunction(::IntArray)
    // acceptFunction(::LongArray)
    // acceptFunction(::FloatArray)
    // acceptFunction(::DoubleArray)
    // acceptFunction<Int, Array<Any?>>(::arrayOfNulls)
    // acceptFunction<Int, Array<Any?>>(::arrayOfNulls)

    // acceptFunction(Function(::BooleanArray))
    // acceptFunction(Function(::CharArray))
    // acceptFunction(Function(::ByteArray))
    // acceptFunction(Function(::ShortArray))
    // acceptFunction(Function(::IntArray))
    // acceptFunction(Function(::LongArray))
    // acceptFunction(Function(::FloatArray))
    // acceptFunction(Function(::DoubleArray))
    // acceptFunction(Function<Int, Array<Any?>>(::arrayOfNulls))
    // acceptFunction(Function<Int, Array<Any?>>(::arrayOfNulls))

    acceptJsProducer(::Any)
    acceptJsProducer(::p)
    acceptJsProducer(::m)

    acceptJsProducer(Obj::objectProperty)
    acceptJsProducer(Obj::objectMethod)

    acceptJsProducer(::companionObjectProperty)
    acceptJsProducer(::companionObjectMethod)

    acceptJsProducer(this::property)
    acceptJsProducer(this::self)
    acceptJsFunctionInterface(this::sameAs)
    acceptJsProducer(MethodReferences<T>()::property)
    acceptJsProducer(MethodReferences<T>()::self)
    acceptJsFunctionInterface(MethodReferences<T>()::sameAs)

    acceptJsFunctionInterface(MethodReferences<T>::property)
    acceptJsFunctionInterface(MethodReferences<T>::self)
    acceptJsBiFunction(MethodReferences<T>::sameAs)

    // TODO(b/277785487): Implement intrinsic function references.
    // acceptJsFunctionInterface(::IntArray)

    // Type inference for generic functions
    acceptFunction(::getString, "")

    acceptFunction1(Int::toString, 1)

    // These are all redundant casts that should be removable, even after the method references is
    // rewritten as a lambda.
    acceptProducer(((::m as Producer<*>) as Any) as Producer<Any>)

    // in-variance
    acceptFunctionInVariance(MethodReferences<Any?>::self)

    // local function
    fun localFunction(p: Int): String {
      return "p$p"
    }
    acceptFunction(::localFunction, 1)

    fun String.localExtensionFunction() = this + "ff"

    acceptProducer("bar"::localExtensionFunction)
  }

  fun <T> acceptProducer(f: Producer<T>) {}

  fun <T> acceptPredicate(f: Predicate<T>) {}

  fun acceptArrayProducer(f: ArrayProducer) {}

  fun <T, U> acceptFunction(f: Function<T, U>) {}

  fun <T, U> acceptFunction(f: Function<T, U>, arg: T) {}

  fun <U, V> acceptFunctionInVariance(f: Function<in U, V>) {}

  fun <T, U, V> acceptBiFunction(f: BiFunction<T, U, V>) {}

  fun <T> acceptJsProducer(f: JsProducer<T>) {}

  fun <T, U> acceptJsFunctionInterface(f: JsFunctionInterface<T, U>) {}

  fun <T, U, V> acceptJsBiFunction(f: JsBiFunction<T, U, V>) {}

  fun <T, V> acceptFunction1(f: Function1<T, V>, arg: T) = f(arg)

  companion object {
    var companionObjectProperty = "test"

    fun companionObjectMethod() = Any()
  }
}

object Obj {
  var objectProperty = "test"

  fun objectMethod() = Any()
}

fun testFunctionReferences() {
  val topLevelFunRef = ::m
  topLevelFunRef()
  topLevelFunRef.invoke()

  val classFunRef = MethodReferences<*>::sameAs
  classFunRef(MethodReferences<Any>(), 1)
  classFunRef.invoke(MethodReferences<Any>(), 1)

  val extensionFunRef = MethodReferences<Any>::appendX
  extensionFunRef(MethodReferences())
  extensionFunRef.invoke(MethodReferences())

  // There is no way to directly reference a generic function without assigning it to a function
  // type. See https://youtrack.jetbrains.com/issue/KT-13003
  // val genericTopLevelFunRef = ::getString<String>
}

fun acceptFunctionExpression(f: () -> Any) {}

val kFunctionVar: kotlin.reflect.KFunction0<Any> = ::m

fun returnKFunction(): kotlin.reflect.KFunction0<Any> = ::m

fun testKFunctionReference() {
  acceptFunctionExpression(kFunctionVar)
  acceptFunctionExpression(returnKFunction())
}
