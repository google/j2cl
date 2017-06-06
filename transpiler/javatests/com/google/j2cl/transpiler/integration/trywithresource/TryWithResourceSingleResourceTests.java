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
package com.google.j2cl.transpiler.integration.trywithresource;

import com.google.j2cl.transpiler.integration.trywithresource.FailableResource.FailureMode;

import java.util.ArrayList;

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
      testSingleResource(FailureMode.Open, false);
    } catch (Exception e) {
      assert e.getMessage().equals("open") : "Should fail to open";
      numCaught++;
    }
    try {
      testSingleResource(FailureMode.Open, true);
    } catch (Exception e) {
      assert e.getMessage().equals("open") : "Should fail to open";
      numCaught++;
    }
    assert numCaught == 2 : "Should have thrown an exception in every case.";
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
    } catch (Exception e) {
      assert e.getMessage().equals("try") : "Should fail to complete try block.";
      numCaught++;
    }
    assert numCaught == 1 : "Should have thrown an exception.";

    // If the automatic closing of the resource completes abruptly because of a throw of a value V2,
    // then the try-with-resources statement completes abruptly because of a throw of value V with
    // V2 added to the suppressed exception list of V.
    try {
      testSingleResource(FailureMode.Close, true);
    } catch (Exception e) {
      assert e.getMessage().equals("try") : "Should fail to complete try block.";
      assert e.getSuppressed().length == 1;
      assert e.getSuppressed()[0].getMessage().equals("close");
      numCaught++;
    }
    assert numCaught == 2 : "Should have thrown an exception in every case.";
  }

  /**
   * If the initialization of the resource completes normally, and the try block completes normally,
   * and the automatic closing of the resource completes abruptly because of a throw of a value V,
   * then the try-with-resources statement completes abruptly because of a throw of the value V.
   */
  private static void testTryWithResourceSingleResourceCloseFails() {
    int numCaught = 0;
    try {
      testSingleResource(FailureMode.Close, false);
    } catch (Exception e) {
      assert e.getMessage().equals("close") : "Should fail to close resource.";
      numCaught++;
    }
    assert numCaught == 1 : "Should have thrown an exception.";
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
