package com.google.j2cl.transpiler.integration.staticfields;

public class Main {
  public static int a = 2;
  protected static int b = a * 3;

  public static void main(String[] args) {
    assert Main.a == 2;
    assert Main.b == 6;
  }
}
