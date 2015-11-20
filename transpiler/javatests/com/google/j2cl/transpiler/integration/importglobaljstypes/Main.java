package com.google.j2cl.transpiler.integration.importglobaljstypes;

public class Main {
  public static void main(String... args) {
    assert Math.fun(-1) == 1;
    assert Number.test(1.0);
    assert !Number.test(1.1);
    assert Number.fromCharCode(new int[] {65, 66}).equals("AB");
  }
}
