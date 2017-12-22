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

public class Main {
  public static void main(String[] args) {
    new Main().run();
  }

  public void run() {
    // Tests from GWT
    testBaseIntersectionCast();
    testIntersectionCastWithLambdaExpr();
    testIntersectionCastPolymorphism();
    // Custom tests
    testLambdaTypeInference();
  }

  private static class EmptyA {}

  private interface EmptyI {}

  private interface EmptyJ {}

  private static class EmptyB extends EmptyA implements EmptyI {}

  private static class EmptyC extends EmptyA implements EmptyI, EmptyJ {}

  @SuppressWarnings("unused")
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
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.intersectiontype.Main$EmptyB cannot be cast "
                  + "to com.google.j2cl.transpiler.integration.intersectiontype.Main$EmptyC");
    }
    try {
      EmptyB c2BI = (EmptyB & EmptyI) localC;
      assert false : "Should have thrown a ClassCastException";
    } catch (ClassCastException e) {
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.intersectiontype.Main$EmptyC cannot be cast "
                  + "to com.google.j2cl.transpiler.integration.intersectiontype.Main$EmptyB");
    }
    try {
      EmptyJ jj = (EmptyB & EmptyJ) localB;
      assert false : "Should have thrown a ClassCastException";
    } catch (ClassCastException e) {
      assert e.getMessage()
          .equals(
              "com.google.j2cl.transpiler.integration.intersectiontype.Main$EmptyB cannot be cast "
                  + "to com.google.j2cl.transpiler.integration.intersectiontype.Main$EmptyJ");
    }
  }

  private interface SimpleI {
    int fun();
  }

  private interface SimpleJ {
    int foo();

    int bar();
  }

  private interface SimpleK {}

  public void testIntersectionCastWithLambdaExpr() {
    SimpleI simpleI1 = (SimpleI & EmptyI) () -> 11;
    assert 11 == simpleI1.fun();
    SimpleI simpleI2 = (EmptyI & SimpleI) () -> 22;
    assert 22 == simpleI2.fun();
    EmptyI emptyI = (EmptyI & SimpleI) () -> 33;
    assert 55 == ((SimpleI & SimpleK) () -> 55).fun();
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

  private interface Serial {}

  private interface Cmp {
    int cmp();
  }

  private interface Cmp2 {
    int cmp(int a);
  }

  public static Cmp method() {
    return (Cmp & Serial) () -> 1;
  }

  // This Lambda is type correctly
  public static Cmp2 method2() {
    return (Cmp2 & Serial) (a) -> a / 2;
  }

  public static void testLambdaTypeInference() {
    // The first one fails because the lambda is not properly typed.
    assert method().cmp() == 1;
    assert method2().cmp(4) == 2;
  }
}
