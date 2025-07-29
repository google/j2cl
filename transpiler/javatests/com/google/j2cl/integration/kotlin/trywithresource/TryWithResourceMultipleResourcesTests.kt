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
import java.util.ArrayList
import trywithresource.FailableResource.FailureMode

object TryWithResourceMultipleResourcesTests {

  fun run() {
    testTryWithResourceMultipleResourcesCorrectOrder()
    testTryWithResourceMultipleResourcesFailures()
  }

  /**
   * The resources must be opened from left to right and closed in the reverse order that they were
   * opened in. If a resource fails to open then the resources opened before it need to be closed.
   */
  private fun testTryWithResourceMultipleResourcesCorrectOrder() {
    val orderLog = ArrayList<String>()
    val r1 = FailableResource("r1", orderLog, FailureMode.None)
    val r2 = FailableResource("r2", orderLog, FailureMode.None)
    try {
      r1.use {
        r2.use {
          FailableResource("r3", orderLog, FailureMode.None).use { orderLog.add("try block") }
        }
      }
    } catch (e: Exception) {
      fail("Should not have throw any exceptions.")
    }

    assertTrue(orderLog[0] == "r1 OnConstruction")
    assertTrue(orderLog[1] == "r2 OnConstruction")
    assertTrue(orderLog[2] == "r3 OnConstruction")
    assertTrue(orderLog[3] == "try block")
    assertTrue(orderLog[4] == "r3 OnClose")
    assertTrue(orderLog[5] == "r2 OnClose")
    assertTrue(orderLog[6] == "r1 OnClose")
  }

  private fun testTryWithResourceMultipleResourcesFailures() {
    testTryWithResourceMultipleResourcesIntializationFails()
    testTryWithResourceMultipleResourcesTryFails()
    testTryWithResourceMultipleResourcesCloseFails()
  }

  /**
   * If the initialization of every resource completes normally, and the try block completes
   * normally, then:
   */
  private fun testTryWithResourceMultipleResourcesCloseFails() {
    // If more than one automatic closing of an initialized resource completes abruptly because of
    // throws of values V1...Vn, then the try-with-resources statement completes abruptly because
    // of a throw of the value V1 with any remaining values V2...Vn added to the suppressed
    // exception list of V1 (where V1 is the exception from the rightmost resource failing to
    // close and Vn is the exception from the leftmost resource failing to close).
    val orderLog = ArrayList<String>()
    try {
      FailableResource("r1", orderLog, FailureMode.OnClose).use {
        FailableResource("r2", orderLog, FailureMode.OnClose).use {
          FailableResource("r3", orderLog, FailureMode.OnClose).use { orderLog.add("try block") }
        }
      }
      fail("Should have throw exception.")
    } catch (e: Exception) {
      assertTrue(e.message == "OnClose")
      assertTrue(e.suppressed.size == 2)
      assertTrue(e.suppressed[0].message == "OnClose")
      assertTrue(e.suppressed[1].message == "OnClose")
    }

    assertTrue(orderLog[0] == "r1 OnConstruction")
    assertTrue(orderLog[1] == "r2 OnConstruction")
    assertTrue(orderLog[2] == "r3 OnConstruction")
    assertTrue(orderLog[3] == "try block")
    assertTrue(orderLog[4] == "r3 throw OnClose")
    assertTrue(orderLog[5] == "r2 throw OnClose")
    assertTrue(orderLog[6] == "r1 throw OnClose")
  }

  /**
   * If the initialization of all resources completes normally, and the try block completes abruptly
   * because of a throw of a value V, then:
   */
  private fun testTryWithResourceMultipleResourcesTryFails() {
    // If the automatic closings of all initialized resources complete normally, then the
    // try-with-resources statement completes abruptly because of a throw of the value V.
    run {
      val orderLog = ArrayList<String>()
      try {
        FailableResource("r1", orderLog, FailureMode.None).use {
          FailableResource("r2", orderLog, FailureMode.None).use { throw Exception("try") }
        }
        fail("Should have throw exception.")
      } catch (e: Exception) {
        assertTrue(e.message == "try")
      }

      assertTrue(orderLog[0] == "r1 OnConstruction")
      assertTrue(orderLog[1] == "r2 OnConstruction")
      assertTrue(orderLog[2] == "r2 OnClose")
      assertTrue(orderLog[3] == "r1 OnClose")
    }

    // If the automatic closings of one or more initialized resources complete abruptly because of
    // throws of values V1...Vn, then the try-with-resources statement completes abruptly because
    // of a throw of the value V with any remaining values V1...Vn added to the suppressed
    // exception list of V.
    run {
      val orderLog = ArrayList<String>()
      try {
        FailableResource("r1", orderLog, FailureMode.OnClose).use {
          FailableResource("r2", orderLog, FailureMode.OnClose).use {
            FailableResource("r3", orderLog, FailureMode.OnClose).use {
              orderLog.add("try block")
              throw Exception("try")
            }
          }
        }
        fail("Should have throw exception.")
      } catch (e: Exception) {
        assertTrue(e.message == "try")
        assertTrue(e.suppressed.size == 3)
        assertTrue(e.suppressed[0].message == "OnClose")
        assertTrue(e.suppressed[1].message == "OnClose")
        assertTrue(e.suppressed[2].message == "OnClose")
      }

      assertTrue(orderLog[0] == "r1 OnConstruction")
      assertTrue(orderLog[1] == "r2 OnConstruction")
      assertTrue(orderLog[2] == "r3 OnConstruction")
      assertTrue(orderLog[3] == "try block")
      assertTrue(orderLog[4] == "r3 throw OnClose")
      assertTrue(orderLog[5] == "r2 throw OnClose")
      assertTrue(orderLog[6] == "r1 throw OnClose")
    }
  }

