/*
 * Copyright 2016 Google Inc.
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.j2cl.junit.async.AsyncTestRunner;
import com.google.j2cl.junit.integration.testing.async.Thenable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Integration test used in J2clTestRunnerTest. */
@RunWith(AsyncTestRunner.class)
public class TestTimeOutNotProvided {
  @Before
  public Thenable beforeThenable() {
    return null;
  }

  @Before
  public ListenableFuture<Void> beforeFuture() {
    return null;
  }

  @After
  public Thenable afterThenable() {
    return null;
  }

  @After
  public ListenableFuture<Void> afterFuture() {
    return null;
  }

  @Test
  public Thenable thenableDoesNotHaveTimeout() {
    return null;
  }

  @Test
  public ListenableFuture<Void> futureDoesNotHaveTimeout() {
    return null;
  }
}
