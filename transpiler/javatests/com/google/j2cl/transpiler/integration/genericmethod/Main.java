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
    assert m.foo(new Object()) instanceof Object;
    assert m.foo(new Error()) instanceof Error;
    assert m.foo(new Exception()) instanceof Exception;
    assert m.foo(new String[] {"asdf"}) instanceof String[];
  }
}
