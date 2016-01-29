package com.google.j2cl.transpiler.readable.wideningandboxing;

public class Main {
  public static void fun(Double... elements) {}

  public static void bar(Float... elements) {}

  public static void foo() {
    int[] numbers = new int[] {1, 2};
    fun((double) 'a', (double) 4, (double) numbers[0]);
    bar((float) 'a', (float) 4, (float) numbers[0]);
  }
}
