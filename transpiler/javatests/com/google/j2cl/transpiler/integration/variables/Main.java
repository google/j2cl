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
package com.google.j2cl.transpiler.integration.variables;

/** A class that has the same simple name of the runtime class Asserts */
class Asserts {}

public class Main {

  public static int testJSKeywords(int in) {
    int let = 100;
    return let + in;
  }

  private static void testClassLocalVarCollision() {
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

  private static void testClassParameterCollision(
      boolean Main, Object Asserts, Object $Asserts, int l_Asserts) {
    assert Main;
    assert Asserts instanceof Asserts;
    assert $Asserts instanceof CollisionInConstructor;
    assert l_Asserts == 1;
  }

  private static class CollisionInConstructor {
    CollisionInConstructor() {}

    CollisionInConstructor(
        boolean CollisionInConstructor, Object Asserts, Object $Asserts, int l_Asserts) {
      assert CollisionInConstructor;
      assert Asserts instanceof Asserts;
      assert $Asserts instanceof CollisionInConstructor;
      assert l_Asserts == 1;
    }
  }

  private static void testClassParameterCollisionInConstructor() {
    new CollisionInConstructor(true, new Asserts(), new CollisionInConstructor(), 1);
  }

  private static void testVariableInference() {
    var str = "String";
    assert str instanceof String;

    // The type of anonymous is not Runnable but the actual anonymous class which is non-denotable.
    var anonymous = new Runnable() {
      @Override
      public void run() {}

      public String exposedMethod() {
        return "Hello";
      }
    };

    assert "Hello".equals(anonymous.exposedMethod());
  }

  public static void main(String... args) {
    assert testJSKeywords(10) == 110;
    testClassLocalVarCollision();
    testClassParameterCollision(true, new Asserts(), new CollisionInConstructor(), 1);
    testClassParameterCollisionInConstructor();
    testVariableInference();
  }
}
