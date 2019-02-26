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

import static com.google.j2cl.transpiler.utils.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/**
 * Test for bridge method with accidental overriding.
 */
public class Main {
  public static void main(String[] args) {
    testBridgeForwardsToSpecializedMethod();
    testBridgeSpecializesSuperclassMethod();
  }

  private static void testBridgeForwardsToSpecializedMethod() {
    Child c = new Child();
    Error e = new Error();
    assertTrue((callInterfaceFoo(c, e) == e));
    assertTrue((c.foo(e) == callInterfaceFoo(c, e)));
    assertThrowsClassCastException(() -> callInterfaceFoo(c, new Object()));
  }

  private static class Parent {
    public Error foo(Error e) {
      return e;
    }
  }

  interface SuperInterface<T> {
    T foo(T t);
  }

  private static class Child extends Parent implements SuperInterface<Error> {
    // Parent.foo(Error) accidentally overrides SuperInterface.foo(T)
    // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge
    // method delegates to foo__Error() in Parent.
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object callInterfaceFoo(SuperInterface intf, Object t) {
    return intf.foo(t);
  }

  private static void testBridgeSpecializesSuperclassMethod() {
    SupplierStringImpl sImpl = new SupplierStringImpl("Hello");
    SupplierString s = sImpl;
    assertTrue(s.get().equals("Hello"));

    // Assing to raw type to subvert the value.
    AbstractSupplier as = sImpl;
    as.t = new Object();
    assertThrowsClassCastException(() -> s.f(null));
    // TODO(b/119956463): Uncomment when the bug is fixed.
    // assertThrowsClassCastException(() -> s.get());
  }

  private abstract static class AbstractSupplier<T> {
    T t;

    AbstractSupplier(T t) {
      this.t = t;
    }

    public T get() {
      return t;
    }

    public T f(T t) {
      return this.t;
    }
  }

  interface SupplierString {
    String get();

    String f(String s);
  }

  private static class SupplierStringImpl extends AbstractSupplier<String>
      implements SupplierString {
    // T AbstractSupplier.get() accidentally overrides String SupplierString.get(). Hence there
    // should be a bridge method String SupplierStringImpl.get() that has a cast check on the
    // return.
    SupplierStringImpl(String s) {
      super(s);
    }
  }
}
