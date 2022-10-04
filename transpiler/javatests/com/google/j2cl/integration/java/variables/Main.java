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
package variables;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

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
    assertTrue(Main && !(new Object() instanceof Main));
    assertTrue(a instanceof Asserts);
    assertTrue(Asserts == 1);
    assertTrue($Asserts == 2);
    assertTrue(l_Asserts == 3);
  }

  private static void testClassParameterCollision(
      boolean Main, Object Asserts, Object $Asserts, int l_Asserts) {
    assertTrue(Main);
    assertTrue(Asserts instanceof Asserts);
    assertTrue($Asserts instanceof CollisionInConstructor);
    assertTrue(l_Asserts == 1);
  }

  private static class CollisionInConstructor {
    CollisionInConstructor() {}

    CollisionInConstructor(
        boolean CollisionInConstructor, Object Asserts, Object $Asserts, int l_Asserts) {
      assertTrue(CollisionInConstructor);
      assertTrue(Asserts instanceof Asserts);
      assertTrue($Asserts instanceof CollisionInConstructor);
      assertTrue(l_Asserts == 1);
    }
  }

  private static void testClassParameterCollisionInConstructor() {
    Object unused =
        new CollisionInConstructor(true, new Asserts(), new CollisionInConstructor(), 1);
  }

  private static void testVariableInference() {
    var str = "String";
    assertTrue(str instanceof String);

    // The type of anonymous is not Runnable but the actual anonymous class which is non-denotable.
    var anonymous = new Runnable() {
      @Override
      public void run() {}

      public String exposedMethod() {
        return "Hello";
      }
    };

    assertTrue("Hello".equals(anonymous.exposedMethod()));
  }

  public static void main(String... args) {
    assertTrue(testJSKeywords(10) == 110);
    testClassLocalVarCollision();
    testClassParameterCollision(true, new Asserts(), new CollisionInConstructor(), 1);
    testClassParameterCollisionInConstructor();
    testVariableInference();
  }
}
