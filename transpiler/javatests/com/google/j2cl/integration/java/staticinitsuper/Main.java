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
package staticinitsuper;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test static initializers order via super. */
public class Main {

  private static List<String> loadOrder = new ArrayList<>();

  static Object add(String name) {
    loadOrder.add(name);
    return null;
  }

  static class A implements B, C {}

  interface B {
    static final Object rerunClinitOfA = new A();
    static final Object triggerD1 = new D1();

    default void dummyB() {}
  }

  interface C {
    static final Object triggerD2 = new D2();

    default void dummyC() {}
  }

  static class D1 {
    static {
      loadOrder.add("D1");
    }
  }

  static class D2 {
    static {
      loadOrder.add("D2");
    }
  }

  private static void testInterfaceInitialization() {
    loadOrder.clear();
    Object unused = new A(); // start clinit chain
    assertExpectedOrder("D1", "D2");
  }

  interface X1 {
    Object o = add("X1");

    default void m() {}
  }

  interface X2 extends X1 {
    Object o = add("X2");

    default void m() {}
  }

  interface X3 extends X2 {
    Object o = add("X3");
  }

  interface Y1 {
    Object o = add("Y1");

    default void n() {}
  }

  interface Y2 extends Y1 {
    Object o = add("Y2");
  }

  interface Y3 extends Y2 {
    Object o = add("Y3");

    default void n() {}
  }

  static class X implements X3, Y3 {}

  public static void testInitializationViaClass() {
    loadOrder.clear();
    Object unused = new X();
    assertExpectedOrder("X1", "X2", "Y1", "Y3");
  }

  interface Z1 {
    Object o = add("Z1");

    default void n() {}
  }

  interface Z2 extends Z1 {
    Object o = add("Z2");
  }

  interface Z3 extends Z2 {
    Object o = add("Z3");
  }

  public static void testInitializationViaInterfaceStaticFieldAccess() {
    loadOrder.clear();
    Object o = Z3.o;

    // TODO(b/254306517): Initialization of interfaces should never trigger the initialization of
    // super interfaces. Merge the test when this is fixed.
    if (isJvm()) {
      assertExpectedOrder("Z3");
    } else {
      assertExpectedOrder("Z1", "Z3");
    }

    loadOrder.clear();
    o = Z2.o;
    assertExpectedOrder("Z2");
  }

  public static void main(String... args) {
    testInitializationViaClass();
    testInterfaceInitialization();
    testInitializationViaInterfaceStaticFieldAccess();
  }

  private static void assertExpectedOrder(String... expected) {
    assertTrue(
        "Expected <" + Arrays.toString(expected) + " > but was <" + loadOrder + " >",
        Arrays.asList(expected).equals(loadOrder));
  }
}
