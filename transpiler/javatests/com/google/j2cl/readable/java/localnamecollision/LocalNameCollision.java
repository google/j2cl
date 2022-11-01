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
package localnamecollision;

public class LocalNameCollision {
  @SuppressWarnings("unused")
  public void testClassLocalVarCollision() {
    boolean LocalNameCollision = false;
    Object RuntimeException = null;
    int Asserts = 1;
    int $Asserts = 1;
    int l_Asserts = 1;
    int com_google_j2cl_readable_localnamecollision_Class = 1;
    int com_google_j2cl_readable_localnamecollision_package1_A = 1;
    int com_google_j2cl_readable_localnamecollision_package2_A = 1;
    int A = 1;
    LocalNameCollision =
        RuntimeException instanceof LocalNameCollision
            || RuntimeException instanceof RuntimeException
            || RuntimeException instanceof localnamecollision.package1.A
            || RuntimeException instanceof localnamecollision.package2.A
            || RuntimeException instanceof Class;
    assert new Asserts().n() == 5;
  }

  public boolean testClassParameterCollision(
      boolean LocalNameCollision, Object Asserts, Object $Asserts, int l_Asserts, int A) {
    return LocalNameCollision
        && Asserts instanceof LocalNameCollision
        && $Asserts instanceof Asserts
        && (l_Asserts == A);
  }

  @SuppressWarnings("unused")
  // test class and parameters collision in constructor.
  public LocalNameCollision(
      boolean LocalNameCollision, Object Asserts, Object $Asserts, int l_Asserts, int A) {
    boolean result =
        LocalNameCollision
            && Asserts instanceof LocalNameCollision
            && $Asserts instanceof Asserts
            && (l_Asserts == A);
  }
}

class A {
  static A A;
  static B B;

  static class B {}

  static void test() {
    A = A;
    B = B;

    localnamecollision.A.A = localnamecollision.A.A;
    localnamecollision.A.B = localnamecollision.A.B;
  }
}
