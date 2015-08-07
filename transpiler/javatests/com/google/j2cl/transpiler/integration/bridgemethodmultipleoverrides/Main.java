package com.google.j2cl.transpiler.integration.bridgemethodmultipleoverrides;

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
    assert m.callByInterface(c, err, exc) == 2;
    assert c.t == err;
    assert c.s == exc;

    c = new Child();
    assert m.callBySuperParent(c, err, exc) == 2;
    assert c.t == err;
    assert c.s == exc;

    c = new Child();
    assert m.callByParent(c, err, exc) == 2;
    assert c.t == err;
    assert c.s == exc;

    try {
      m.callByInterface(c, new Object(), new Object());
      assert false : "ClassCastException should be thrown.";
    } catch (ClassCastException e) {
      // expected;
    }
    try {
      m.callBySuperParent(c, new Object(), new Object());
      assert false : "ClassCastException should be thrown.";
    } catch (ClassCastException e) {
      // expected;
    }
    try {
      m.callByParent(c, new Object(), new Exception());
      assert false : "ClassCastException should be thrown.";
    } catch (ClassCastException e) {
      // expected;
    }
  }
}
