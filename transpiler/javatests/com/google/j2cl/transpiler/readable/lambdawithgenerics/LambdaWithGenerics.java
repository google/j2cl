package com.google.j2cl.transpiler.readable.lambdawithgenerics;

interface MyInterface<T> {
  T foo(T i);
}

public class LambdaWithGenerics {
  public <T> T test1(MyInterface<T> intf, T e) {
    return intf.foo(e);
  }

  public Error test2(MyInterface<Error> intf, Error e) {
    return intf.foo(e);
  }

  public void testLambdaNoCapture() {
    test1(i -> i, new Error());
    test2(i -> i, new Error());
  }
}
