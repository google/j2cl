package com.google.j2cl.transpiler.integration.inlineinstancefieldinit;

/**
 * Test instance field initializer and instance field reference in field declaration.
 */
public class Main {
  public int a = 1;
  public int b = a * 2;

  public static void main(String[] args) {
    Main m = new Main();
    assert m.a == 1;
    assert m.b == 2;
  }
}
