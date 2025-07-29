/*
 * Copyright 2023 Google Inc.
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
package trywithresource

import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail
import trywithresource.FailableResource.FailureMode

object TryWithResourceSingleResourceTests {
  /**
   * In a basic try-with-resources statement that manages a single resource:
   *
   * The try with resource can throw an exception at the resource initialization stage, the try
   * block stage, or the resource close stage, so we test all combinations.
   */
  fun run() {
    testTryWithResourceSingleResourceInitializationFails()
    testTryWithResourceSingleResourceTryFails()
    testTryWithResourceSingleResourceCloseFails()
  }

  /**
   * If the initialization of the resource completes abruptly because of a throw of a value V, then
   * the try-with-resources statement completes abruptly because of a throw of the value V.
   */
  private fun testTryWithResourceSingleResourceInitializationFails() {
    var numCaught = 0
    try {
      testSingleResource(FailureMode.OnConstruction, false)
      fail("Should have throw exception.")
    } catch (e: Exception) {
      assertTrue("Should fail on construction", e.message == "OnConstruction")
      numCaught++
    }

    try {
      testSingleResource(FailureMode.OnConstruction, true)
      fail("Should have throw exception.")
    } catch (e: Exception) {
      assertTrue("Should fail on construction", e.message == "OnConstruction")
      numCaught++
    }

    assertTrue("Should have thrown an exception in every case.", numCaught == 2)
  }

  /**
   * If the initialization of the resource completes normally, and the try block completes abruptly
   * because of a throw of a value V, then:
   */
  private fun testTryWithResourceSingleResourceTryFails() {
    // If the automatic closing of the resource completes normally, then the try-with-resources
    // statement completes abruptly because of a throw of the value V.
    var numCaught = 0
    try {
      testSingleResource(FailureMode.None, true)
      fail("Should have throw exception.")
    } catch (e: Exception) {
      assertTrue("Should fail to complete try block.", e.message == "try")
      numCaught++
    }

    assertTrue("Should have thrown an exception.", numCaught == 1)

    // If the automatic closing of the resource completes abruptly because of a throw of a value V2,
    // then the try-with-resources statement completes abruptly because of a throw of value V with
    // V2 added to the suppressed exception list of V.
    try {
      testSingleResource(FailureMode.OnClose, true)
      fail("Should have throw exception.")
    } catch (e: Exception) {
      assertTrue("Should fail to complete try block.", e.message == "try")
      assertTrue(e.suppressed.size == 1)
      assertTrue(e.suppressed[0].message == "OnClose")
      numCaught++
    }

    assertTrue("Should have thrown an exception in every case.", numCaught == 2)
  }

  /**
   * If the initialization of the resource completes normally, and the try block completes normally,
   * and the automatic closing of the resource completes abruptly because of a throw of a value V,
   * then the try-with-resources statement completes abruptly because of a throw of the value V.
   */
  private fun testTryWithResourceSingleResourceCloseFails() {
    var numCaught = 0
    try {
      testSingleResource(FailureMode.OnClose, false)
      fail("Should have throw exception.")
    } catch (e: Exception) {
      assertTrue("Should fail to close resource.", e.message == "OnClose")
      numCaught++
    }

    assertTrue("Should have thrown an exception.", numCaught == 1)
  }

  private fun testSingleResource(failureMode: FailureMode, tryBlockFails: Boolean) {
    FailableResource("name", ArrayList<String>(), failureMode).use {
      if (tryBlockFails) {
        throw Exception("try")
      }
    }
  }
}
