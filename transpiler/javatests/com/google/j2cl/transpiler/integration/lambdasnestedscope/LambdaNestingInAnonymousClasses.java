package com.google.j2cl.transpiler.integration.lambdasnestedscope;

/**
 * Test lambdas nested in anonymous classes.
 */
public class LambdaNestingInAnonymousClasses {
  public void test() {
    int x = 42;

    // test lambda nested in anonymous class.
    int result =
        new MyInterface() {

          @Override
          public int fun(int a) {
            MyInterface intf = i -> i + x;
            return intf.fun(a) + 100;
          }
        }.fun(100);
    assert result == 242;

    // test lambda nested in multiple anonymous class.
    int[] y = new int[] {42};
    result =
        new MyInterface() {

          @Override
          public int fun(int a1) {
            return new MyInterface() {

              @Override
              public int fun(int a2) {
                return new MyInterface() {

                  @Override
                  public int fun(int a3) {
                    MyInterface intf = i -> y[0] = y[0] + i + a1 + a2 + a3;
                    return intf.fun(1000);
                  }
                }.fun(2000);
              }
            }.fun(3000);
          }
        }.fun(4000);
    assert result == 10042;
    assert y[0] == 10042;
  }
}
