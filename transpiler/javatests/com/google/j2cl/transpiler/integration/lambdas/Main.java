package com.google.j2cl.transpiler.integration.lambdas;

interface MyInterface {
  int foo(int i);
}

public class Main {
  public int field = 100;

  public int test(MyInterface intf, int n) {
    return this.field + intf.foo(n);
  }

  public int test(MyInterface intf) {
    this.field = 200;
    return this.field + intf.foo(300);
  }

  public void testLambdaNoCapture() {
    int result = test(i -> i + 1, 10);
    assert result == 111;
    result =
        test(
            i -> {
              return i + 2;
            },
            10);
    assert result == 112;
  }

  public void testInstanceofLambda() {
    MyInterface intf = (i -> i + 1);
    assert (intf instanceof MyInterface);
  }

  public void testLambdaCaptureField() {
    int result = test(i -> field + i + 1, 10);
    assert result == 211;
  }

  public void testLambdaCaptureLocal() {
    int x = 1;
    int result = test(i -> x + i + 1, 10);
    assert result == 112;
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
    assert result == 213;
  }

  public void testLambdaCaptureField2() {
    int result = test(i -> field + i + 1);
    assert (result == 701);
    assert (this.field == 200);
  }

  public static void main(String[] args) {
    Main m = new Main();
    m.testLambdaNoCapture();
    m.testInstanceofLambda();
    m.testLambdaCaptureField();
    m.testLambdaCaptureLocal();
    m.testLambdaCaptureFieldAndLocal();
    m.testLambdaCaptureField2();
  }
}
