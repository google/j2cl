package com.google.j2cl.transpiler.readable.staticfieldaccesslevels;

public class StaticFieldAccessLevels {
  public static int a;
  private static boolean b;
  protected static Object c;
  static int d;

  public int test() {
    return b ? a : StaticFieldAccessLevels.d;
  }
}
