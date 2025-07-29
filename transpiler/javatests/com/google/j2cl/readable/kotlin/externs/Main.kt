/*
 * Copyright 2022 Google Inc.
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
package externs

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

class Main {
  class FooImpl {
    @JsProperty var foo: String? = null
  }

  // Is overlaying extern "Foo", not Java class FooImpl. The extern Foo is just a @typedef that
  // declares a string field named "foo".
  @JsType(isNative = true, name = "Foo", namespace = JsPackage.GLOBAL)
  interface FooOverlay {
    /** Returns the foo value. */
    @JsProperty fun getFoo(): String?
  }

  var aFoo: FooOverlay? = null

  @JsType(isNative = true, namespace = JsPackage.GLOBAL)
  class TopLevelExtern {
    @JsType(isNative = true) class InnerExtern

    @JsType(isNative = true, namespace = "externs.Main", name = "FooImpl") class Inner
  }

  var innerExtern: TopLevelExtern.InnerExtern? = null
  var inner: TopLevelExtern.Inner? = null
}

private fun testFooOverlay(fooOverlay: Main.FooOverlay): Boolean {
  return fooOverlay.getFoo() == "Hello"
}

@JsMethod private external fun useDirectlyAsFoo(fooOverlay: Any)

fun main(args: Array<String>) {
  testFooOverlay(Main.FooImpl() as Main.FooOverlay)
  useDirectlyAsFoo(Main.FooImpl())
}
