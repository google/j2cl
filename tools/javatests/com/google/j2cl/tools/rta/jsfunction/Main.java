/*
 * Copyright 2018 Google Inc.
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
package com.google.j2cl.tools.rta.jsfunction;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;

/**
 * J2CL doesn't create javascript type for JsFunctions. This test ensure that different way of
 * implementing a JsFunction is understood and correctly processed by RTA.
 */
public class Main {
  @JsFunction
  interface Foo {
    void accept(String fo);
  }

  @JsFunction
  interface NotUsed {
    void accept(String fo);
  }

  static final class FooImpl implements Foo {
    public void accept(String f) {}
  }

  @JsMethod
  public static void entryPoint() {
    new Main().execute(new FooImpl());
    new Main().execute(Main::log);
    new Main()
        .execute(
            f -> {
              log2(f);
            });

    // JsFunction is handled conservatively and log3 is kept even if it is not used.
    NotUsed notUsed = f -> log3(f);
  }

  private void execute(Foo foo) {
    foo.accept("foo");
  }

  private static void log(Object o) {}

  private static void log2(Object o) {}

  private static void log3(Object o) {}
}
