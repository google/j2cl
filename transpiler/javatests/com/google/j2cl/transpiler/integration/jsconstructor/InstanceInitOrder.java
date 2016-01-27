package com.google.j2cl.transpiler.integration.jsconstructor;

import jsinterop.annotations.JsConstructor;

public class InstanceInitOrder {
  public static int initStep = 1;

  public static void test() {
    InstanceInitOrder m = new InstanceInitOrder();
    assert InstanceInitOrder.initStep == 6;
  }

  public int field1 = this.initializeField1();
  public int field2 = initializeField2();

  {
    assert initStep++ == 3; // #3
  }

  {
    assert initStep++ == 4; // #4
  }

  @JsConstructor
  public InstanceInitOrder() {
    assert initStep++ == 5; // #5
  }

  public int initializeField1() {
    assert initStep++ == 1; // #1
    return 0;
  }

  public int initializeField2() {
    assert initStep++ == 2; // #2
    return 0;
  }
}
