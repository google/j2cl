package com.google.j2cl.transpiler.readable.simplelambda;

interface MyInterface {
  int foo(int i);
}

public class SimpleLambda {
  public int field = 100;

  public void funOuter() {}

  public int test(MyInterface intf, int n) {
    return this.field + intf.foo(n);
  }

  public void testLambdaNoCapture() {
    int result = test(i -> i + 1, 10);
    result =
        test(
            i -> {
              return i + 2;
            },
            10);
  }

  public void testLambdaCaptureField() {
    int result = test(i -> field + i + 1, 10);
  }

  public void testLambdaCaptureLocal() {
    int x = 1;
    int result = test(i -> x + i + 1, 10);
  }

  public void testLambdaCaptureFieldAndLocal() {
    int x = 1;
    int result =
        test(
            i -> {
              int y = 1;
              return x + y + this.field + i + 1;
            },
            10);
  }

  public void testLambdaCallOuterFunction() {
    int result =
        test(
            i -> {
              funOuter();
              this.funOuter();
              SimpleLambda.this.funOuter();
              return i + 2;
            },
            10);
  }

  public static void testLambdaInStaticContext() {
    MyInterface f = (i) -> i;
  }
}
