/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.junit.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsAsync;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Internal helpers intended to be called by the JsUnit adapter gencode. */
public final class JsUnitHelpers {

  /** The marker type of a JS thenable. */
  @JsType(isNative = true, name = "IThenable", namespace = JsPackage.GLOBAL)
  public interface IThenable<T> {}

  @JsMethod(namespace = JsPackage.GLOBAL)
  @Wasm("nop") // Cannot be used in WASM.
  public static native void await(IThenable<?> thenable);

  @JsAsync
  @Wasm("nop") // Cannot be used in WASM.
  public static IThenable<?> withTimeout(Object thenable, double timeout) {
    if (thenable == null) {
      throw new IllegalStateException("Test returned null as its promise");
    }
    GoogTestCase.getActiveTestCase().promiseTimeout = timeout;
    return ((IThenable<?>) thenable);
  }

  public static void handleAssumptionFailure(InternalAssumptionViolatedException e) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    e.printStackTrace(new PrintStream(outputStream));
    GoogTestCase.getActiveTestCase()
        .saveMessage("Skipping test case due to assumption failure: " + outputStream);
    // JSUnit doesn't have an API for skipping tests, so just rethrow the exception.
    throw new UnsupportedOperationException("Assumptions are unsupported in JSUnit", e);
  }

  @JsType(isNative = true, name = "TestCase", namespace = "goog.testing")
  private static class GoogTestCase {
    public static native GoogTestCase getActiveTestCase();

    public double promiseTimeout;

    public native void saveMessage(String message);
  }

  private JsUnitHelpers() {}
}
