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
package com.google.j2cl.transpiler.integration.jsconstructor;

import jsinterop.annotations.JsType;

@JsType
public class StaticInitOrder {
  public static int counter = 1;

  public static void test() {
    assert StaticInitOrder.counter == 5;
    assert StaticInitOrder.field1 == 2;
    assert StaticInitOrder.field2 == 3;
  }

  public static int field1 = initializeField1();
  public static int field2 = initializeField2();

  static {
    assert counter++ == 3; // #3
  }

  static {
    assert counter++ == 4; // #4
  }

  public static int initializeField1() {
    assert counter++ == 1; // #1
    return counter;
  }

  public static int initializeField2() {
    assert counter++ == 2; // #2
    return counter;
  }
}
