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
package com.google.j2cl.junit.integration.testing.testlogger;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

/** Calling stub for window.console. */
@JsType(isNative = true, name = "TestCase", namespace = "goog.testing")
public class TestCaseLogger {
  private static native TestCaseLogger getActiveTestCase();

  private native void saveMessage(String message);

  @JsOverlay
  public static void log(String message) {
    // We are using the prefix here so that we can clearly identify messages coming from our tests
    // vs. messages that just happened to be in the output of a test (e.g. coming from the
    // testing infrastructure itself).
    getActiveTestCase().saveMessage("[java_message_from_test] " + message);
  }
}
