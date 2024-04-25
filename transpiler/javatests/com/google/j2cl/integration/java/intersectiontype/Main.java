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
package intersectiontype;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

public class Main {
  public static void main(String... args) {
    testBaseIntersectionCast();
    testIntersectionCastWithLambdaExpr();
    testIntersectionCastPolymorphism();
    testLambdaTypeInference();
    testErasureCast();
  }

  private static class EmptyA {}

  private interface EmptyI {}

  private interface EmptyJ {}

  private static class EmptyB extends EmptyA implements EmptyI {}

  private static class EmptyC extends EmptyA implements EmptyI, EmptyJ {}

  @SuppressWarnings("unused")
  private static void testBaseIntersectionCast() {
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

    assertThrowsClassCastException(
        () -> {
          EmptyC b2CIJ = (EmptyC & EmptyI & EmptyJ) localB;
        },
        // J2CL performs casts checks from intersection casts in an order different from Java/JVM.
        isJvm() ? EmptyJ.class : EmptyC.class);
    assertThrowsClassCastException(
        () -> {
          EmptyB c2BI = (EmptyB & EmptyI) localC;
        },
        EmptyB.class);
    assertThrowsClassCastException(
        () -> {
          EmptyJ jj = (EmptyB & EmptyJ) localB;
        },
        EmptyJ.class);
  }

  private interface SimpleI {
    int fun();
  }

  private interface SimpleJ {
    int foo();

    int bar();
  }

  private interface SimpleK {}

  private static void testIntersectionCastWithLambdaExpr() {
    SimpleI simpleI1 = (SimpleI & EmptyI) () -> 11;
    assertTrue(11 == simpleI1.fun());
    SimpleI simpleI2 = (EmptyI & SimpleI) () -> 22;
    assertTrue(22 == simpleI2.fun());
    EmptyI emptyI = (EmptyI & SimpleI) () -> 33;
    assertTrue(55 == ((SimpleI & SimpleK) () -> 55).fun());
  }

  private static class SimpleA {
    public int bar() {
      return 11;
    }
  }

  private static class SimpleB extends SimpleA implements SimpleI {
    @Override
    public int fun() {
      return 22;
    }
  }

  private static class SimpleC extends SimpleA implements SimpleI {
    @Override
    public int fun() {
      return 33;
    }

    @Override
    public int bar() {
      return 44;
    }
  }

  private static class SimpleD implements SimpleI, SimpleJ {

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

  private static void testIntersectionCastPolymorphism() {
    SimpleA bb = new SimpleB();
    assertTrue(22 == ((SimpleB & SimpleI) bb).fun());
    assertTrue(11 == ((SimpleB & SimpleI) bb).bar());
    SimpleA cc = new SimpleC();
    assertTrue(33 == ((SimpleC & SimpleI) cc).fun());
    assertTrue(44 == ((SimpleC & SimpleI) cc).bar());
    assertTrue(33 == ((SimpleA & SimpleI) cc).fun());
    SimpleI ii = (SimpleC & SimpleI) cc;

    Object d = new SimpleD();
    assertTrue(4 == ((SimpleI & SimpleJ) d).foo());
    assertTrue(5 == ((SimpleI & SimpleJ) d).bar());
    assertTrue(6 == ((SimpleI & SimpleJ) d).fun());
    assertTrue(33 == ii.fun());
  }

  private interface Serial {}

  private interface Cmp {
    int cmp();
  }

  private interface Cmp2 {
    int cmp(int a);
  }

  private static Cmp method() {
    return (Cmp & Serial) () -> 1;
  }

  // This Lambda is type correctly
  private static Cmp2 method2() {
    return (Cmp2 & Serial) (a) -> a / 2;
  }

  private static void testLambdaTypeInference() {
    // The first one fails because the lambda is not properly typed.
    assertTrue(1 == method().cmp());
    assertTrue(2 == method2().cmp(4));
  }

  private static void testErasureCast() {
    assertThrowsClassCastException(
        () -> {
          new ParameterizedByIntersection().m(new EmptyA());
        },
        SimpleI.class);
  }

  private static class ParameterizedByIntersection<T extends EmptyA & SimpleI> {
    void m(T t) {
      // fun is a method on SimpleI.
      t.fun();
    }
  }
}
