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
package jsbridgemultipleexposing;

import jsinterop.annotations.JsMethod;

class A {
  public int m() {
    return 1;
  }
}

class B extends A {
  @Override
  public int m() {
    return 5;
  }
}

interface I {
  int m();
}

public class Foo extends B implements I {
  @Override
  @JsMethod
  public int m() {
    return 10;
  }
}
