package com.google.j2cl.transpiler.integration.nestedgenericclass;

public class Main<T> {
  public <T> void fun(T outer) {
    /**
     * Generic local class declaring a shadow type variable.
     */
    class A<T> {
      public T t;

      public A(T t) {
        this.t = t;
      }
    }

    /**
     * Non-generic local class that infers type variable declared in enclosing method.
     */
    class B {
      public T t;

      public B(T t) {
        this.t = t;
      }
    }

    // new A<> with different type arguments.
    assert new A<Error>(new Error()).t instanceof Error;
    assert new A<Exception>(new Exception()).t instanceof Exception;

    assert (new B(outer).t == outer);
  }

  public static void main(String[] args) {
    Main<String> m = new Main<>();
    // invokes local classes' creation with different types of outer parameters.
    m.fun(new Object());
    m.fun(new Error());
    m.fun(new Exception());
    m.fun(m);
  }
}
