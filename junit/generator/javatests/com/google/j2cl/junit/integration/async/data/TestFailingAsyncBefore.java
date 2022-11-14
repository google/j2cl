/*
 * Copyright 2015 Google Inc.
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
package com.google.j2cl.junit.integration.async.data;

import com.google.j2cl.junit.async.AsyncTestRunner;
import com.google.j2cl.junit.async.Timeout;
import com.google.j2cl.junit.integration.testing.async.Thenable;
import com.google.j2cl.junit.integration.testing.async.Timer;
import com.google.j2cl.junit.integration.testing.testlogger.TestCaseLogger;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * TestCase used for integration testing for j2cl JUnit support.
 *
 * <p>Note this test will not pass and this is intentional since we want to test test failures in
 * our integration tests as well.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AsyncTestRunner.class)
public class TestFailingAsyncBefore {

  private static boolean first = true;

  @Timeout(200L)
  @Before
  public Thenable before() {
    return (onFulfilled, onRejected) ->
        Timer.schedule(
            () -> {
              TestCaseLogger.log("before");
              if (first) {
                first = false;
                onRejected.execute(new RuntimeException("failure in before()"));
              } else {
                onFulfilled.execute(null);
              }
            },
            0);
  }

  @Test
  public void test() {
    TestCaseLogger.log("should_not_be_in_log");
  }

  @Test
  public void testOther() {
    TestCaseLogger.log("testOther");
  }
}
