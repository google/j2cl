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
import com.google.j2cl.junit.integration.testing.async.Thenable;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test processing for test with expected exception */
@RunWith(AsyncTestRunner.class)
public class TestWithExpectedException {

  @SuppressWarnings("TestExceptionChecker")
  @Test(timeout = 200, expected = Exception.class)
  public Thenable test() {
    return null;
  }
}
