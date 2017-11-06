/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.readable.async;

import elemental2.promise.IThenable;
import elemental2.promise.Promise;
import jsinterop.annotations.JsAsync;
import jsinterop.annotations.JsFunction;

public class Async {
  @JsAsync
  IThenable<Void> asyncMethod() {
    return Promise.resolve((Void) null);
  }

  @JsAsync
  static IThenable<Void> staticAsyncMethod() {
    return Promise.resolve((Void) null);
  }

  void testAsyncLambdas() {
    AsyncInterface i = () -> Promise.resolve(5);
    i.asyncCall();

    AsyncJsInterface j = () -> Promise.resolve(5);
    j.doSomething();
  }

  interface AsyncInterface {
    @JsAsync
    IThenable<Integer> asyncCall();
  }

  interface AsyncInterfaceWithDefaultMethod {
    @JsAsync
    default IThenable<Integer> asyncCall() {
      return Promise.resolve(5);
    }
  }

  // TODO(skill): fix @JsAsync use case for @JsProperty
  /*
  static class ClassWithProperty {
    @JsAsync
    @JsProperty
    IThenable<Integer> getX() {
      return Promise.resolve(5);
    }
  }
  */

  @JsFunction
  interface AsyncJsInterface {
    @JsAsync
    IThenable<Integer> doSomething();
  }
}
