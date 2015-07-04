package com.google.j2cl.transpiler.readable.ternaryexpression;

public class TernaryExpression {
  public void test() {
    boolean a = true;
    int number = a ? 1 : 2;

    boolean b = number == 1 ? number == 2 : number == 1;
  }
}
