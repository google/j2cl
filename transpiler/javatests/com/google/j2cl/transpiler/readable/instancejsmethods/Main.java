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

public class Main {
  public void testCallBySuperParent() {
    SuperParent sp = new SuperParent();
    SuperParent p = new Parent();
    SuperParent c = new Child();
    Parent pp = new Parent();
    Parent cc = new Child();
    MyInterface intf = new Child();

    sp.fun(12, 35);
    sp.bar(6, 7);
    p.fun(12, 35);
    p.bar(6, 7);
    c.fun(12, 35);
    c.bar(6, 7);
    pp.foo(1);
    cc.foo(1);
    intf.intfFoo();
  }

  public static void testCallByConcreteType() {
    SuperParent sp = new SuperParent();
    Parent p = new Parent();
    Child c = new Child();

    sp.fun(12, 35);
    sp.bar(6, 7);
    p.fun(12, 35);
    p.bar(6, 7);
    p.foo(1);
    c.fun(12, 35);
    c.bar(6, 7);
    c.foo(1);
    c.intfFoo();
  }
}
