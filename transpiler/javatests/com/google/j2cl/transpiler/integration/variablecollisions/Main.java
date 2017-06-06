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
package com.google.j2cl.transpiler.integration.variablecollisions;

class Asserts {}

public class Main {
  public Main() {}

  public Main(boolean Main, Object Asserts, Object $Asserts, int l_Asserts) {
    assert Main;
    assert Asserts instanceof Asserts;
    assert $Asserts instanceof Main;
    assert l_Asserts == 1;
  }

  public int testJSKeywords(int in) {
    int let = 100;
    return let + in;
  }

  public void testClassLocalVarCollision() {
    boolean Main = true;
    Asserts a = new Asserts();
    int Asserts = 1;
    int $Asserts = 2;
    int l_Asserts = 3;
    assert Main && !(new Object() instanceof Main);
    assert a instanceof Asserts;
    assert Asserts == 1;
    assert $Asserts == 2;
    assert l_Asserts == 3;
  }

  public void testClassParameterCollision(
      boolean Main, Object Asserts, Object $Asserts, int l_Asserts) {
    assert Main;
    assert Asserts instanceof Asserts;
    assert $Asserts instanceof Main;
    assert l_Asserts == 1;
  }

  public void testClassParameterCollisionInConstructor() {
    Main m = new Main(true, new Asserts(), new Main(), 1);
  }

  public static void main(String... args) {
    Main m = new Main();
    assert m.testJSKeywords(10) == 110;
    m.testClassLocalVarCollision();
    m.testClassParameterCollision(true, new Asserts(), m, 1);
    m.testClassParameterCollisionInConstructor();
  }
}
