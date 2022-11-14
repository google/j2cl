/*
 * Copyright 2020 Google Inc.
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
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AsyncTestRunner.class)
public class TestMethodOrder {

  @Timeout(200L)
  @Before
  public Thenable before1() {
    return asyncLog("before1");
  }

  @Before
  public void before2() {
    TestCaseLogger.log("before2");
  }

  @Timeout(200L)
  @Before
  public Thenable before3() {
    return asyncLog("before3");
  }

  @Timeout(200L)
  @After
  public Thenable after1() {
    return asyncLog("after1");
  }

  @After
  public void after2() {
    TestCaseLogger.log("after2");
  }

  @Timeout(200L)
  @After
  public Thenable after3() {
    return asyncLog("after3");
  }

  @Test(timeout = 200L)
  public Thenable test() {
    return asyncLog("test");
  }

  private static Thenable asyncLog(String logMsg) {
    return (onFulfilled, onRejected) ->
        Timer.schedule(
            () -> {
              TestCaseLogger.log(logMsg);
              onFulfilled.execute(null);
            },
            10);
  }
}
