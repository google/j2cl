/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
// Direct call of suspendCoroutine is not allowed by android linter.
@file:Suppress("SuspendCoroutine")

package coroutines

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrows
import com.google.j2cl.integration.testing.Asserts.assertTrue
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

fun main(vararg unused: String) {
  testNonSuspendingCoroutine()
  testSuspendingCoroutine()
  testSuspendingCoroutineAndResuming()
  testCoroutineCannotBeStartedTwice()
  testSuspendingCoroutineWithReceiver()
  testCoroutineWithExceptions()
  testDefaultParametersInCoroutines()
  testCoroutineWithVarargs()
  testCoroutineInterception()
}

private suspend fun notActuallySuspendingButReturningString(): String {
  return "CoroutineResult"
}

class WithSuspendFunction(private val suffix: String) {
  suspend fun returnString(): String {
    return "CoroutineResult$suffix"
  }
}

private fun testNonSuspendingCoroutine() {
  startCoroutine { notActuallySuspendingButReturningString() }.assertSuccess("CoroutineResult")

  startCoroutine { WithSuspendFunction("FromWithSuspendFunction").returnString() }
    .assertSuccess("CoroutineResultFromWithSuspendFunction")
}

private fun testSuspendingCoroutine() {
  startCoroutine { it.suspendCoroutine() }.assertSuspended()
}

private fun testSuspendingCoroutineAndResuming() {
  startCoroutine {
      val resumedString = it.suspendCoroutine()
      val resumedInt = it.suspendCoroutine()
      "result=$resumedString$resumedInt"
    }
    .assertSuspended()
    .resume("Foo")
    .assertSuspended()
    .resume(10)
    .assertSuccess("result=Foo10")
}

private fun testCoroutineCannotBeStartedTwice() {
  var started = false
  val coroutine =
    suspend {
        started = true
        suspendCoroutine<Unit> {}
      }
      .createCoroutine(noopCompletion)

  coroutine.resume(Unit)
  assertTrue(started)

  // The original continution returned on the coroutine creation cannot be used to resumed the
  // coroutine.
  assertThrows(IllegalStateException::class.java) { coroutine.resume(Unit) }
}

private fun testSuspendingCoroutineWithReceiver() {
  class MessageHolder() {
    var message = ""

    suspend fun append(controller: ContinuationController<Any>) {
      message += controller.suspendCoroutine()
    }
  }

  // Start the coroutine, providing a MessageHolder as the receiver.
  startCoroutine(MessageHolder()) { controller ->
      append(controller)
      append(controller)
      message
    }
    .resume("Hello, ")
    .resume("World!")
    .assertSuccess("Hello, World!")
}

private suspend fun suspendWithDefaultParameters(
  p1: String = "defaultP1",
  p2: Int = 100,
  p3: suspend () -> Any = { suspendCoroutine { continuation -> continuation.resume("defaultP3") } },
): String {
  val p3Result = p3()
  return suspendCoroutine { continuation -> continuation.resume("$p1-$p2-${p3Result}") }
}

private fun testDefaultParametersInCoroutines() {
  // All defaults
  startCoroutine { suspendWithDefaultParameters() }.assertSuccess("defaultP1-100-defaultP3")
  // Override first (positional)
  startCoroutine { suspendWithDefaultParameters("newP1") }.assertSuccess("newP1-100-defaultP3")
  // Override with named argument
  startCoroutine { suspendWithDefaultParameters(p2 = 200) }.assertSuccess("defaultP1-200-defaultP3")
  // Override all (positional)
  startCoroutine { suspendWithDefaultParameters("allNewP1", 300, { "allNewP3Value" }) }
    .assertSuccess("allNewP1-300-allNewP3Value")
  // Override all (named, different order)
  startCoroutine { suspendWithDefaultParameters(p3 = { "orderP3Value" }, p1 = "orderP1", p2 = 400) }
    .assertSuccess("orderP1-400-orderP3Value")

  // Test p3 with a suspending lambda block
  startCoroutine {
      suspendWithDefaultParameters(p1 = "suspendP1", p2 = 600, p3 = { it.suspendCoroutine() })
    }
    .resume("p3SuspendedValueResumed")
    .assertSuccess("suspendP1-600-p3SuspendedValueResumed")
}

private suspend fun suspendInt(value: Int): Int = value

private suspend fun collectVarargs(vararg items: Any): List<Any> = items.toList()

private fun testCoroutineWithVarargs() {
  startCoroutine {
      val suspendResultsArray = arrayOf(it.suspendCoroutine(), suspendInt(20))
      collectVarargs(suspendInt(0), *suspendResultsArray, it.suspendCoroutine())
    }
    .resume(10)
    .resume(30)
    .assertSuccess(listOf(0, 10, 20, 30))
}

private fun testCoroutineInterception() {
  val eventFlow: ArrayList<String> = arrayListOf()
  val continuationInterceptor =
    object : ContinuationInterceptor {
      override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        eventFlow.add("interceptContinuation")

        return Continuation<T>(continuation.context) {
          eventFlow.add("interceptedResumeWithCalled")
          continuation.resumeWith(it)
        }
      }

      override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor
    }
  var continuation: Continuation<Unit>? = null

  suspend {
      eventFlow.add("suspendFunctionCalled")
      suspendCoroutine { cont: Continuation<Unit> ->
        eventFlow.add("coroutineSuspended")
        continuation = cont
      }
      eventFlow.add("suspendFunctionDone")
      Unit
    }
    .startCoroutine(
      Continuation<Unit>(continuationInterceptor) { eventFlow.add("completionCalled") }
    )

  continuation!!.resumeWith(Result.success(Unit))

  assertEquals(
    arrayOf(
      "interceptContinuation",
      "interceptedResumeWithCalled",
      "suspendFunctionCalled",
      "coroutineSuspended",
      "interceptedResumeWithCalled",
      "suspendFunctionDone",
      "completionCalled",
    ),
    eventFlow.toArray(),
  )
}
