package com.google.j2cl.transpiler.integration.subnativejstype;

public class Main {
  public static void main(String... args) {
    SubMyNativeType sub = new SubMyNativeType(10, 20);
    assert sub.executed;
    assert sub.x == 30;
    assert sub.y == 200;
    assert sub.f == 10;
    assert sub.foo(100) == 330;
  }
}
