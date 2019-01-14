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

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.A;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.B;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.C;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.D;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.E;
import com.google.j2cl.transpiler.integration.jsconstructor.JsConstructorClass.F;

public class Main {
  public static void main(String... args) {
    A a1 = new A();
    assertTrue((a1.fA == 1));
    A a2 = new A(10);
    assertTrue((a2.fA == 10));
    B b1 = new B(5);
    assertTrue((b1.fA == 6));
    assertTrue((b1.fB == 5));
    B b2 = new B();
    assertTrue((b2.fA == 11));
    assertTrue((b2.fB == 9));
    B b3 = new B(5, 6);
    assertTrue((b3.fA == 12));
    assertTrue((b3.fB == 35));
    C c1 = new C(10);
    assertTrue((c1.fA == 21));
    assertTrue((c1.fB == 5));
    assertTrue((c1.fC == 7));
    C c2 = new C(10, 20);
    assertTrue((c2.fA == 61));
    assertTrue((c2.fB == 5));
    assertTrue((c2.fC == 14));
    D d1 = new D();
    assertTrue((d1.fA == 10));
    assertTrue((d1.fB == 5));
    assertTrue((d1.fD == 18));
    D d2 = new D(11);
    assertTrue((d2.fA == 10));
    assertTrue((d2.fB == 5));
    assertTrue((d2.fD == 29));
    E e = new E();
    assertTrue((e.fA == 21));
    assertTrue((e.fB == 5));
    assertTrue((e.fC == 7));
    assertTrue((e.fE == 23));
    F f = new F(12);
    assertTrue((f.fA == 29));
    assertTrue((f.fB == 5));
    assertTrue((f.fC == 7));
    assertTrue((f.fF == 28));

    InstanceInitOrder.test();
    StaticInitOrder.test();
  }
}
