package com.google.j2cl.transpiler.integration.complexcascadingconstructor;

/**
 * Test for cascading constructors with side effect in field initialization.
 */
public class Main {
  public static int counter = 1;
  public int a = incCounter();
  public int b = incCounter();

  private Main(int a, int b) {
    this.a += a;
    this.b += b;
  }

  public Main(int a) {
    this(a, a * 2);
  }

  public static void main(String[] args) {
    Main m = new Main(10);
    assert m.a == 12;
    assert m.b == 23;
    assert counter == 3;
  }

  private int incCounter() {
    counter += 1;
    return counter;
  }
}
