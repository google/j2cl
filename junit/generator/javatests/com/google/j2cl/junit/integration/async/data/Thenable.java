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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

/** A Thenable contract for testing purposes. */
@JsType
public interface Thenable {
  // We are defining two different callback types here so that we can reproduce b/32406287
  @JsFunction
  public interface FullFilledCallback {
    void execute(Object o);
  }

  @JsFunction
  public interface RejectedCallback {
    void execute(Object o);
  }

  void then(FullFilledCallback onFulfilled, RejectedCallback onRejected);
}
