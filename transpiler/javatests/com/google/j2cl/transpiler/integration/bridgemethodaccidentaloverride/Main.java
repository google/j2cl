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
package com.google.j2cl.transpiler.integration.bridgemethodaccidentaloverride;

class Parent {
  public Error foo(Error e) {
    return e;
  }
}

interface SuperInterface<T> {
  T foo(T t);
}

class Child extends Parent implements SuperInterface<Error> {
  // Parent.foo(Error) accidentally overrides SuperInterface.foo(T)
  // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge
  // method delegates to foo__Error() in Parent.
}

/**
 * Test for bridge method with accidental overriding.
 */
public class Main {
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Object callInterfaceFoo(SuperInterface intf, Object t) {
    return intf.foo(t);
  }

  public static void main(String[] args) {
    Main m = new Main();
    Child c = new Child();
    Error e = new Error();
    assert (m.callInterfaceFoo(c, e) == e);
    assert (c.foo(e) == m.callInterfaceFoo(c, e));
    try {
      m.callInterfaceFoo(c, new Object());
      assert false : "ClassCastException should be thrown.";
    } catch (ClassCastException cce) {
      // expected.
    }
  }
}
