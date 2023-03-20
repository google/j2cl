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
import java.util.List;
import trywithresource.FailableResource.FailureMode;

public class TryWithResourceMultipleResourcesTests {

  public static void run() {
    testTryWithResourceMultipleResourcesCorrectOrder();
    testTryWithResourceMultipleResourcesFailures();
  }

  /**
   * The resources must be opened from left to right and closed in the reverse order that they were
   * opened in. If a resource fails to open then the resources opened before it need to be closed.
   */
  private static void testTryWithResourceMultipleResourcesCorrectOrder() {
    List<String> orderLog = new ArrayList<>();
    FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.None);
    FailableResource r2 = new FailableResource("r2", orderLog, FailureMode.None);
    try (r1;
        r2;
        FailableResource r3 = new FailableResource("r3", orderLog, FailureMode.None)) {
      orderLog.add("try block");
    } catch (Exception e) {
      fail("Should not have throw any exceptions.");
    }
    assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
    assertTrue(orderLog.get(1).equals("r2 OnConstruction"));
    assertTrue(orderLog.get(2).equals("r3 OnConstruction"));
    assertTrue(orderLog.get(3).equals("try block"));
    assertTrue(orderLog.get(4).equals("r3 OnClose"));
    assertTrue(orderLog.get(5).equals("r2 OnClose"));
    assertTrue(orderLog.get(6).equals("r1 OnClose"));
  }

  private static void testTryWithResourceMultipleResourcesFailures() {
    testTryWithResourceMultipleResourcesIntializationFails();
    testTryWithResourceMultipleResourcesTryFails();
    testTryWithResourceMultipleResourcesCloseFails();
  }

  /**
   * If the initialization of every resource completes normally, and the try block completes
   * normally, then:
   */
  private static void testTryWithResourceMultipleResourcesCloseFails() {
    // If more than one automatic closing of an initialized resource completes abruptly because of
    // throws of values V1...Vn, then the try-with-resources statement completes abruptly because
    // of a throw of the value V1 with any remaining values V2...Vn added to the suppressed
    // exception list of V1 (where V1 is the exception from the rightmost resource failing to
    // close and Vn is the exception from the leftmost resource failing to close).
    List<String> orderLog = new ArrayList<>();
    try {
      try (FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.OnClose);
          FailableResource r2 = new FailableResource("r2", orderLog, FailureMode.OnClose);
          FailableResource r3 = new FailableResource("r3", orderLog, FailureMode.OnClose)) {
        orderLog.add("try block");
      }
      fail("Should have throw exception.");
    } catch (Exception e) {
      assertTrue(e.getMessage().equals("OnClose"));
      assertTrue(e.getSuppressed().length == 2);
      assertTrue(e.getSuppressed()[0].getMessage().equals("OnClose"));
      assertTrue(e.getSuppressed()[1].getMessage().equals("OnClose"));
    }
    assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
    assertTrue(orderLog.get(1).equals("r2 OnConstruction"));
    assertTrue(orderLog.get(2).equals("r3 OnConstruction"));
    assertTrue(orderLog.get(3).equals("try block"));
    assertTrue(orderLog.get(4).equals("r3 throw OnClose"));
    assertTrue(orderLog.get(5).equals("r2 throw OnClose"));
    assertTrue(orderLog.get(6).equals("r1 throw OnClose"));
  }

  /**
   * If the initialization of all resources completes normally, and the try block completes
   * abruptly because of a throw of a value V, then:
   */
  private static void testTryWithResourceMultipleResourcesTryFails() {
    // If the automatic closings of all initialized resources complete normally, then the
    // try-with-resources statement completes abruptly because of a throw of the value V.
    {
      List<String> orderLog = new ArrayList<>();
      try {
        try (FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.None);
            FailableResource r2 = new FailableResource("r2", orderLog, FailureMode.None)) {
          throw new Exception("try");
        }
      } catch (Exception e) {
        assertTrue(e.getMessage().equals("try"));
      }
      assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
      assertTrue(orderLog.get(1).equals("r2 OnConstruction"));
      assertTrue(orderLog.get(2).equals("r2 OnClose"));
      assertTrue(orderLog.get(3).equals("r1 OnClose"));
    }

    // If the automatic closings of one or more initialized resources complete abruptly because of
    // throws of values V1...Vn, then the try-with-resources statement completes abruptly because
    // of a throw of the value V with any remaining values V1...Vn added to the suppressed
    // exception list of V.
    {
      List<String> orderLog = new ArrayList<>();
      try {
        try (FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.OnClose);
            FailableResource r2 = new FailableResource("r2", orderLog, FailureMode.OnClose);
            FailableResource r3 = new FailableResource("r3", orderLog, FailureMode.OnClose)) {
          orderLog.add("try block");
          throw new Exception("try");
        }
      } catch (Exception e) {
        assertTrue(e.getMessage().equals("try"));
        assertTrue(e.getSuppressed().length == 3);
        assertTrue(e.getSuppressed()[0].getMessage().equals("OnClose"));
        assertTrue(e.getSuppressed()[1].getMessage().equals("OnClose"));
        assertTrue(e.getSuppressed()[2].getMessage().equals("OnClose"));
      }
      assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
      assertTrue(orderLog.get(1).equals("r2 OnConstruction"));
      assertTrue(orderLog.get(2).equals("r3 OnConstruction"));
      assertTrue(orderLog.get(3).equals("try block"));
      assertTrue(orderLog.get(4).equals("r3 throw OnClose"));
      assertTrue(orderLog.get(5).equals("r2 throw OnClose"));
      assertTrue(orderLog.get(6).equals("r1 throw OnClose"));
    }
  }

  /**
   * If the initialization of a resource completes abruptly because of a throw of a value V, then:
   */
  private static void testTryWithResourceMultipleResourcesIntializationFails() {
    // If the automatic closings of all successfully initialized resources (possibly zero) complete
    // normally, then the try-with-resources statement completes abruptly because of a throw of the
    // value V.
    {
      List<String> orderLog = new ArrayList<>();
      try {
        try (FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.None);
            FailableResource r2 =
                new FailableResource("r2", orderLog, FailureMode.OnConstruction)) {
          fail("Should have throw exception.");
        }
      } catch (Exception e) {
        assertTrue(e.getMessage().equals("OnConstruction"));
      }
      assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
      assertTrue(orderLog.get(1).equals("r2 throw OnConstruction"));
      assertTrue(orderLog.get(2).equals("r1 OnClose"));
    }

    // If the automatic closings of all successfully initialized resources (possibly zero) complete
    // abruptly because of throws of values V1...Vn, then the try-with-resources statement completes
    // abruptly because of a throw of the value V with any remaining values V1...Vn added to the
    // suppressed exception list of V.
    {
      List<String> orderLog = new ArrayList<>();
      try {
        try (FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.OnClose);
            FailableResource r2 =
                new FailableResource("r2", orderLog, FailureMode.OnConstruction)) {
          fail("Should have throw exception.");
        }
      } catch (Exception e) {
        assertTrue(e.getMessage().equals("OnConstruction"));
      }
      assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
      assertTrue(orderLog.get(1).equals("r2 throw OnConstruction"));
      assertTrue(orderLog.get(2).equals("r1 throw OnClose"));
    }

    // The first 2 fail to close the 3rd fails to open.
    {
      List<String> orderLog = new ArrayList<>();
      try {
        try (FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.OnClose);
            FailableResource r2 = new FailableResource("r2", orderLog, FailureMode.OnClose);
            FailableResource r3 =
                new FailableResource("r3", orderLog, FailureMode.OnConstruction)) {
          fail("Should have throw exception.");
        }
      } catch (Exception e) {
        assertTrue(e.getMessage().equals("OnConstruction"));
        assertTrue(e.getSuppressed().length == 2);
        assertTrue(e.getSuppressed()[0].getMessage().equals("OnClose"));
        assertTrue(e.getSuppressed()[1].getMessage().equals("OnClose"));
      }
      assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
      assertTrue(orderLog.get(1).equals("r2 OnConstruction"));
      assertTrue(orderLog.get(2).equals("r3 throw OnConstruction"));
      assertTrue(orderLog.get(3).equals("r2 throw OnClose"));
      assertTrue(orderLog.get(4).equals("r1 throw OnClose"));
    }

    // The first 1 fails to close the 3rd fails to open.
    {
      List<String> orderLog = new ArrayList<>();
      try {
        try (FailableResource r1 = new FailableResource("r1", orderLog, FailureMode.OnClose);
            FailableResource r2 = new FailableResource("r2", orderLog, FailureMode.None);
            FailableResource r3 =
                new FailableResource("r3", orderLog, FailureMode.OnConstruction)) {
          fail("Should have throw exception.");
        }
      } catch (Exception e) {
        assertTrue(e.getMessage().equals("OnConstruction"));
        assertTrue(e.getSuppressed().length == 1);
        assertTrue(e.getSuppressed()[0].getMessage().equals("OnClose"));
      }
      assertTrue(orderLog.get(0).equals("r1 OnConstruction"));
      assertTrue(orderLog.get(1).equals("r2 OnConstruction"));
      assertTrue(orderLog.get(2).equals("r3 throw OnConstruction"));
      assertTrue(orderLog.get(3).equals("r2 OnClose"));
      assertTrue(orderLog.get(4).equals("r1 throw OnClose"));
    }
  }
}
