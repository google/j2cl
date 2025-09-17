/*
 * Copyright 2025 Google Inc.
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
package suspendfunction

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import kotlin.coroutines.Continuation
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class SuspendFunction : SuspendFunInterface {
  fun nonSuspendingFunction(param: String): String = param

  suspend fun anotherSuspendFunction(foo: String): String = "Foo$foo"

  suspend fun main() {
    var s = anotherSuspendFunction("Foo")
    nonSuspendingFunction(s)

    testSuspendLambda(anotherSuspendFunction(s))

    s = testSuspendFunctionReference(::anotherSuspendFunction)
    s = testSuspendFunctionReference(::nonSuspendingFunction)

    // Test boxing on return type of suspend function
    val boxedInteger: Int? = GenericSuspendFunInterface<Int> { param -> param }.suspendMe(0)

    s = testChainedSuspendFunction().suspendMe("Chain of suspend function calls.")
  }

  suspend fun testSuspendLambda(param: String) {
    val lambda: suspend (param: String) -> String = {
      var s = anotherSuspendFunction(it)
      nonSuspendingFunction(s)
      s
    }
    val s = lambda(param)

    consumeSuspendLambda("Outer") {
      consumeSuspendLambda("Inner") {
        var s = anotherSuspendFunction("Lambda")
        nonSuspendingFunction(s)
      }
    }

    consumeSuspendFunInterface("FunInterface") { param -> param }

    consumeSuspendFunInterface(
      "FunInterface",
      object : SuspendFunInterface {
        override suspend fun suspendMe(param: String): String = param
      },
    )
  }

  suspend fun testSuspendFunctionReference(suspendFunction: suspend (String) -> String): String {
    return suspendFunction("FunctionReference")
  }

  suspend fun consumeSuspendFunInterface(
    param: String,
    suspendFunInterface: SuspendFunInterface,
  ): String {
    return suspendFunInterface.suspendMe(param)
  }

  suspend fun testChainedSuspendFunction(): SuspendFunInterface = this

  override suspend fun suspendMe(param: String): String = param

  class InnerClass {
    suspend fun testSuspendFunctionInInnerClass(param: String) {
      anotherSuspendFunctionInInnerClass(param)
    }

    private suspend fun anotherSuspendFunctionInInnerClass(param: Any?) {}
  }
}

suspend fun consumeSuspendLambda(param: String, suspendBlock: suspend (s: String) -> Unit) {
  suspendBlock.extFunOnSuspendLambda(param)
}

suspend fun (suspend (s: String) -> Unit).extFunOnSuspendLambda(param: String) {
  this(param)
}

suspend fun testSuspendLambdaInStaticContext(suspendable: SuspendFunInterface) {
  consumeSuspendLambda("StaticContext") {
    consumeSuspendLambda("Inner") { suspendable.suspendMe("") }
  }
}

fun interface SuspendFunInterface : GenericSuspendFunInterface<String> {
  override suspend fun suspendMe(param: String): String
}

fun interface GenericSuspendFunInterface<T> {
  suspend fun suspendMe(param: T): T
}

class Child : SuspendFunction() {
  override suspend fun suspendMe(param: String): String {
    return super.suspendMe(param)
  }

  inner class InnerClass {
    suspend fun testSuperQualified() {
      super@Child.main()
    }
  }
}

interface IConflict<T> {
  fun multiLevelConflict(c: Continuation<*>, t: T)
}

abstract class Conflict : IConflict<String> {
  // deliberately return String so JavaScript method names do not conflict but Java signatures do.
  suspend fun multiLevelConflict(s: String): String = s

  fun sameLevelConflict(c: Continuation<*>) {}

  suspend fun sameLevelConflict() {}
}

suspend fun testYield() {
  yield()

  yield("yield")
}

@JsMethod(namespace = JsPackage.GLOBAL) private external fun yield(arg: Any?): Any?

@JsMethod(namespace = JsPackage.GLOBAL) private external fun yield(): Any?

suspend fun testSuspendInlining() {
  suspendCoroutine { continuation -> continuation.resume(Unit) }
}

class GenericClass<T> {
  fun <V> testSuspendLambdReferingGenerics(param: V) {
    val suspendLambdaReferingGeneric: suspend () -> Unit = {
      val tRef: T? = null
      val vRef: V? = null
    }
  }
}

fun (suspend (String) -> Unit).extFunOnSuspendLambda() {}

fun testSuspendReferenceAsVariable() {
  suspend fun testIsActive(foo: String) {}
  ::testIsActive.extFunOnSuspendLambda()

  val suspendFunReference = ::testIsActive
  suspendFunReference.extFunOnSuspendLambda()
}

suspend fun testAccessCoroutineContext() {
  val context = coroutineContext
}
