/*
 * Copyright 2026 Google Inc.
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

/** Tests stacktraces for throwing in implicit super call */
@RunWith(JUnit4.class)
public class ThrowsInImplicitSuperConstructor extends StacktraceTestBase {

  public static class Parent {
    public Parent() {
      throw new RuntimeException("__the_message__!");
    }
  }

  public static class Child extends Parent {
    public Child() {
      // Implicitly calls super() -> Parent()
    }
  }

  @Test
  public void test() {
    var _ = new Child();
  }
}
