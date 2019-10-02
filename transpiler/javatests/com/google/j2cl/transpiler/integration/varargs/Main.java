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
package com.google.j2cl.transpiler.integration.varargs;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/** Tests varargs. */
public class Main {
  public static void main(String... args) {
    testVarargs_method();
    testVarargs_constructor();
    testVarargs_superMethodCall();
    // TODO(b/141990540): uncomment the test call once the bug if fixed.
    // testVarargs_implicitSuperConstructorCall();
    testVarargs_genericVarargsParameter();
  }

  /** Class that has a constructor with var arg parameters. */
  private static class Parent {
    public int value;

    public Parent(int... args) {
      for (int i = 0; i < args.length; i++) {
        value += args[i];
      }
    }

    public Parent(String f) {
      // call the vararg-constructor by this() constructor call.
      this(1);
    }

    public int sum(int... args) {
      int sum = 0;
      for (int i = 0; i < args.length; i++) {
        sum += args[i];
      }
      return sum;
    }
  }

  private static class Child extends Parent {
    public Child() {
      // call the vararg-constructor by super() constructor call.
      super(2);
    }

    public int sum(int a, int b, int c, int d) {
      // call the vararg-constructor by super() method call.
      return super.sum(a, b, c, d);
    }
  }

  private static void testVarargs_constructor() {
    Parent p = new Parent(1, 2, 3); // constructor call with varargs.
    assertTrue((p.value == 6));

    Parent pp = new Parent(""); // constructor call with varargs is invoked by this() call.
    assertTrue((pp.value == 1));

    Child cc = new Child(); // constructor call with varargs is invoked by super() constructor call.
    assertTrue((cc.value == 2));
  }

  private static void testVarargs_genericVarargsParameter() {
    assertTrue((generics(1, 2) == 1));
    assertTrue((generics("abc", "def").equals("abc")));
  }

  private static <T> T generics(T... elements) {
    return elements[0];
  }

  private static void testVarargs_superMethodCall() {
    // method call with varargs is invoked by super() method call.
    assertTrue((new Child().sum(1, 2, 3, 4) == new Parent().sum(1, 2, 3, 4)));
  }

  private static class ChildWithImplicitSuperCall extends Parent {
    public ChildWithImplicitSuperCall() {
      // Implicit super call should call the vararg-constructor.
    }
  }

  private static void testVarargs_implicitSuperConstructorCall() {
    // method call with varargs is invoked by super() method call.
    assertTrue(new ChildWithImplicitSuperCall().value == 0);
  }

  private static void testVarargs_method() {
    Integer i1 = new Integer(1);
    Integer i2 = new Integer(2);
    Integer i3 = new Integer(3);
    Integer i = new Integer(4);
    int a = bar(i1, i2, i3, i); // varargs with mulitple arguments.
    assertTrue((a == 10));

    int b = bar(i1); // no argument for varargs.
    assertTrue((b == 1));

    int c = bar(i1, i2); // varargs with one element.
    assertTrue((c == 3));

    int d = bar(i1, new Integer[] {i2, i3, i}); // array argument for the varargs.
    assertTrue((d == 10));

    int e = bar(i1, new Integer[] {}); // empty array for the varargs.
    assertTrue((e == 1));

    int f = bar(i1, null); // null for the varargs.
    assertTrue((f == 1));
  }

  public static int bar(Integer a, Integer... args) {
    int result = a.intValue();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        result += args[i].intValue();
      }
    }
    return result;
  }
}
