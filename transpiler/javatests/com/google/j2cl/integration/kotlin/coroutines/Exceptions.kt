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
@file:Suppress("SuspendInFinally")

package coroutines

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.fail
import kotlin.coroutines.suspendCoroutine

fun testCoroutineWithExceptions() {
  testUncaughtException()
  testUncaughtExceptionOnSuspension()
  testResumeWithException()
  testUncaughtExceptionAfterResume()
  testNoExceptionFinallyExecuted()
  testUncaughtExceptionFinallyReturns()
  testCaughtExceptionAfterResume()
  testCaughtExceptionOnSuspension()
  testCaughtExceptionFinallyExecuted()
  testCatchThrowsExceptionFinallyExecuted()
  testFinallyThrowsException()
  // TODO(b/421721846): Re-enable this test once the bug in JS Compiler is fixed.
  // testFinallyReturnCancelledByOuterBreak()
  testFinallyReturnCancelledByOuterContinue()
  testFinallyReturnSupersededByOuterReturn()
  testFinallyExceptionSupersededByOuterException()
  testFinallyExceptionCancelledByOuterBreak()
}

// Use custom exceptions to be sure that the correct exception is thrown when crossing Kotlin/Js
// boundary.
private object CustomException : RuntimeException("Test exception")

private object TryException : RuntimeException("Try exception")

private object CatchException : RuntimeException("Catch exception")

private object FinallyException : RuntimeException("Finally exception")

private fun testUncaughtException() {
  startCoroutine { throw CustomException }.assertFailure(CustomException)
}

@Suppress("SuspendCoroutine")
private fun testUncaughtExceptionOnSuspension() {
  startCoroutine {
      suspendCoroutine<Unit> { c -> throw CustomException }
      fail("unreachable")
    }
    .assertFailure(CustomException)
}

private fun testResumeWithException() {
  startCoroutine {
      it.suspendCoroutine()
      fail("unreachable")
    }
    .resumeWithException(CustomException)
    .assertFailure(CustomException)
}

private fun testUncaughtExceptionAfterResume() {
  startCoroutine {
      it.suspendCoroutine()
      throw CustomException
    }
    .resume(Unit)
    .assertFailure(CustomException)
}

private fun testNoExceptionFinallyExecuted() {
  var captureFinally = ""
  startCoroutine {
      var result = ""
      try {
        try {
          result += "inner_try_${it.suspendCoroutine()}"
        } finally {
          result += "inner_finally_${it.suspendCoroutine()}"
        }
        result += "outer_try_${it.suspendCoroutine()}"
        return@startCoroutine result
      } finally {
        result += "outer_finally_${it.suspendCoroutine()}"
        captureFinally = result
      }
    }
    .resume("OK_1")
    .resume("OK_2")
    .resume("OK_3")
    .resume("OK_4")
    .assertSuccess("inner_try_OK_1inner_finally_OK_2outer_try_OK_3")

  assertEquals("inner_try_OK_1inner_finally_OK_2outer_try_OK_3outer_finally_OK_4", captureFinally)
}

private fun testUncaughtExceptionFinallyReturns() {
  startCoroutine {
      var result = ""
      try {
        result += "try_${it.suspendCoroutine()}"
        fail("unreachable")
      } finally {
        result += "finally_${it.suspendCoroutine()}"
        return@startCoroutine result
      }
      fail("unreachable")
    }
    .resumeWithException(CustomException)
    .resume("OK_1")
    .assertSuccess("finally_OK_1")
}

private fun testCaughtExceptionAfterResume() {
  startCoroutine {
      var result = ""
      try {
        it.suspendCoroutine()
      } catch (e: RuntimeException) {
        assertEquals(TryException, e)
        result = "catch_OK"
      }
      result
    }
    .resumeWithException(TryException)
    .assertSuccess("catch_OK")
}

@Suppress("SuspendCoroutine")
private fun testCaughtExceptionOnSuspension() {
  startCoroutine {
      var result = ""
      try {
        suspendCoroutine<Unit> { c -> throw CustomException }
      } catch (e: RuntimeException) {
        assertEquals(CustomException, e)
        result += "catch_OK"
      }
      result
    }
    .assertSuccess("catch_OK")
}

private fun testCaughtExceptionFinallyExecuted() {
  startCoroutine {
      var result = ""
      try {
        result += "try_${it.suspendCoroutine()}"
        fail("unreachable")
      } catch (e: TryException) {
        assertEquals(TryException, e)
        result += "catch_${it.suspendCoroutine()}"
      } finally {
        result += "finally_${it.suspendCoroutine()}"
      }
      result
    }
    .resumeWithException(TryException)
    .resume("OK_1")
    .resume("OK_2")
    .assertSuccess("catch_OK_1finally_OK_2")
}

private fun testCatchThrowsExceptionFinallyExecuted() {
  var accumulatedSteps = ""
  startCoroutine {
      try {
        accumulatedSteps += "try_${it.suspendCoroutine()}"
        fail("unreachable")
      } catch (e: TryException) {
        assertEquals(TryException, e)
        accumulatedSteps += "catch_${it.suspendCoroutine()}"
        fail("unreachable")
      } finally {
        accumulatedSteps += "finally_${it.suspendCoroutine()}"
      }
      fail("unreachable")
    }
    .resumeWithException(TryException)
    .resumeWithException(CatchException)
    .resume("OK_1")
    .assertFailure(CatchException)
  assertEquals("finally_OK_1", accumulatedSteps)
}