  /**
   * If the initialization of a resource completes abruptly because of a throw of a value V, then:
   */
  private fun testTryWithResourceMultipleResourcesIntializationFails() {
    // If the automatic closings of all successfully initialized resources (possibly zero) complete
    // normally, then the try-with-resources statement completes abruptly because of a throw of the
    // value V.
    run {
      val orderLog = ArrayList<String>()
      try {
        FailableResource("r1", orderLog, FailureMode.None).use {
          FailableResource("r2", orderLog, FailureMode.OnConstruction).use {
            fail("Should have throw exception.")
          }
        }
        fail("Should have throw exception.")
      } catch (e: Exception) {
        assertTrue(e.message == "OnConstruction")
      }

      assertTrue(orderLog[0] == "r1 OnConstruction")
      assertTrue(orderLog[1] == "r2 throw OnConstruction")
      assertTrue(orderLog[2] == "r1 OnClose")
    }

    // If the automatic closings of all successfully initialized resources (possibly zero) complete
    // abruptly because of throws of values V1...Vn, then the try-with-resources statement completes
    // abruptly because of a throw of the value V with any remaining values V1...Vn added to the
    // suppressed exception list of V.
    run {
      val orderLog = ArrayList<String>()
      try {
        FailableResource("r1", orderLog, FailureMode.OnClose).use {
          FailableResource("r2", orderLog, FailureMode.OnConstruction).use {
            fail("Should have throw exception.")
          }
        }
        fail("Should have throw exception.")
      } catch (e: Exception) {
        assertTrue(e.message == "OnConstruction")
      }

      assertTrue(orderLog[0] == "r1 OnConstruction")
      assertTrue(orderLog[1] == "r2 throw OnConstruction")
      assertTrue(orderLog[2] == "r1 throw OnClose")
    }

    // The first 2 fail to close the 3rd fails to open.
    run {
      val orderLog = ArrayList<String>()
      try {
        FailableResource("r1", orderLog, FailureMode.OnClose).use {
          FailableResource("r2", orderLog, FailureMode.OnClose).use {
            FailableResource("r3", orderLog, FailureMode.OnConstruction).use {
              fail("Should have throw exception.")
            }
          }
        }
        fail("Should have throw exception.")
      } catch (e: Exception) {
        assertTrue(e.message == "OnConstruction")
        assertTrue(e.suppressed.size == 2)
        assertTrue(e.suppressed[0].message == "OnClose")
        assertTrue(e.suppressed[1].message == "OnClose")
      }

      assertTrue(orderLog[0] == "r1 OnConstruction")
      assertTrue(orderLog[1] == "r2 OnConstruction")
      assertTrue(orderLog[2] == "r3 throw OnConstruction")
      assertTrue(orderLog[3] == "r2 throw OnClose")
      assertTrue(orderLog[4] == "r1 throw OnClose")
    }

    // The first 1 fails to close the 3rd fails to open.
    run {
      val orderLog = ArrayList<String>()
      try {
        FailableResource("r1", orderLog, FailureMode.OnClose).use {
          FailableResource("r2", orderLog, FailureMode.None).use {
            FailableResource("r3", orderLog, FailureMode.OnConstruction).use {
              fail("Should have throw exception.")
            }
          }
        }
        fail("Should have throw exception.")
      } catch (e: Exception) {
        assertTrue(e.message == "OnConstruction")
        assertTrue(e.suppressed.size == 1)
        assertTrue(e.suppressed[0].message == "OnClose")
      }

      assertTrue(orderLog[0] == "r1 OnConstruction")
      assertTrue(orderLog[1] == "r2 OnConstruction")
      assertTrue(orderLog[2] == "r3 throw OnConstruction")
      assertTrue(orderLog[3] == "r2 OnClose")
      assertTrue(orderLog[4] == "r1 throw OnClose")
    }
  }
}
