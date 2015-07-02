package com.google.j2cl.transpiler.readable.ifstatement;

public class IfStatement {
  public void test() {
    boolean a = true;
    boolean b = true;
    int number = 1;
    if (a) {
      number = 2;
    } else if (b) {
      number = 3;
    } else {
      number = 4;
    }
  }
}
