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
package com.google.j2cl.transpiler.integration.bridgemethods;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;
import static com.google.j2cl.transpiler.utils.Asserts.fail;

/**
 * Test for complicated cases for bridge methods, which includes bounded type varaibles, multiple
 * inheritance and accidental overrides.
 */
class Parent<T extends Error> {
  @SuppressWarnings("unused")
  public int foo(T t) {
    return 1;
  }
}

interface SomeInterface<T> {
  int foo(T t);
}

class Child extends Parent<AssertionError> implements SomeInterface<AssertionError> {
  // accidental overrides.
}

class AnotherChild extends Parent<Error> implements SomeInterface<AssertionError> {
  @Override
  public int foo(AssertionError t) {
    return 2;
  }
}

public class Main {
  public int callByInterface(SomeInterface<AssertionError> intf, AssertionError ae) {
    return intf.foo(ae);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public int callByInterface(SomeInterface intf, Error e) {
    return intf.foo(e);
  }

  public int callByParent(Parent<AssertionError> p, AssertionError ae) {
    return p.foo(ae);
  }

  public int callByParent(Parent<Error> p, Error e) {
    return p.foo(e);
  }

  public static void main(String[] args) {
    Main m = new Main();
    Child c = new Child();
    AnotherChild ac = new AnotherChild();

    assertTrue((m.callByInterface(c, new AssertionError()) == 1));
    try {
      // javac throws a ClassCastException, but eclipse does not.
      assertTrue((m.callByInterface(c, new Error()) == 1));
      fail("ClassCastException should be thrown.");
    } catch (ClassCastException e) {
      // expected.
    }
    assertTrue((m.callByParent(c, new AssertionError()) == 1));
    assertTrue((c.foo(new AssertionError()) == 1));

    assertTrue((m.callByParent(ac, new Error()) == 1));
    assertTrue((m.callByParent(ac, new AssertionError()) == 1));
    assertTrue((m.callByInterface(ac, new AssertionError()) == 2));
    assertTrue((ac.foo(new AssertionError()) == 2));
    assertTrue((ac.foo(new Error()) == 1));
  }
}
