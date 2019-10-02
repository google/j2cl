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
package com.google.j2cl.junit.integration.stacktrace.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Simple stack test for user defined exception class with a custom getMessage. */
@RunWith(JUnit4.class)
public class CustomExceptionStacktraceTest extends StacktraceTestBase {
  @Test
  public void test() {
    throw new MyException();
  }

  static class MyException extends RuntimeException {
    MyException() {
      super("__the_message__!");
    }

    @Override
    public String getMessage() {
      return super.getMessage() + " and custom exception message!";
    }
  }
}
