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
package com.google.j2cl.transpiler.integration.bridgemethodmultipleoverrides;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;
import static com.google.j2cl.transpiler.utils.Asserts.fail;

/**
 * Test for class that extends/implements parameterized type that also extends/implements
 * parameterized type.
 */
interface SomeInterface<T, S> {
  int set(T t, S s);
}

class SuperParent<T, S> {
  public T t;
  public S s;

  public int set(T t, S s) {
    this.t = t;
    this.s = s;
    return 0;
  }
}

class Parent<T> extends SuperParent<T, Exception> {
  @Override
  public int set(T t, Exception s) {
    super.set(t, s);
    return 1;
  }
}

class Child extends Parent<Error> implements SomeInterface<Error, Exception> {
  @Override
  public int set(Error t, Exception s) {
    super.set(t, s);
    return 2;
  }
}

public class Main {
  @SuppressWarnings({"rawtypes", "unchecked"})
  public int callByInterface(SomeInterface intf, Object t, Object s) {
    return intf.set(t, s);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public int callBySuperParent(SuperParent sp, Object t, Object s) {
    return sp.set(t, s);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public int callByParent(Parent p, Object t, Exception s) {
    return p.set(t, s);
  }

  public static void main(String[] args) {
    Main m = new Main();
    Error err = new Error();
    Exception exc = new Exception();

    Child c = new Child();
    assertTrue(m.callByInterface(c, err, exc) == 2);
    assertTrue(c.t == err);
    assertTrue(c.s == exc);

    c = new Child();
    assertTrue(m.callBySuperParent(c, err, exc) == 2);
    assertTrue(c.t == err);
    assertTrue(c.s == exc);

    c = new Child();
    assertTrue(m.callByParent(c, err, exc) == 2);
    assertTrue(c.t == err);
    assertTrue(c.s == exc);

    try {
      m.callByInterface(c, new Object(), new Object());
      fail("ClassCastException should be thrown.");
    } catch (ClassCastException e) {
      // expected;
    }
    try {
      m.callBySuperParent(c, new Object(), new Object());
      fail("ClassCastException should be thrown.");
    } catch (ClassCastException e) {
      // expected;
    }
    try {
      m.callByParent(c, new Object(), new Exception());
      fail("ClassCastException should be thrown.");
    } catch (ClassCastException e) {
      // expected;
    }
  }
}
