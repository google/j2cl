/*
 * Copyright 2022 Google Inc.
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
package com.google.j2cl.junit.integration.asynckotlintest.data

import com.google.common.util.concurrent.ListenableFuture
import com.google.j2cl.junit.async.AsyncTestRunner
import com.google.j2cl.junit.integration.testing.async.Thenable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith

/** Integration test used in J2clTestRunnerTest. */
@RunWith(AsyncTestRunner::class)
class TestTimeOutNotProvided {
  @BeforeTest
  fun beforeThenable(): Thenable? {
    return null
  }

  @BeforeTest
  fun beforeFuture(): ListenableFuture<Void>? {
    return null
  }

  @AfterTest
  fun afterThenable(): Thenable? {
    return null
  }

  @AfterTest
  fun afterFuture(): ListenableFuture<Void>? {
    return null
  }

  @Test
  fun thenableDoesNotHaveTimeout(): Thenable? {
    return null
  }

  @Test
  fun futureDoesNotHaveTimeout(): ListenableFuture<Void>? {
    return null
  }
}
