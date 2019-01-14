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
package com.google.j2cl.transpiler.integration.genericmethod;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

public class Main {
  public Object foo(Object o) {
    return o;
  }

  public <T extends Error> T foo(T t) {
    return t;
  }

  public <T extends Exception> T foo(T t) {
    return t;
  }

  public <T> T[] foo(T[] array) {
    return array;
  }

  public static void main(String[] args) {
    Main m = new Main();
    assertTrue(m.foo(new Object()) instanceof Object);
    assertTrue(m.foo(new Error()) instanceof Error);
    assertTrue(m.foo(new Exception()) instanceof Exception);
    assertTrue(m.foo(new String[] {"asdf"}) instanceof String[]);
  }
}
