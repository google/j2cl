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
package com.google.j2cl.transpiler.integration.jsasync;

import jsinterop.annotations.JsAsync;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  static Promise five() {
    return Promise.resolve(5);
  }

  static Promise ten() {
    return Promise.resolve(10);
  }

  @JsAsync
  @JsMethod
  static Promise fifteen() {
    int a = await(five());
    assert a == 5;
    JsIntFunction asyncFunction =
        () -> {
          int ten = await(ten());
          return Promise.resolve(ten);
        };
    int b = await(asyncFunction.get());
    assert b == 10;
    return Promise.resolve(a + b);
  }

  @JsAsync
  public static Promise main(String... args) {
    int result = await(fifteen());
    assert result == 15;
    result += await(InterfaceWithDefaultMethod.staticAsyncMethod());
    assert result == 20;
    result += await(new InterfaceWithDefaultMethod() {}.defaultAsyncMethod());
    assert result == 30;
    // TODO(b/69036598): uncomment when bug is fixed.
    // result += same(await(ten())); // Causes expression decomposition error in JsCompiler.
    // assert result == 40;
    return Promise.resolve(result);
  }

  private static int same(int i) {
    return i;
  }

  @JsFunction
  private interface JsIntFunction {
    @JsAsync
    Promise get();
  }

  private interface InterfaceWithDefaultMethod {
    @JsAsync
    default Promise defaultAsyncMethod() {
      int result = await(ten());
      assert result == 10;
      return Promise.resolve(result);
    }

    @JsAsync
    static Promise staticAsyncMethod() {
      int result = await(five());
      assert result == 5;
      return Promise.resolve(result);
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  private static class Promise {
    public static native Promise resolve(int value);
  }

  @JsMethod(namespace = JsPackage.GLOBAL)
  private static native int await(Promise thenable);
}
