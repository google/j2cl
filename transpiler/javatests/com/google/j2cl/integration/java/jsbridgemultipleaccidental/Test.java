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
package jsbridgemultipleaccidental;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsType;

interface InterfaceOne {
  int fun(int i);
}

interface InterfaceTwo extends InterfaceOne {
  @Override
  int fun(int i);
}

interface InterfaceThree {
  int fun(int i);
}

@JsType
class C {
  public int fun(int i) {
    return i;
  }
}

public class Test extends C implements InterfaceTwo, InterfaceThree {
  @JsConstructor
  public Test() {}
  // there should be only one bridge method created for List.fun() and Collection.fun().
}
