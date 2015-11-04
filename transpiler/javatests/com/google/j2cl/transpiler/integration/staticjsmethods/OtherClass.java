package com.google.j2cl.transpiler.integration.staticjsmethods;

public class OtherClass {
  public static int callF1(int a) {
    return Main.f1(a);
  }

  public static int callF2(int a) {
    return Main.f2(a);
  }
}
