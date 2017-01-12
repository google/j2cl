package com.google.j2cl.transpiler.integration.morebridgemethods;

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

public class BridgeMethodsMain {
  public int callByInterface(SomeInterface<AssertionError> intf, AssertionError ae) {
    return intf.foo(ae);
  }

  public static void test() {
    BridgeMethodsMain m = new BridgeMethodsMain();
    Child c = new Child();

    assert (m.callByInterface(c, new AssertionError()) == 1);
  }
}
