package com.google.j2cl.transpiler.integration.lambdaserializable;

import java.io.Serializable;

interface MyInterface<T> extends Serializable {
  T foo(T i);
}

public class Main {

  public int test(MyInterface<Integer> intf) {
    return intf.foo(3);
  }

  public void testLambda() {
    int result = test(i -> i * 3);
    assert (result == 9);
  }

  public static void main(String[] args) {
    Main m = new Main();
    m.testLambda();
  }
}
