package com.google.j2cl.transpiler.readable.boundedtypesupercall;

public class Main {

  public abstract static class Foo<T> {
    public Foo(T foo) {}
    public abstract void foo();
  }

  public static class Bar<T extends Comparable> {

    public T getSomething() {
      return null;
    }

    public Foo<T> doSomething() {
      return new Foo<T>(getSomething()) {
        @Override
        public void foo() {}
      };
    }
  }
}
