package com.google.j2cl.transpiler.readable.genericmethod;

import javaemul.internal.annotations.UncheckedCast;

public class GenericMethod<T> {
  public <T, S> void foo(T f, S s) {} // two type parameters, no bounds.

  public void fun(Object o) {}

  public <T extends Exception> void fun(T t) {} // type parameter with bounds.

  public <T extends Error> void fun(T t) { // type parameter with different bounds.
    new GenericMethod<T>() { // inherit method T
      public void fun2(T t) {} // inherit method T

      public <T> void fun2(T t) {} // redefine T
    };

    class LocalClass<T> extends GenericMethod<T> {
      public void fun2(T t) {}

      public <T extends Number> void fun2(T t) {}
    }
    new LocalClass<T>();
  }

  public <T> GenericMethod<T> bar() {
    return null;
  } // return parameterized type.

  public <T> T[] fun(T[] array) { // generic array type
    return array;
  }

  public <T> T checked() {
    return null;
  }

  @UncheckedCast
  public <T> T unchecked() {
    return null;
  }

  public void test() {
    GenericMethod<Number> g = new GenericMethod<>();
    g.foo(g, g); // call generic method without diamond.
    g.<Error, Exception>foo(new Error(), new Exception()); // call generic method with diamond.

    g.fun(new Object());
    g.fun(new Exception());
    g.fun(new Error());
    g.fun(new String[] {"asdf"});

    String s = checked();
    s = unchecked();
  }
}