private fun testFinallyThrowsException() {
  var accumulatedSteps = ""
  startCoroutine {
      try {
        accumulatedSteps += it.suspendCoroutine()
      } finally {
        accumulatedSteps += it.suspendCoroutine()
        fail("unreachable")
      }
      fail("unreachable")
    }
    .resume("try_OK")
    .resumeWithException(FinallyException)
    .assertFailure(FinallyException)
  assertEquals("try_OK", accumulatedSteps)
}

private fun testFinallyReturnCancelledByOuterBreak() {
  startCoroutine {
      var result = ""
      try {
        OUT@ do {
          try {
            result += "inner_try_${it.suspendCoroutine()}"
            return@startCoroutine "innerTry"
          } finally {
            result += "inner_finally_${it.suspendCoroutine()}"
            break@OUT
          }
        } while (false)
        result += "outer_try_${it.suspendCoroutine()}"
      } finally {
        result += "outer_finally_${it.suspendCoroutine()}"
      }
      result += "end_${it.suspendCoroutine()}"
      return@startCoroutine result
    }
    .resume("OK_1")
    .resume("OK_2")
    .resume("OK_3")
    .resume("OK_4")
    .resume("OK_5")
    .assertSuccess("inner_try_OK_1inner_finally_OK_2outer_try_OK_3outer_finally_OK_4end_OK_5")
}

private fun testFinallyReturnCancelledByOuterContinue() {
  var accumulatedSteps = ""
  startCoroutine {
      for (i in 0..1) {
        if (i == 1) {
          return@startCoroutine "topOfLoop"
        }
        try {
          try {
            accumulatedSteps += "innerTry_${it.suspendCoroutine()}"
            return@startCoroutine "innerTry"
          } finally {
            accumulatedSteps += "innerFinally_${it.suspendCoroutine()}"
          }
          fail("unreachable")
        } finally {
          accumulatedSteps += "outerFinally_${it.suspendCoroutine()}"
          continue
        }
        fail("unreachable")
      }
      return@startCoroutine "end"
    }
    .resume("OK_1")
    .resume("OK_2")
    .resume("OK_3")
    .assertSuccess("topOfLoop")

  assertEquals("innerTry_OK_1innerFinally_OK_2outerFinally_OK_3", accumulatedSteps)
}

private fun testFinallyReturnSupersededByOuterReturn() {
  var accumulatedSteps = ""
  startCoroutine {
      try {
        try {
          accumulatedSteps += "inner_try_${it.suspendCoroutine()}"
          return@startCoroutine "innerTry"
        } finally {
          accumulatedSteps += "inner_finally_${it.suspendCoroutine()}"
        }
        fail("unreachable")
      } finally {
        accumulatedSteps += "outer_finally_${it.suspendCoroutine()}"
        return@startCoroutine "outerFinally"
      }
      fail("unreachable")
      return@startCoroutine "end"
    }
    .resume("OK_1")
    .resume("OK_2")
    .resume("OK_3")
    .assertSuccess("outerFinally")

  assertEquals("inner_try_OK_1inner_finally_OK_2outer_finally_OK_3", accumulatedSteps)
}

private fun testFinallyExceptionSupersededByOuterException() {
  var accumulatedSteps = ""
  startCoroutine {
      try {
        try {
          accumulatedSteps += "inner_try_${it.suspendCoroutine()}"
        } finally {
          accumulatedSteps += "inner_finally_${it.suspendCoroutine()}"
        }
        fail("unreachable")
      } finally {
        accumulatedSteps += "outer_finally_${it.suspendCoroutine()}"
      }
      fail("unreachable")
    }
    .resumeWithException(TryException)
    .resume("OK_1")
    .resumeWithException(FinallyException)
    .assertFailure(FinallyException)

  assertEquals("inner_finally_OK_1", accumulatedSteps)
}

private fun testFinallyExceptionCancelledByOuterBreak() {
  var accumulatedSteps = ""
  startCoroutine {
      OUT@ do {
        try {
          try {
            accumulatedSteps += "inner_try_${it.suspendCoroutine()}"
            return@startCoroutine "innerTry"
          } finally {
            accumulatedSteps += "inner_finally_${it.suspendCoroutine()}"
          }
          // This should not be executed. We cannot use fail() here because the error it may throw
          // will be caught by the outer try block and cancelled by the break.
          accumulatedSteps += "unreachable_${it.suspendCoroutine()}"
        } finally {
          accumulatedSteps += "outer_finally_${it.suspendCoroutine()}"
          break@OUT
        }
        fail("unreachable")
      } while (false)
      accumulatedSteps += "end_${it.suspendCoroutine()}"
      return@startCoroutine "end"
    }
    .resumeWithException(TryException)
    .resume("OK_1")
    .resume("OK_2")
    .resume("OK_3")
    .assertSuccess("end")

  assertEquals("inner_finally_OK_1outer_finally_OK_2end_OK_3", accumulatedSteps)
}
