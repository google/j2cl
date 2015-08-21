package com.google.j2cl.transpiler.readable.lambdawithgenerics;

interface MyInterface<T> {
  T foo(T i);
}

public class LambdaWithGenerics {
  public Error test(MyInterface<Error> intf, Error e) {
    return intf.foo(e);
  }

  public void testLambdaNoCapture() {
    test(i -> i, new Error());
  }
}
