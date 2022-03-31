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
package nativejstypeobjectmethods;

import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithToString {
    @Override
    @JsNonNull
    public native String toString();
  }

  @JsType(isNative = true, namespace = "test.foo")
  public static class NativeJsTypeWithoutToString {}

  public static void test() {
    NativeJsTypeWithToString n1 = new NativeJsTypeWithToString();
    n1.toString();
    Object n2 = new NativeJsTypeWithToString();
    n2.toString();

    NativeJsTypeWithoutToString n3 = new NativeJsTypeWithoutToString();
    n3.toString();
    Object n4 = new NativeJsTypeWithoutToString();
    n4.toString();
  }
}
