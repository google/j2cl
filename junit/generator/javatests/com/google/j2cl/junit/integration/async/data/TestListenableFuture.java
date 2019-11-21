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
import com.google.j2cl.junit.async.AsyncTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Integration test used in J2clTestRunnerTest. */
@RunWith(AsyncTestRunner.class)
public class TestListenableFuture {

  @Test(timeout = 200L)
  public ListenableFuture<Object> testListenableFuture() {
    return Futures.immediateFuture(null);
  }

  @Test(timeout = 200L)
  public ListenableFuture<Object> testListenableFuture_fail() {
    return Futures.immediateFailedFuture(new Exception("Should see this message"));
  }
}
