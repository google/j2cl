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
package com.google.j2cl.tools.rta.pruningresults;

import jsinterop.annotations.JsMethod;

/**
 * Entry point of our program. We expect that RTA removes the default constructor, the
 * unusedMethod() and the unusedStaticField.
 */
public class Foo {
  public static String unusedStaticField;

  {
    System.err.println("This is unused");
  }

  public Foo() {
    System.err.println("This is unused");
  }

  @JsMethod
  public static void entryPoint() {
    new Bar().bar();
  }

  public void unusedMethod() {
    System.err.println("This is unused");
  }
}
