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
package com.google.j2cl.junit.runtime;

import jsinterop.annotations.JsAsync;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Internal async helpers intended to be called by the JsUnit adapter gencode. */
public final class JsUnitAsyncHelpers {

  /** The marker type of a JS thenable. */
  @JsType(isNative = true, name = "IThenable", namespace = JsPackage.GLOBAL)
  public interface IThenable<T> {}

  @JsMethod(namespace = JsPackage.GLOBAL)
  public static native void await(IThenable<?> thenable);

  @JsAsync
  public static IThenable<?> withTimeout(Object thenable, double timeout) {
    if (thenable == null) {
      throw new IllegalStateException("Test returned null as its promise");
    }
    if (timeout == 0) {
      // Default timeout for async tests that are allowed to not specify timeout.
      timeout = 5000;
    }
    JsUnitHelpers.GoogTestCase.getActiveTestCase().promiseTimeout = timeout;
    return ((IThenable<?>) thenable);
  }

  private JsUnitAsyncHelpers() {}
}
