package com.google.j2cl.transpiler.readable.staticinitializers;

public class StaticInitializers {
  public static int a = 5;

  public static int b = a * 2;

  static {
    a = 10;
  }
}
