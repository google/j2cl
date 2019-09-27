/*
 * Copyright 2017 Google Inc.
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

package com.google.j2cl.junit.integration.stacktrace.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests stacktraces for throwing class initializer */
@RunWith(JUnit4.class)
public class ThrowsInClassInitializer extends StacktraceTestBase {

  public static class Inner {
    private static final Inner throwsInConstructor = Inner.willThrow();

    private static Inner willThrow() {
      throw new RuntimeException("__the_message__!");
    }
  }

  @Test
  public void test() {
    Object o = Inner.throwsInConstructor;
  }
}
