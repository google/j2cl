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

/**
 * Class that has a constructor with var arg parameters.
 */
class Parent {
  public int value;

  public Parent(int... args) {
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        value += args[i];
      }
    }
  }

  public Parent(String f) {
    // call the vararg-constructor by this() constructor call.
    this(1);
  }

  public int sum(int... args) {
    int sum = 0;
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        sum += args[i];
      }
    }
    return sum;
  }

  public static <T> T generics(T... elements) {
    return elements[0];
  }
}

class Child extends Parent {
  public Child() {
    // call the vararg-constructor by super() constructor call.
    super(2);
  }

  public int sum(int a, int b, int c, int d) {
    // call the vararg-constructor by super() method call.
    return super.sum(a, b, c, d);
  }
}

public class Main {
  public int field;

  public Main(int f) {
    field = f;
  }

  public int bar(Main a, Main... args) {
    int result = a.field;
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        result += args[i].field;
      }
    }
    return result;
  }

  public static void main(String... args) {
    Main m = new Main(0);
    Main m1 = new Main(1);
    Main m2 = new Main(2);
    Main m3 = new Main(3);
    Main m4 = new Main(4);
    int a = m.bar(m1, m2, m3, m4); // varargs with mulitple arguments.
    assert (a == 10);

    int b = m.bar(m1); // no argument for varargs.
    assert (b == 1);

    int c = m.bar(m1, m2); // varargs with one element.
    assert (c == 3);

    int d = m.bar(m1, new Main[] {m2, m3, m4}); // array argument for the varargs.
    assert (d == 10);

    int e = m.bar(m1, new Main[] {}); // empty array for the varargs.
    assert (e == 1);

    int f = m.bar(m1, null); // null for the varargs.
    assert (f == 1);

    Parent p = new Parent(1, 2, 3); // constructor call with varargs.
    assert (p.value == 6);

    Parent pp = new Parent(""); // constructor call with varargs is invoked by this() call.
    assert (pp.value == 1);

    Child cc = new Child(); // constructor call with varargs is invoked by super() constructor call.
    assert (cc.value == 2);

    // method call with varargs is invoked by super() method call.
    assert (new Child().sum(1, 2, 3, 4) == new Parent().sum(1, 2, 3, 4));
    
    assert (Parent.generics(1, 2) == 1);
    assert (Parent.generics("abc", "def").equals("abc"));
  }
}
