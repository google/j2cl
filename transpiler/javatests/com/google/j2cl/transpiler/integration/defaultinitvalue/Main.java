package com.google.j2cl.transpiler.integration.defaultinitvalue;

/**
 * Test default initial value.
 */
public class Main {
  public int instanceInt;
  public boolean instanceBoolean;
  public Object instanceObject;
  public static int staticInt;
  public static boolean staticBoolean;
  public static Object staticObject;

  public static void main(String[] args) {
    Main m = new Main();
    assert m.instanceInt == 0;
    assert !m.instanceBoolean;
    assert Main.staticInt == 0;
    assert !Main.staticBoolean;

    // Avoiding a "condition always evaluates to true" error in JSComp type checking.
    Object maybeNull = Main.staticInt == 0 ? null : new Object();
    assert Main.staticObject == maybeNull;
    assert m.instanceObject == maybeNull;
  }
}
