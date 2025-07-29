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
package coroutines

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

internal val noopCompletion: Continuation<Any?> =
  Continuation(EmptyCoroutineContext) { result ->
    // ensure the coroutine was sucessfully completed.
    result.getOrThrow()
  }

internal fun startCoroutine(
  testBlock: suspend (c: ContinuationController<Any>) -> Any
): CoroutineTester {
  val test = CoroutineTester()
  (suspend { testBlock(test.controller) }).startCoroutine(test.completion)
  return test
}

internal fun <R> startCoroutine(
  receiver: R,
  testBlock: suspend R.(c: ContinuationController<Any>) -> Any,
): CoroutineTester {
  val test = CoroutineTester()
  val coroutine: suspend R.() -> Any = { testBlock(test.controller) }
  coroutine.startCoroutine(receiver, test.completion)
  return test
}

internal class CoroutineTester {
  internal val controller = ContinuationController<Any>()
  internal val completion = CapturingContinuation<Any>()

  fun resume(value: Any): CoroutineTester {
    controller.resume(value)
    return this
  }

  fun resumeWithException(exception: Throwable): CoroutineTester {
    controller.resumeWithException(exception)
    return this
  }

  fun assertSuccess(expectedValue: Any? = null) = completion.assertSuccess(expectedValue)

  fun assertFailure(expectedException: Throwable) = completion.assertFailure(expectedException)

  fun assertSuspended(): CoroutineTester {
    assertTrue(controller.suspending)
    return this
  }
}

internal class CapturingContinuation<T>(
  override val context: CoroutineContext = EmptyCoroutineContext
) : Continuation<T> {
  private var capturedResult: Result<T>? = null

  override fun resumeWith(result: Result<T>) {
    assertNull(capturedResult)
    capturedResult = result
  }

  fun assertSuccess(expectedValue: T? = null) {
    assertNotNull(capturedResult)
    assertTrue(capturedResult!!.isSuccess)
    // TODO(dramaix): Coroutine that does not return a value should pass Unit to the completion
    // instead of null. When this bug is fixed remove the default value for expectedValue and this
    // null check.
    if (expectedValue != null) {
      assertEquals(expectedValue, capturedResult!!.getOrNull())
    }
  }

  fun assertFailure(expectedException: Throwable) {
    assertNotNull(capturedResult)
    assertTrue(capturedResult!!.isFailure)
    val exception = capturedResult!!.exceptionOrNull()!!
    assertEquals(expectedException, exception)
  }
}

internal class ContinuationController<T> {
  private var continuation: Continuation<T>? = null

  val suspending: Boolean
    get() = continuation != null

  fun resume(value: T) {
    releaseContinuation().resume(value)
  }

  fun resumeWithException(exception: Throwable) {
    releaseContinuation().resumeWithException(exception)
  }

  private fun releaseContinuation(): Continuation<T> {
    assertNotNull(continuation)
    val c = continuation!!
    continuation = null
    return c
  }

  @Suppress("SuspendCoroutine")
  suspend fun suspendCoroutine(): T {
    return suspendCoroutine<T> {
      assertNull(continuation)
      // capture the continuation for later use.
      continuation = it
    }
  }
}
