package com.google.j2cl.transpiler.integration.genericmethod;

public class Main {
  public Object foo(Object o) {
    return o;
  }

  public <T extends Error> T foo(T t) {
    return t;
  }

  public <T extends Exception> T foo(T t) {
    return t;
  }

  public <T> T[] foo(T[] array) {
    return array;
  }

  public static void main(String[] args) {
    Main m = new Main();
    assert m.foo(new Object()) instanceof Object;
    assert m.foo(new Error()) instanceof Error;
    assert m.foo(new Exception()) instanceof Exception;
    assert m.foo(new String[] {"asdf"}) instanceof String[];
  }
}
