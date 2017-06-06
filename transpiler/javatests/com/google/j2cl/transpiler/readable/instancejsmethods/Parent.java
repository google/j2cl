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
package com.google.j2cl.transpiler.readable.instancejsmethods;

import jsinterop.annotations.JsMethod;

public class Parent extends SuperParent {
  /**
   * JsMethod that exposes non-JsMethod in parent, with renaming.
   *
   * <p>A bridge method m_fun__int__int should be generated.
   */
  @Override
  @JsMethod(name = "sum")
  public int fun(int a, int b) {
    return a + b;
  }

  /**
   * JsMethod that exposes non-JsMethod in parent, without renaming.
   *
   * <p>A bridge method m_bar__int__int should be generated.
   */
  @Override
  @JsMethod
  public int bar(int a, int b) {
    return a * b;
  }

  /**
   * JsMethod that does not expose non-JsMethod in parent.
   *
   * <p>No bridge method should be generated.
   */
  @JsMethod(name = "myFoo")
  public int foo(int a) {
    return a;
  }
}
