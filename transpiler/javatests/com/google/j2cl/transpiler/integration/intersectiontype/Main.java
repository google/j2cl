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
package com.google.j2cl.transpiler.integration.intersectiontype;

@SuppressWarnings("unused")
public class Main {
  public static void main(String[] args) {
    new Main().run();
  }

  public void run() {
    // Tests from GWT
    testBaseIntersectionCast();
    //    testIntersectionCastWithLambdaExpr();
    testIntersectionCastPolymorphism();
    // Custom tests
    testLambdaTypeInference();
  }

  class EmptyA {}

  interface EmptyI {}

  interface EmptyJ {}

  class EmptyB extends EmptyA implements EmptyI {}

  class EmptyC extends EmptyA implements EmptyI, EmptyJ {}

  public void testBaseIntersectionCast() {
    EmptyA localB = new EmptyB();
    EmptyA localC = new EmptyC();
    EmptyB b2BI = (EmptyB & EmptyI) localB;
    EmptyC c2CIJ = (EmptyC & EmptyI & EmptyJ) localC;
    EmptyI ii1 = (EmptyB & EmptyI) localB;
    EmptyI ii2 = (EmptyC & EmptyI) localC;
    EmptyI ii3 = (EmptyC & EmptyJ) localC;
    EmptyI ii4 = (EmptyC & EmptyI & EmptyJ) localC;
    EmptyJ jj1 = (EmptyC & EmptyI & EmptyJ) localC;
    EmptyJ jj2 = (EmptyC & EmptyI) localC;
    EmptyJ jj3 = (EmptyC & EmptyJ) localC;
    EmptyJ jj4 = (EmptyI & EmptyJ) localC;

    try {
      EmptyC b2CIJ = (EmptyC & EmptyI & EmptyJ) localB;
      assert false : "Should have thrown a ClassCastException";
    } catch (ClassCastException e) {
      // Expected.
    }
    try {
      EmptyB c2BI = (EmptyB & EmptyI) localC;
      assert false : "Should have thrown a ClassCastException";
    } catch (ClassCastException e) {
      // Expected.
    }
    try {
      EmptyJ jj = (EmptyB & EmptyJ) localB;
      assert false : "Should have thrown a ClassCastException";
    } catch (ClassCastException e) {
      // Expected.
    }
  }

  interface SimpleI {
    int fun();
  }

  interface SimpleJ {
    int foo();

    int bar();
  }

  interface SimpleK {}

  // TODO(b/36781939): Fix Lambda type inference.
  //  public void testIntersectionCastWithLambdaExpr() {
  //    SimpleI simpleI1 =
  //        (SimpleI & EmptyI)
  //            () -> {
  //              return 11;
  //            };
  //    assert 11 == simpleI1.fun();
  //    SimpleI simpleI2 =
  //        (EmptyI & SimpleI)
  //            () -> {
  //              return 22;
  //            };
  //    assert 22 == simpleI2.fun();
  //    EmptyI emptyI =
  //        (EmptyI & SimpleI)
  //            () -> {
  //              return 33;
  //            };
  //    try {
  //      ((EmptyA & SimpleI)
  //              () -> {
  //                return 33;
  //              })
  //          .fun();
  //      assert false : "Should have thrown a ClassCastException";
  //    } catch (ClassCastException e) {
  //      // expected.
  //    }
  //    try {
  //      ((SimpleI & SimpleJ)
  //              () -> {
  //                return 44;
  //              })
  //          .fun();
  //      assert false : "Should have thrown a ClassCastException";
  //    } catch (ClassCastException e) {
  //      // expected.
  //    }
  //    try {
  //      ((SimpleI & SimpleJ)
  //              () -> {
  //                return 44;
  //              })
  //          .foo();
  //      assert false : "Should have thrown a ClassCastException";
  //    } catch (ClassCastException e) {
  //      // expected.
  //    }
  //    try {
  //      ((SimpleI & SimpleJ)
  //              () -> {
  //                return 44;
  //              })
  //          .bar();
  //      assert false : "Should have thrown a ClassCastException";
  //    } catch (ClassCastException e) {
  //      // expected.
  //    }
  //    assert 55
  //        == ((SimpleI & SimpleK)
  //                () -> {
  //                  return 55;
  //                })
  //            .fun();
  //  }

  class SimpleA {
    public int bar() {
      return 11;
    }
  }

  class SimpleB extends SimpleA implements SimpleI {
    @Override
    public int fun() {
      return 22;
    }
  }

  class SimpleC extends SimpleA implements SimpleI {
    @Override
    public int fun() {
      return 33;
    }

    @Override
    public int bar() {
      return 44;
    }
  }

  class SimpleD implements SimpleI, SimpleJ {

    @Override
    public int foo() {
      return 4;
    }

    @Override
    public int bar() {
      return 5;
    }

    @Override
    public int fun() {
      return 6;
    }
  }

  public void testIntersectionCastPolymorphism() {
    SimpleA bb = new SimpleB();
    assert 22 == ((SimpleB & SimpleI) bb).fun();
    assert 11 == ((SimpleB & SimpleI) bb).bar();
    SimpleA cc = new SimpleC();
    assert 33 == ((SimpleC & SimpleI) cc).fun();
    assert 44 == ((SimpleC & SimpleI) cc).bar();
    assert 33 == ((SimpleA & SimpleI) cc).fun();
    SimpleI ii = (SimpleC & SimpleI) cc;

    Object d = new SimpleD();
    assert 4 == ((SimpleI & SimpleJ) d).foo();
    assert 5 == ((SimpleI & SimpleJ) d).bar();
    assert 6 == ((SimpleI & SimpleJ) d).fun();
    assert 33 == ii.fun();
  }

  interface Serial {}

  interface Cmp {
    int cmp();
  }

  interface Cmp2 {
    int cmp(int a);
  }

  // TODO(b/36781939): Lambdas do not have the correct types applied yet.  Jdt only recognizes this
  // Lambda as Cmp whereas it should recognize Cmp and Serial.
  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=496596
  public static Cmp method() {
    return (Cmp & Serial) () -> 1;
  }

  // This Lambda is type correctly
  public static Cmp2 method2() {
    return (Cmp2 & Serial) (a) -> a / 2;
  }

  public static void testLambdaTypeInference() {
    // The first one fails because the lambda is not properly typed.
    // assert method().cmp() == 1;
    assert method2().cmp(4) == 2;
  }
}
