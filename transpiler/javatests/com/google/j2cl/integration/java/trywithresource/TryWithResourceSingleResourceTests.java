/*
 * Copyright 2017 Google Inc.
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
package trywithresource;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import java.util.ArrayList;
import trywithresource.FailableResource.FailureMode;

public class TryWithResourceSingleResourceTests {
  /**
   * In a basic try-with-resources statement that manages a single resource:
   *
   * The try with resource can throw an exception at the resource initialization stage, the try
   * block stage, or the resource close stage, so we test all combinations.
   */
  public static void run() {
    testTryWithResourceSingleResourceInitializationFails();
    testTryWithResourceSingleResourceTryFails();
    testTryWithResourceSingleResourceCloseFails();
  }

  /**
   * If the initialization of the resource completes abruptly because of a throw of a value V,
   * then the try-with-resources statement completes abruptly because of a throw of the value V.
   */
  private static void testTryWithResourceSingleResourceInitializationFails() {
    int numCaught = 0;
    try {
      testSingleResource(FailureMode.OnConstruction, false);
      fail("Should have throw exception.");
    } catch (Exception e) {
      assertTrue("Should fail on construction", e.getMessage().equals("OnConstruction"));
      numCaught++;
    }
    try {
      testSingleResource(FailureMode.OnConstruction, true);
      fail("Should have throw exception.");
    } catch (Exception e) {
      assertTrue("Should fail on construction", e.getMessage().equals("OnConstruction"));
      numCaught++;
    }
    assertTrue("Should have thrown an exception in every case.", numCaught == 2);
  }

  /**
   * If the initialization of the resource completes normally, and the try block completes abruptly
   * because of a throw of a value V, then:
   */
  private static void testTryWithResourceSingleResourceTryFails() {
    // If the automatic closing of the resource completes normally, then the try-with-resources
    // statement completes abruptly because of a throw of the value V.
    int numCaught = 0;
    try {
      testSingleResource(FailureMode.None, true);
      fail("Should have throw exception.");
    } catch (Exception e) {
      assertTrue("Should fail to complete try block.", e.getMessage().equals("try"));
      numCaught++;
    }
    assertTrue("Should have thrown an exception.", numCaught == 1);

    // If the automatic closing of the resource completes abruptly because of a throw of a value V2,
    // then the try-with-resources statement completes abruptly because of a throw of value V with
    // V2 added to the suppressed exception list of V.
    try {
      testSingleResource(FailureMode.OnClose, true);
      fail("Should have throw exception.");
    } catch (Exception e) {
      assertTrue("Should fail to complete try block.", e.getMessage().equals("try"));
      assertTrue(e.getSuppressed().length == 1);
      assertTrue(e.getSuppressed()[0].getMessage().equals("OnClose"));
      numCaught++;
    }
    assertTrue("Should have thrown an exception in every case.", numCaught == 2);
  }

  /**
   * If the initialization of the resource completes normally, and the try block completes normally,
   * and the automatic closing of the resource completes abruptly because of a throw of a value V,
   * then the try-with-resources statement completes abruptly because of a throw of the value V.
   */
  private static void testTryWithResourceSingleResourceCloseFails() {
    int numCaught = 0;
    try {
      testSingleResource(FailureMode.OnClose, false);
      fail("Should have throw exception.");
    } catch (Exception e) {
      assertTrue("Should fail to close resource.", e.getMessage().equals("OnClose"));
      numCaught++;
    }
    assertTrue("Should have thrown an exception.", numCaught == 1);
  }

  private static void testSingleResource(FailureMode failureMode, boolean tryBlockFails)
      throws Exception {
    try (AutoCloseable resource =
            new FailableResource("name", new ArrayList<String>(), failureMode)) {
      if (tryBlockFails) {
        throw new Exception("try");
      }
    }
  }
}
