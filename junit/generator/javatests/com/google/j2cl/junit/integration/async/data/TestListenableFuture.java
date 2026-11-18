/*
 * Copyright 2019 Google Inc.
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.j2cl.junit.async.AsyncTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Integration test used in J2clTestRunnerTest. */
@RunWith(AsyncTestRunner.class)
public class TestListenableFuture {

  @After
  public void after() {
    if (Thread.interrupted()) {
      throw new AssertionError("should_not_be_in_log: the timeout interrupt leaked");
    }
  }

  @Test(timeout = 200L)
  public ListenableFuture<Object> testListenableFuture() {
    return Futures.immediateFuture(null);
  }

  @Test(timeout = 200L)
  public ListenableFuture<Object> testListenableFuture_fail() {
    return Futures.immediateFailedFuture(new Exception("Should see this message"));
  }

  @Test(timeout = 200L)
  public ListenableFuture<Object> testListenableFuture_returnsNull() {
    return null;
  }

  @Test(timeout = 50L)
  public ListenableFuture<Object> testListenableFuture_timeout() {
    // Expected to time out since the returned future never completes.
    return SettableFuture.create();
  }

  @Test(timeout = 50L)
  public ListenableFuture<Object> testListenableFuture_timeoutBeforeReturningFuture()
      throws Exception {
    // Expected to time out since it blocks forever before returning its future.
    try {
      return Futures.immediateFuture(SettableFuture.create().get());
    } catch (InterruptedException e) {
      // Restore the interrupt status like blocking helpers often do. It must not leak into after().
      Thread.currentThread().interrupt();
      throw e;
    }
  }
}
