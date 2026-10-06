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
package externs;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  public static class TopLevelExtern {
    @JsType(isNative = true)
    public static class InnerExtern {}

    @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Foo")
    public static class Inner {
      public static int staticField;

      public static native int staticMethod();

      public String foo;

      public Inner(String str) {}

      public native String instanceMethod();
    }
  }

  TopLevelExtern.InnerExtern innerExtern;
  TopLevelExtern.Inner inner;

  public static void main(String... args) {
    TopLevelExtern.Inner inner = new TopLevelExtern.Inner("Hello");
    TopLevelExtern.Inner.staticField = TopLevelExtern.Inner.staticMethod();
    inner.foo = inner.instanceMethod();
  }
}
