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
package com.google.j2cl.transpiler.integration.jsbridgemethodaccidentaloverride;

public class Main {
  public static void main(String... args) {
    MyJsType a = new MyJsType();
    SubJsType b = new SubJsType();
    MyInterface c = new SubJsType();
    OtherInterface d = new SubJsType();
    assert a.foo(1) == 1;
    assert b.foo(1) == 1;
    assert c.foo(1) == 1;
    assert a.bar(1) == 2;
    assert b.bar(1) == 3;
    assert c.bar(1) == 3;
    assert a.fun(1) == 0;
    assert b.fun(1) == 0;
    assert d.fun(1) == 0;
    assert a.toString().startsWith(a.getClass().getName());
  }
}
