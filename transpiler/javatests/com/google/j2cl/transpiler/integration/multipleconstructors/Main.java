package com.google.j2cl.transpiler.integration.multipleconstructors;

/**
 * Test multiple constructors.
 */
public class Main {
  private int id;
  private boolean flag;

  public Main(int id) {
    this.id = id;
    this.flag = (id == 0);
  }

  public Main(boolean flag) {
    this.id = -1;
    this.flag = flag;
  }

  public Main(int id, boolean flag) {
    this.id = id;
    this.flag = flag;
  }

  public static void main(String[] args) {
    Main m1 = new Main(1);
    assert m1.id == 1;
    assert !m1.flag;

    Main m2 = new Main(true);
    assert m2.id == -1;
    assert m2.flag;

    Main m3 = new Main(10, false);
    assert m3.id == 10;
    assert !m3.flag;
  }
}
