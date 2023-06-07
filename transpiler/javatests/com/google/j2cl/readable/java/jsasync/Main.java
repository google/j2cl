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
package jsasync;

import jsinterop.annotations.JsAsync;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  private final IThenable<Integer> x = Promise.resolve(10);

  @JsAsync
  IThenable<Void> emptyAsyncMethod() {
    return null;
  }

  @JsAsync
  IThenable<Integer> asyncMethod() {
    int result = await(Promise.resolve(7));
    return parametricAsyncMethod(result);
  }

  @JsAsync
  <T> IThenable<T> parametricAsyncMethod(T value) {
    return Promise.resolve(await(Promise.resolve(value)));
  }

  @JsAsync
  static IThenable<Void> staticAsyncMethod() {
    return Promise.resolve(await(Promise.resolve((Void) null)));
  }

  @SuppressWarnings("unused")
  void testAsyncLambdas() {
    AsyncInterface i = () -> Promise.resolve(await(Promise.resolve(5)));

    BaseInterface o =
        new BaseInterface() {
          @JsAsync
          @Override
          public IThenable<Integer> asyncCall() {
            return Promise.resolve(await(x));
          }
        };

    AsyncJsFunctionInterface j = () -> Promise.resolve(await(Promise.resolve(5)));

    JsFunctionInterface optimizableJsFunction =
        new JsFunctionInterface() {
          @JsAsync
          @Override
          public IThenable<Integer> doSomething() {
            return Promise.resolve(await(x));
          }
        };

    JsFunctionInterface unoptimizableJsFunction =
        new JsFunctionInterface() {
          @JsAsync
          @Override
          public IThenable<Integer> doSomething() {
            @SuppressWarnings("unused")
            JsFunctionInterface tmp = this;
            return Promise.resolve(await(x));
          }
        };
  }

  interface BaseInterface {
    IThenable<Integer> asyncCall();
  }

  interface AsyncInterface extends BaseInterface {
    @JsAsync
    @Override
    IThenable<Integer> asyncCall();
  }

  interface InterfaceWithAsyncDefaultMethod {
    @JsAsync
    default IThenable<Integer> asyncCall() {
      return Promise.resolve(await(Promise.resolve(5)));
    }
  }

  @JsFunction
  interface JsFunctionInterface {
    IThenable<Integer> doSomething();
  }

  @JsFunction
  interface AsyncJsFunctionInterface {
    @JsAsync
    IThenable<Integer> doSomething();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public interface IThenable<T> {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  private static class Promise<T> implements IThenable<T> {
    public static native <T> Promise<T> resolve(T value);
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  private static native <T> T await(IThenable<T> thenable);
}
