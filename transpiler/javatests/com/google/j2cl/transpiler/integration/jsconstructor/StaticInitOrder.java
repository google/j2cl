package com.google.j2cl.transpiler.integration.jsconstructor;

import jsinterop.annotations.JsType;

@JsType
public class StaticInitOrder {
  public static int counter = 1;

  public static void test() {
    assert StaticInitOrder.counter == 5;
    assert StaticInitOrder.field1 == 2;
    assert StaticInitOrder.field2 == 3;
  }

  public static int field1 = initializeField1();
  public static int field2 = initializeField2();

  static {
    assert counter++ == 3; // #3
  }

  static {
    assert counter++ == 4; // #4
  }

  public static int initializeField1() {
    assert counter++ == 1; // #1
    return counter;
  }

  public static int initializeField2() {
    assert counter++ == 2; // #2
    return counter;
  }
}
