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

import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.A;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.B;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.C;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.D;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.E;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.F;

public class Main {
  public static void main(String... args) {
    A a1 = new A();
    assert (a1.fA == 1);
    A a2 = new A(10);
    assert (a2.fA == 10);
    B b1 = new B(5);
    assert (b1.fA == 6);
    assert (b1.fB == 5);
    B b2 = new B();
    assert (b2.fA == 11);
    assert (b2.fB == 9);
    B b3 = new B(5, 6);
    assert (b3.fA == 12);
    assert (b3.fB == 35);
    C c1 = new C(10);
    assert (c1.fA == 21);
    assert (c1.fB == 5);
    assert (c1.fC == 7);
    C c2 = new C(10, 20);
    assert (c2.fA == 61);
    assert (c2.fB == 5);
    assert (c2.fC == 14);
    D d1 = new D();
    assert (d1.fA == 10);
    assert (d1.fB == 5);
    assert (d1.fD == 18);
    D d2 = new D(11);
    assert (d2.fA == 10);
    assert (d2.fB == 5);
    assert (d2.fD == 29);
    E e = new E();
    assert (e.fA == 21);
    assert (e.fB == 5);
    assert (e.fC == 7);
    assert (e.fE == 23);
    F f = new F(12);
    assert (f.fA == 29);
    assert (f.fB == 5);
    assert (f.fC == 7);
    assert (f.fF == 28);

    InstanceInitOrder.test();
    StaticInitOrder.test();
  }
}
