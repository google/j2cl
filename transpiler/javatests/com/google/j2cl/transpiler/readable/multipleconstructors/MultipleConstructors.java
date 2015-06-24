package com.google.j2cl.transpiler.readable.multipleconstructors;

public class MultipleConstructors {
  private int id;
  private boolean flag;

  public MultipleConstructors(int id) {
    this.id = id;
    this.flag = (id == 0);
  }

  public MultipleConstructors(boolean flag) {
    this.id = -1;
    this.flag = flag;
  }

  public MultipleConstructors(int id, boolean flag) {
    this.id = id;
    this.flag = flag;
  }
}
