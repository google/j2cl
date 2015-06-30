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
    assert m.instanceObject == null;
    assert Main.staticInt == 0;
    assert !Main.staticBoolean;
    assert Main.staticObject == null;
  }
}
