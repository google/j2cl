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
package jsconstructor;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;

import jsconstructor.JsConstructorClass.A;
import jsconstructor.JsConstructorClass.B;
import jsconstructor.JsConstructorClass.C;

public class Main {
  public static void main(String... args) {
    A a1 = new A();
    assertEquals(1, a1.fA);
    A a2 = new A(10);
    assertEquals(10, a2.fA);
    B b1 = new B(5);
    assertEquals(6, b1.fA);
    assertEquals(5, b1.fB);
    B b2 = new B();
    assertEquals(11, b2.fA);
    assertEquals(9, b2.fB);
    B b3 = new B(5, 6);
    assertEquals(12, b3.fA);
    assertEquals(35, b3.fB);
    C c1 = new C(10);
    assertEquals(21, c1.fA);
    assertEquals(5, c1.fB);
    assertEquals(7, c1.fC);
    C c2 = new C(10, 20);
    assertEquals(61, c2.fA);
    assertEquals(5, c2.fB);
    assertEquals(14, c2.fC);

    InstanceInitOrder.test();
    StaticInitOrder.test();
    InnerClassCapture.test();
  }
}
