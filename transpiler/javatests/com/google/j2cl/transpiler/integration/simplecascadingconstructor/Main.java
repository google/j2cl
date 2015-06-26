package com.google.j2cl.transpiler.integration.simplecascadingconstructor;

/**
 * Test for simple cascading constructors.
 */
public class Main {
  public int a;
  public int b;

  private Main(int a, int b) {
    this.a = a;
    this.b = b;
  }

  public Main(int a) {
    this(a, a * 2);
  }

  public static void main(String[] args) {
    Main m = new Main(10);
    assert m.a == 10;
    assert m.b == 20;
  }
}
