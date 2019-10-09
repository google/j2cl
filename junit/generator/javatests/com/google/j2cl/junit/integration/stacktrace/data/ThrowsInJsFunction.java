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

import jsinterop.annotations.JsFunction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration test for throwing in a JsFunction */
@RunWith(JUnit4.class)
public class ThrowsInJsFunction extends StacktraceTestBase {
  @JsFunction
  public interface MyFunction {
    public void run();
  }

  @Test
  public void test() {
    executesFunction(this::methodRefJsFunction);
  }

  private void methodRefJsFunction() {
    executesFunction(
        // Lambda JsFunction
        () -> {
          executesFunction(
              // Anonymous JsFunction
              new MyFunction() {
                @Override
                public void run() {
                  // Concrete JsFunction
                  executesFunction(new MyFunctionImpl());
                }
              });
        });
  }

  private static final class MyFunctionImpl implements MyFunction {
    @Override
    public void run() {
      throw new RuntimeException("__the_message__!");
    }
  }

  void executesFunction(MyFunction myFunction) {
    myFunction.run();
  }
}
