package com.google.j2cl.transpiler.integration.bridgemethods;

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

    assert (m.callByInterface(c, new AssertionError()) == 1);
    try {
      // javac throws a ClassCastException, but eclipse does not.
      assert (m.callByInterface(c, new Error()) == 1);
      assert false : "ClassCastException should be thrown.";
    } catch (ClassCastException e) {
      // expected.
    }
    assert (m.callByParent(c, new AssertionError()) == 1);
    assert (c.foo(new AssertionError()) == 1);

    assert (m.callByParent(ac, new Error()) == 1);
    assert (m.callByParent(ac, new AssertionError()) == 1);
    assert (m.callByInterface(ac, new AssertionError()) == 2);
    assert (ac.foo(new AssertionError()) == 2);
    assert (ac.foo(new Error()) == 1);
  }
}
